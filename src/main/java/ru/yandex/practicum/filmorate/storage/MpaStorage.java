package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.Collection;

public interface MpaStorage {

    Collection<MpaRating> getAll();

    MpaRating getById(Integer id);
}