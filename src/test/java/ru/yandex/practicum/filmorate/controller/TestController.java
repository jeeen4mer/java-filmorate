package ru.yandex.practicum.filmorate.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

@RestController
@RequestMapping("/test")
public class TestController {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public TestController(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    @DeleteMapping("/clear")
    public void clearAllData() {
        filmStorage.clear();
        userStorage.clear();
    }
}