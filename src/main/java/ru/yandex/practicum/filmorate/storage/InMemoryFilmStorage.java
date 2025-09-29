package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

import java.util.*;

public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();
    private Long nextId = 1L;

    @Override
    public void clear() {
        films.clear();
        nextId = 1L;
    }

    @Override
    public Collection<Film> findAll() {
        return new ArrayList<>(films.values());
    }

    @Override
    public Film add(Film film) {
        film.setId(nextId++);
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film update(Film newFilm) {
        if (!films.containsKey(newFilm.getId())) {
            throw new NotFoundException("Фильм не найден");
        }
        films.put(newFilm.getId(), newFilm);
        return newFilm;
    }

    @Override
    public Film getById(Long id) {
        Film film = films.get(id);
        if (film == null) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }
        return film;
    }
}