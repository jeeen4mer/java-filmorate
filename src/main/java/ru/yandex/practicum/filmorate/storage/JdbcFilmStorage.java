package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Component
@Primary

public class JdbcFilmStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    private void validateFilmExists(Long id) {
        if (!containsFilm(id)) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }
    }

    private Set<Genre> loadGenresForFilm(Long filmId) {
        String sql = """
        SELECT g.genre_id, g.name
        FROM genres g
        INNER JOIN film_genres fg ON g.genre_id = fg.genre_id
        WHERE fg.film_id = ?
        ORDER BY g.genre_id
        """;

        List<Genre> genres = jdbcTemplate.query(sql, (rs, rowNum) ->
                        Genre.builder()
                                .id(rs.getInt("genre_id"))
                                .name(rs.getString("name"))
                                .build(),
                filmId);

        return new LinkedHashSet<>(genres); // Сохраняем порядок
    }

    @Autowired
    public JdbcFilmStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Film> filmRowMapper = (rs, rowNum) -> {
        Film film = Film.builder()
                .id(rs.getLong("film_id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .releaseDate(rs.getDate("release_date").toLocalDate())
                .duration(rs.getInt("duration"))
                .mpa(getMpaById(rs.getInt("rating_id")))
                .genres(new HashSet<>())
                .build();

        Set<Genre> genres = getGenresByFilmId(film.getId());
        film.getGenres().addAll(genres);

        Set<Long> likes = getLikesByFilmId(film.getId());
        film.getLikes().addAll(likes);

        return film;
    };

    @Override
    public Collection<Film> findAll() {
        String sql = "SELECT * FROM films ORDER BY film_id";
        return jdbcTemplate.query(sql, filmRowMapper);
    }

    @Override
    public Film add(Film film) {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("films")
                .usingGeneratedKeyColumns("film_id");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", film.getName());
        parameters.put("description", film.getDescription());
        parameters.put("release_date", java.sql.Date.valueOf(film.getReleaseDate()));
        parameters.put("duration", film.getDuration());
        parameters.put("rating_id", film.getMpa().getId());

        Number generatedId = simpleJdbcInsert.executeAndReturnKey(parameters);

        film.setId(generatedId.longValue());

        return film;
    }

    @Override
    public Film update(Film film) {
        if (!containsFilm(film.getId())) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }

        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, rating_id = ? WHERE film_id = ?";
        int rows = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId());

        if (rows == 0) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }

        deleteGenresForFilm(film.getId());
        saveGenresForFilm(film.getId(), film.getGenres());

        return film;
    }

    @Override
    public Film getById(Long id) {
        validateFilmExists(id);

        String sql = """
        SELECT f.film_id, f.name, f.description, f.release_date, f.duration, 
               m.rating_id AS mpa_id, m.name AS mpa_name
        FROM films f
        LEFT JOIN mpa_ratings m ON f.rating_id = m.rating_id
        WHERE f.film_id = ?
        """;

        try {
            Film film = jdbcTemplate.queryForObject(sql, new RowMapper<Film>() {
                @Override
                public Film mapRow(ResultSet rs, int rowNum) throws SQLException {
                    MpaRating mpa = MpaRating.builder()
                            .id(rs.getInt("mpa_id"))
                            .name(rs.getString("mpa_name"))
                            .build();

                    return Film.builder()
                            .id(rs.getLong("film_id"))
                            .name(rs.getString("name"))
                            .description(rs.getString("description"))
                            .releaseDate(rs.getDate("release_date").toLocalDate())
                            .duration(rs.getInt("duration"))
                            .mpa(mpa)
                            .build();
                }
            }, id);

            if (film == null) {
                throw new NotFoundException("Фильм с id=" + id + " не найден");
            }

            film.setGenres(loadGenresForFilm(film.getId()));

            return film;
        } catch (Exception e) {
            log.error("Ошибка при получении фильма с id={}", id, e);
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }
    }

    @Override
    public void clear() {
        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM film_genres");
        jdbcTemplate.update("DELETE FROM films");
    }

    private boolean containsFilm(Long id) {
        String sql = "SELECT COUNT(*) FROM films WHERE film_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    private MpaRating getMpaById(int ratingId) {
        String sql = "SELECT rating_id, name FROM mpa_ratings WHERE rating_id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> MpaRating.builder()
                .id(rs.getInt("rating_id"))
                .name(rs.getString("name"))
                .build(), ratingId);
    }

    private Set<Genre> getGenresByFilmId(Long filmId) {
        String sql = """
            SELECT g.genre_id, g.name
            FROM genres g
            JOIN film_genres fg ON g.genre_id = fg.genre_id
            WHERE fg.film_id = ?
            """;
        return new HashSet<>(jdbcTemplate.query(sql, (rs, rowNum) -> Genre.builder()
                .id(rs.getInt("genre_id"))
                .name(rs.getString("name"))
                .build(), filmId));
    }

    private Set<Long> getLikesByFilmId(Long filmId) {
        String sql = "SELECT user_id FROM film_likes WHERE film_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Long.class, filmId));
    }

    private void saveGenresForFilm(Long filmId, Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) return;

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        for (Genre genre : genres) {
            jdbcTemplate.update(sql, filmId, genre.getId());
        }
    }

    private void deleteGenresForFilm(Long filmId) {
        String sql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(sql, filmId);
    }
}