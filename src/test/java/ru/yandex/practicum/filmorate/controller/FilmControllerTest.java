package ru.yandex.practicum.filmorate.controller;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SpringBootTest
@AutoConfigureMockMvc
public class FilmControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private FilmController controller;
    private Film film;
    private Film film1;
    private Film film2;

    @BeforeEach
    public void beforeEach() {
        controller = new FilmController();
        film = Film.builder()
                .name("Name")
                .description("Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .build();
        film1 = Film.builder()
                .id(1L)
                .name("newName")
                .description("newDescription")
                .releaseDate(LocalDate.of(2001, 1, 1))
                .duration(120)
                .build();
        film2 = Film.builder()
                .id(555L)
                .name("newName")
                .description("newDescription")
                .releaseDate(LocalDate.of(2001, 1, 1))
                .duration(120)
                .build();
    }

    @Test
    @DisplayName("тест создания фильма")
    void testCreate() {
        controller.add(film);
        final Collection<Film> films = new ArrayList<>(controller.findAll());

        assertNotNull(films, "Фильм не найден");
        assertEquals(1, films.size(), "Неверное количество фильмов");
    }

    @Test
    @DisplayName("тест создания фильма с отсутствующим названием")
    void testCreateNullName() throws Exception {
        String filmJson = "{ " +
                "\"name\": \"\", " +
                "\"description\": \"TestDescription\", " +
                "\"releaseDate\": \"2020-02-02\", " +
                "\"duration\": 100" +
                "}";
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filmJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessages").isArray())
                .andExpect(jsonPath("$.errorMessages[0]").value(
                        "Название фильма не указано"
                ));
    }

    @Test
    @DisplayName("тест создания фильма с неправильной датой релиза")
    void testCreateFailReleaseDate() throws Exception {
        String filmJson = "{ " +
                "\"name\": \"NameFilm\", " +
                "\"description\": \"TestDescription\", " +
                "\"releaseDate\": \"1800-02-02\", " +
                "\"duration\": 100" +
                "}";
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filmJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessages").isArray())
                .andExpect(jsonPath("$.errorMessages[0]").value(
                        "Дата релиза должна быть не раньше 28 декабря 1895 года"
                ));
    }

    @Test
    @DisplayName("тест создания фильма с отрицательной продолжительностью")
    void testCreateNegativeDuration() throws Exception {
        String filmJson = "{ " +
                "\"name\": \"NameFilm\", " +
                "\"description\": \"TestDescription\", " +
                "\"releaseDate\": \"2014-02-02\", " +
                "\"duration\": -10" +
                "}";
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filmJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessages").isArray())
                .andExpect(jsonPath("$.errorMessages[0]").value(
                        "Продолжительность фильма должна быть более 1 минуты"
                ));
    }

    @Test
    @DisplayName("тест на обновление фильма")
    void testUpdateFilm() {
        controller.add(film);
        controller.update(film1);
        Map<Long, Film> films = controller.findAll().stream()
                .collect(Collectors.toMap(Film::getId, Function.identity()));

        assertNotNull(films, "Фильм не найден");
        assertEquals(1, films.size(), "Неверное количество фильмов");
        assertTrue(films.containsKey(1L), "Фильм не совпадает");
        assertEquals(film1, films.get(1L), "Фильм не совпадает");
    }

    @Test
    @DisplayName("тест обновления когда номер id некорректен")
    void testUpdateFail() {
        controller.add(film);
        final NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> controller.update(film2));
        assertEquals("Фильм с id = 555 не найден", exception.getMessage());

        Map<Long, Film> films = controller.findAll().stream()
                .collect(Collectors.toMap(Film::getId, Function.identity()));

        assertNotNull(films, "Фильм не найден");
        assertEquals(1, films.size(), "Неверное количество фильмов");
        assertTrue(films.containsKey(1L), "Фильм не совпадает");
    }
}