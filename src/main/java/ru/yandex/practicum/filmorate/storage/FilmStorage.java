package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;

import java.util.List;

public interface FilmStorage {
    void clear();

    Collection<Film> findAll();

    Film add(Film film);

    Film update(Film film);

    Film getById(Long id);

    void addLike(Long filmId, Long userId);

    void removeLike(Long filmId, Long userId);

    List<Film> getPopular(int count);
}