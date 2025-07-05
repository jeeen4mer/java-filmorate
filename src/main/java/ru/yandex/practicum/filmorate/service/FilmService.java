package ru.yandex.practicum.filmorate.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public void likeFilm(Long filmId, Long userId) {
        filmStorage.getById(filmId);
        userStorage.getById(userId);
        Film film = filmStorage.getById(filmId);
        film.addLike(userId);
        filmStorage.update(film);
    }

    public void unlikeFilm(Long filmId, Long userId) {
        filmStorage.getById(filmId);
        userStorage.getById(userId);
        Film film = filmStorage.getById(filmId);
        film.removeLike(userId);
        filmStorage.update(film);
    }

    public List<Film> getPopularFilms(int count) {
        return filmStorage.findAll().stream()
                .sorted((f1, f2) -> Integer.compare(f2.getLikesCount(), f1.getLikesCount()))
                .limit(count)
                .collect(Collectors.toList());
    }
}