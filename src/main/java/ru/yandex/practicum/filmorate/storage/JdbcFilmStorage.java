package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Primary
@Slf4j
public class JdbcFilmStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JdbcFilmStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @Override
    public Collection<Film> findAll() {
        String sql = """
            SELECT f.film_id, f.name, f.description, f.release_date, f.duration,
                   m.rating_id AS mpa_id, m.name AS mpa_name
            FROM films f
            LEFT JOIN mpa_ratings m ON f.rating_id = m.rating_id
            ORDER BY f.film_id
            """;
        List<Film> films = jdbcTemplate.query(sql, (rs, rowNum) -> {
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
        });

        List<Long> filmIds = films.stream().map(Film::getId).collect(Collectors.toList());

        Map<Long, Set<Genre>> genresByFilm = loadGenresForFilms(filmIds);

        for (Film film : films) {
            Set<Genre> genres = genresByFilm.getOrDefault(film.getId(), Collections.emptySet());
            film.setGenres(genres);
        }

        return films;
    }

    @Override
    public Film add(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new ValidationException("Поле mpa.id обязательно для заполнения");
        }
        validateMpaExists(film.getMpa().getId());
        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                if (genre.getId() == null) {
                    throw new ValidationException("ID жанра не может быть null");
                }
                validateGenreExists(genre.getId());
            }
        }
        SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("films")
                .usingGeneratedKeyColumns("film_id");
        Map<String, Object> params = new HashMap<>();
        params.put("name", film.getName());
        params.put("description", film.getDescription());
        params.put("release_date", java.sql.Date.valueOf(film.getReleaseDate()));
        params.put("duration", film.getDuration());
        params.put("rating_id", film.getMpa().getId());
        try {
            Number key = insert.executeAndReturnKey(params);
            film.setId(key.longValue());
            if (film.getGenres() != null && !film.getGenres().isEmpty()) {
                saveGenresForFilm(film.getId(), film.getGenres());
            }
            log.info("Добавлен фильм: {}", film.getName());
            return film;
        } catch (Exception e) {
            log.error("Ошибка при добавлении фильма: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось добавить фильм", e);
        }
    }

    @Override
    public Film update(Film film) {
        validateFilmExists(film.getId());
        validateMpaExists(film.getMpa().getId());
        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                validateGenreExists(genre.getId());
            }
        }
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, rating_id = ? WHERE film_id = ?";
        int rows = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                java.sql.Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId());
        if (rows == 0) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }
        deleteGenresForFilm(film.getId());
        saveGenresForFilm(film.getId(), film.getGenres());
        log.info("Обновлён фильм: {}", film.getName());
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
            Film film = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
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
            }, id);

            film.setGenres(loadGenresForFilm(film.getId()));

            return film;
        } catch (Exception e) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }
    }

    public void addLike(Long filmId, Long userId) {
        String checkFilmSql = "SELECT COUNT(*) FROM films WHERE film_id = ?";
        String checkUserSql = "SELECT COUNT(*) FROM users WHERE user_id = ?";
        String insertSql = "INSERT INTO film_likes (film_id, user_id) VALUES (?, ?)";
        if (jdbcTemplate.queryForObject(checkFilmSql, Integer.class, filmId) == 0) {
            throw new NotFoundException("Фильм с id=" + filmId + " не найден");
        }
        if (jdbcTemplate.queryForObject(checkUserSql, Integer.class, userId) == 0) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        try {
            jdbcTemplate.update(insertSql, filmId, userId);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.info("Лайк уже существует: film_id={}, user_id={}", filmId, userId);
        }
    }

    public void removeLike(Long filmId, Long userId) {
        String sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
        int rows = jdbcTemplate.update(sql, filmId, userId);
        if (rows == 0) {
            log.warn("Лайк не найден для удаления: film_id={}, user_id={}", filmId, userId);
        }
    }

    @Override
    public List<Film> getPopular(int count) {
        String sql = """
        SELECT f.film_id, f.name, f.description, f.release_date, f.duration,
               m.rating_id AS mpa_id, m.name AS mpa_name,
               COUNT(fl.user_id) AS likes_count
        FROM films f
        LEFT JOIN mpa_ratings m ON f.rating_id = m.rating_id
        LEFT JOIN film_likes fl ON f.film_id = fl.film_id
        GROUP BY f.film_id, m.name
        ORDER BY likes_count DESC, f.film_id
        LIMIT ?
        """;
        List<Film> films = jdbcTemplate.query(sql, (rs, rowNum) -> {
            MpaRating mpa = MpaRating.builder()
                    .id(rs.getInt("mpa_id"))
                    .name(rs.getString("mpa_name"))
                    .build();
            Film film = Film.builder()
                    .id(rs.getLong("film_id"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .releaseDate(rs.getDate("release_date").toLocalDate())
                    .duration(rs.getInt("duration"))
                    .mpa(mpa)
                    .build();
            film.setLikesCount(rs.getInt("likes_count"));
            return film;
        }, count);

        List<Long> filmIds = films.stream().map(Film::getId).collect(Collectors.toList());

        Map<Long, Set<Genre>> genresByFilm = loadGenresForFilms(filmIds);

        for (Film film : films) {
            Set<Genre> genres = genresByFilm.getOrDefault(film.getId(), Collections.emptySet());
            film.setGenres(genres);
        }

        return films;
    }

    @Override
    public void clear() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM film_genres");
        jdbcTemplate.update("DELETE FROM films");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    private void validateFilmExists(Long id) {
        if (!containsFilm(id)) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }
    }

    private void validateMpaExists(Integer ratingId) {
        String sql = "SELECT COUNT(*) FROM mpa_ratings WHERE rating_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, ratingId);
        if (count == null || count == 0) {
            throw new NotFoundException("Рейтинг MPA с id=" + ratingId + " не найден");
        }
    }

    private void validateGenreExists(Integer genreId) {
        String sql = "SELECT COUNT(*) FROM genres WHERE genre_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, genreId);
        if (count == null || count == 0) {
            throw new NotFoundException("Жанр с id=" + genreId + " не найден");
        }
    }

    private Map<Long, Set<Genre>> loadGenresForFilms(Collection<Long> filmIds) {
        if (filmIds == null || filmIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String inClause = filmIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        String sql = """
            SELECT fg.film_id, g.genre_id, g.name
            FROM film_genres fg
            INNER JOIN genres g ON fg.genre_id = g.genre_id
            WHERE fg.film_id IN (%s)
            ORDER BY fg.film_id, g.genre_id
            """.formatted(inClause);

        Map<Long, Set<Genre>> genresByFilm = new HashMap<>();
        jdbcTemplate.query(sql, (rs, rowNum) -> {
            Long filmId = rs.getLong("film_id");
            Genre genre = Genre.builder()
                    .id(rs.getInt("genre_id"))
                    .name(rs.getString("name"))
                    .build();

            genresByFilm.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genre);
            return null;
        });

        return genresByFilm;
    }

    private Set<Genre> loadGenresForFilm(Long filmId) {
        if (filmId == null) return Collections.emptySet();
        String sql = """
            SELECT g.genre_id, g.name
            FROM genres g
            INNER JOIN film_genres fg ON g.genre_id = fg.genre_id
            WHERE fg.film_id = ?
            ORDER BY g.genre_id
            """;
        List<Genre> genres = jdbcTemplate.query(sql, (rs, rowNum) -> Genre.builder()
                .id(rs.getInt("genre_id"))
                .name(rs.getString("name"))
                .build(), filmId);
        return new LinkedHashSet<>(genres);
    }

    private boolean containsFilm(Long id) {
        String sql = "SELECT COUNT(*) FROM films WHERE film_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
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