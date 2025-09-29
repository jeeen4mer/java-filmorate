package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.MpaStorage;

import java.util.Collection;

@RestController
@RequestMapping("/mpa")
@RequiredArgsConstructor

public class MpaController {

    private final MpaStorage mpaStorage;

    @GetMapping
    public Collection<MpaRating> getAllMpa() {
        return mpaStorage.getAll();
    }

    @GetMapping("/{id}")
    public MpaRating getMpaById(@PathVariable Integer id) {
        return mpaStorage.getById(id);
    }
}