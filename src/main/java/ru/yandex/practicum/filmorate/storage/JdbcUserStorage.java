package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Component
@Primary
@Slf4j

public class JdbcUserStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JdbcUserStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        return User.builder()
                .id(rs.getLong("user_id"))
                .email(rs.getString("email"))
                .login(rs.getString("login"))
                .name(rs.getString("name"))
                .birthday(rs.getDate("birthday").toLocalDate())
                .build();
    }

    @Override
    public void clear() {
        jdbcTemplate.update("DELETE FROM friendships");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Override
    public Collection<User> findAll() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, this::mapRowToUser);
    }

    @Override
    public User add(User user) {
        SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("users")
                .usingGeneratedKeyColumns("user_id");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("email", user.getEmail());
        parameters.put("login", user.getLogin());
        parameters.put("name", user.getName() != null && !user.getName().isBlank() ? user.getName() : user.getLogin());
        parameters.put("birthday", java.sql.Date.valueOf(user.getBirthday()));

        try {
            Number key = insert.executeAndReturnKey(parameters);
            user.setId(key.longValue());
            log.info("Добавлен пользователь: {}", user.getLogin());
            return user;
        } catch (DataAccessException e) {
            log.error("Ошибка при добавлении пользователя: {}", e.getMessage());
            throw new RuntimeException("Не удалось добавить пользователя", e);
        }
    }

    @Override
    public User update(User user) {
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?";
        int rows = jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                java.sql.Date.valueOf(user.getBirthday()),
                user.getId());

        if (rows == 0) {
            throw new NotFoundException("Пользователь с id=" + user.getId() + " не найден");
        }
        return user;
    }

    @Override
    public User getById(Long id) {
        validateUserExists(id);
        String sql = "SELECT * FROM users WHERE user_id = ?";
        return jdbcTemplate.queryForObject(sql, this::mapRowToUser, id);
    }

    @Override
    public void addFriendRequest(Long userId, Long friendId) {
        validateUserExists(userId);
        validateUserExists(friendId);

        String checkSql = "SELECT COUNT(*) FROM friendships WHERE user_id = ? AND friend_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, friendId);
        if (count != null && count > 0) {
            return;
        }

        String insertSql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, 'UNCONFIRMED')";
        jdbcTemplate.update(insertSql, userId, friendId);
        jdbcTemplate.update(insertSql, friendId, userId);
    }

    @Override
    public void confirmFriendRequest(Long userId, Long friendId) {
        validateUserExists(userId);
        validateUserExists(friendId);

        String checkSql = "SELECT COUNT(*) FROM friendships WHERE user_id = ? AND friend_id = ? AND status = 'UNCONFIRMED'";
        if (jdbcTemplate.queryForObject(checkSql, Integer.class, userId, friendId) == 0) {
            throw new NotFoundException("Запрос в друзья от пользователя с id=" + friendId + " к пользователю с id=" + userId + " не найден");
        }

        String updateSql = "UPDATE friendships SET status = 'CONFIRMED' WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(updateSql, userId, friendId);
        jdbcTemplate.update(updateSql, friendId, userId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        validateUserExists(userId);
        validateUserExists(friendId);

        String sql = "DELETE FROM friendships WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";
        int rows = jdbcTemplate.update(sql, userId, friendId, friendId, userId);
    }

    @Override
    public Collection<User> getConfirmedFriends(Long userId) {
        String sql = """
            SELECT u.* FROM users u
            JOIN friendships f ON u.user_id = f.friend_id
            WHERE f.user_id = ? AND f.status = 'CONFIRMED'
            """;
        return jdbcTemplate.query(sql, this::mapRowToUser, userId);
    }

    @Override
    public boolean containsUser(Long userId) {
        String sql = "SELECT COUNT(*) FROM users WHERE user_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, userId) > 0;
    }

    private void validateUserExists(Long userId) {
        String sql = "SELECT COUNT(*) FROM users WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        if (count == null || count == 0) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }
}