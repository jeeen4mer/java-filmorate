package ru.yandex.practicum.filmorate.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.storage.UserStorage;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserStorage userStorage;

    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public void addFriend(Long userId, Long friendId) {
        User user = userStorage.getById(userId);
        User friend = userStorage.getById(friendId);
        user.getFriends().add(friendId);
        friend.getFriends().add(userId);
        userStorage.update(user);
        userStorage.update(friend);
    }

    public void removeFriend(Long userId, Long friendId) {
        User user = userStorage.getById(userId);
        User friend = userStorage.getById(friendId);
        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);
        userStorage.update(user);
        userStorage.update(friend);
    }

    public Collection<User> getFriends(Long userId) {
        User user = userStorage.getById(userId);
        return user.getFriends()
                .stream()
                .map(userStorage::getById)
                .collect(Collectors.toList());
    }

    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        Set<Long> friends1 = userStorage.getById(userId).getFriends();
        Set<Long> friends2 = userStorage.getById(otherId).getFriends();
        return friends1.stream()
                .filter(friends2::contains)
                .map(userStorage::getById)
                .collect(Collectors.toList());
    }
}