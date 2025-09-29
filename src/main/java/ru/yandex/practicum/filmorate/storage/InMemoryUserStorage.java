package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users = new HashMap<>();
    private final Set<String> emails = new HashSet<>();
    private final Map<Long, Map<Long, FriendshipStatus>> friendships = new HashMap<>();
    private Long nextId = 1L;

    @Override
    public void clear() {
        users.clear();
        emails.clear();
        friendships.clear();
        nextId = 1L;
    }

    @Override
    public Collection<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public User add(User user) {
        if (emails.contains(user.getEmail())) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }
        user.setId(nextId++);
        users.put(user.getId(), user);
        emails.add(user.getEmail());

        friendships.put(user.getId(), new HashMap<>());

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
        User user = users.get(id);
        if (user == null) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
        return user;
    }

    @Override
    public void addFriendRequest(Long userId, Long friendId) {
        if (!containsUser(userId) || !containsUser(friendId)) {
            throw new NotFoundException("Пользователь не найден");
        }
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("Нельзя добавить самого себя в друзья");
        }

        friendships.get(userId).put(friendId, FriendshipStatus.UNCONFIRMED);
    }

    @Override
    public void confirmFriendRequest(Long userId, Long friendId) {
        if (!users.containsKey(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        Map<Long, FriendshipStatus> incomingRequests = friendships.get(userId);
        FriendshipStatus status = incomingRequests != null ? incomingRequests.get(friendId) : null;

        if (status != FriendshipStatus.UNCONFIRMED) {
            throw new NotFoundException("Запрос в друзья от пользователя " + friendId + " не найден или уже подтверждён");
        }

        incomingRequests.put(friendId, FriendshipStatus.CONFIRMED);

        friendships.get(friendId).put(userId, FriendshipStatus.CONFIRMED);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        if (!containsUser(userId) || !containsUser(friendId)) {
            throw new NotFoundException("Пользователь не найден");
        }

        if (friendships.containsKey(userId)) {
            friendships.get(userId).remove(friendId);
        }
        if (friendships.containsKey(friendId)) {
            friendships.get(friendId).remove(userId);
        }
    }

    @Override
    public Collection<User> getConfirmedFriends(Long userId) {
        if (!users.containsKey(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        return friendships.getOrDefault(userId, Collections.emptyMap()).entrySet().stream()
                .filter(entry -> entry.getValue() == FriendshipStatus.CONFIRMED)
                .map(entry -> users.get(entry.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public boolean containsUser(Long userId) {
        return users.containsKey(userId);
    }

    private enum FriendshipStatus {
        UNCONFIRMED, CONFIRMED
    }
}