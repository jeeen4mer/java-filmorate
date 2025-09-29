package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

@Component
@RequiredArgsConstructor

public class JdbcMpaStorage implements MpaStorage {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<MpaRating> mpaRowMapper = new RowMapper<MpaRating>() {
        @Override
        public MpaRating mapRow(ResultSet rs, int rowNum) throws SQLException {
            return MpaRating.builder()
                    .id(rs.getInt("rating_id"))
                    .name(rs.getString("name"))
                    .build();
        }
    };

    @Override
    public Collection<MpaRating> getAll() {
        String sql = "SELECT rating_id, name FROM mpa_ratings ORDER BY rating_id";
        return jdbcTemplate.query(sql, mpaRowMapper);
    }

    @Override
    public MpaRating getById(Integer id) {
        String sql = "SELECT rating_id, name FROM mpa_ratings WHERE rating_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, mpaRowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Рейтинг MPA с id=" + id + " не найден");
        }
    }
}