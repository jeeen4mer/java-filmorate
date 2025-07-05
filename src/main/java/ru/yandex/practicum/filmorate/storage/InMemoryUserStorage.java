package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private static Long nextId = 1L;

    @Override
    public Collection<User> findAll() {
        return users.values();
    }

    @Override
    public User add(User user) {
        user.setId(nextId++);
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User update(User newUser) {
        if (!users.containsKey(newUser.getId())) {
            throw new NotFoundException("Пользователь не найден");
        }
        users.put(newUser.getId(), newUser);
        return newUser;
    }

    @Override
    public User getById(Long id) {
        if (!users.containsKey(id)) {
            throw new NotFoundException("Пользователь не найден");
        }
        return users.get(id);
    }
}