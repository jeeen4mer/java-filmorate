package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;

public interface UserStorage {
    void clear();

    Collection<User> findAll();

    User add(User user);

    User update(User user);

    User getById(Long id);

}