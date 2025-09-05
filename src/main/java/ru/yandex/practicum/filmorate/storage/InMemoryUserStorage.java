package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private final Set<String> emails = new HashSet<>();
    private Long nextId = 1L;

    @Override
    public void clear() {
        users.clear();
        emails.clear();
        nextId = 1L;
    }

    @Override
    public Collection<User> findAll() {
        return users.values();
    }

    @Override
    public User add(User user) {
        if (emails.contains(user.getEmail())) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }
        user.setId(nextId++);
        users.put(user.getId(), user);
        emails.add(user.getEmail());
        return user;
    }

    @Override
    public User update(User newUser) {
        if (!users.containsKey(newUser.getId())) {
            throw new NotFoundException("Пользователь не найден");
        }
        User oldUser = users.get(newUser.getId());
        if (!oldUser.getEmail().equals(newUser.getEmail())) {
            if (emails.contains(newUser.getEmail())) {
                throw new IllegalArgumentException("Пользователь с таким email уже существует");
            }
            emails.remove(oldUser.getEmail());
            emails.add(newUser.getEmail());
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