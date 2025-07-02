package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


@RestController
@Validated
@RequestMapping("/films")
@Slf4j
public class FilmController {
    private final Map<Long, Film> films = new HashMap<>();

    @GetMapping
    public Collection<Film> findAll() {
        return films.values();
    }

    @PostMapping
    public Film add(@Valid @RequestBody final Film film) {
        checkFilm(film);
        film.setId(getNextId());
        films.put(film.getId(), film);
        log.info("Фильм с id = {} успешно добавлен", film.getId());
        return film;
    }

    private long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    @PutMapping
    public Film update(@Valid @RequestBody final Film newFilm) {
        if (newFilm.getId() == null) {
            throw new ConditionsNotMetException("Не указан id фильма");
        }

        checkFilm(newFilm);

        if (films.containsKey(newFilm.getId())) {
            Film oldFilm = films.get(newFilm.getId());
            oldFilm.setName(newFilm.getName());
            oldFilm.setDescription(newFilm.getDescription());
            oldFilm.setReleaseDate(newFilm.getReleaseDate());
            oldFilm.setDuration(newFilm.getDuration());
            log.info("Фильм с id = {} успешно обновлён", newFilm.getId());
            return oldFilm;
        }
        throw new NotFoundException(
                String.format("Фильм с id = %d не найден", newFilm.getId())
        );
    }

    public Film checkFilm(@RequestBody final Film film) {
        LocalDate birthdayFilm = LocalDate.of(1895, 12, 28);
        if (film.getReleaseDate().isBefore(birthdayFilm)) {
            throw new ValidationException("Дата релиза должна быть не раньше 28 декабря 1895 года");
        }
        return film;
    }
}