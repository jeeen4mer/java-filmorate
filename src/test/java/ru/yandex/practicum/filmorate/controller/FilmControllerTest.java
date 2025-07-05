package ru.yandex.practicum.filmorate.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class FilmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    public void beforeEach() throws Exception {
        mockMvc.perform(delete("/test/clear"));
    }

    @Test
    @DisplayName("Создание фильма успешно")
    void shouldCreateFilm() throws Exception {
        String filmJson = "{"
                + "\"name\":\"Interstellar\","
                + "\"description\":\"Космическая одиссея\","
                + "\"releaseDate\":\"2014-11-05\","
                + "\"duration\":169"
                + "}";

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filmJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Interstellar"));
    }

    @Test
    @DisplayName("Ошибка создания: отсутствует название фильма")
    void shouldReturnErrorWhenNameIsEmpty() throws Exception {
        String filmJson = "{"
                + "\"name\":\"\","
                + "\"description\":\"Описание\","
                + "\"releaseDate\":\"2014-11-05\","
                + "\"duration\":120"
                + "}";

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filmJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessages").isArray())
                .andExpect(jsonPath("$.errorMessages[0]").value("Название фильма не указано"));
    }

    @Test
    @DisplayName("Ошибка создания: дата релиза раньше 28 декабря 1895")
    void shouldReturnErrorWhenReleaseDateIsInvalid() throws Exception {
        String filmJson = "{"
                + "\"name\":\"Old Movie\","
                + "\"description\":\"Старый фильм\","
                + "\"releaseDate\":\"1800-01-01\","
                + "\"duration\":120"
                + "}";

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filmJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Дата релиза должна быть не раньше 28 декабря 1895 года"));
    }

    @Test
    @DisplayName("Ошибка создания: продолжительность меньше 1 минуты")
    void shouldReturnErrorWhenDurationIsNegative() throws Exception {
        String filmJson = "{"
                + "\"name\":\"Short Film\","
                + "\"description\":\"Очень короткий фильм\","
                + "\"releaseDate\":\"2014-11-05\","
                + "\"duration\":-10"
                + "}";

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filmJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessages").isArray())
                .andExpect(jsonPath("$.errorMessages[0]").value("Продолжительность фильма должна быть более 1 минуты"));
    }

    @Test
    @DisplayName("Обновление существующего фильма")
    void shouldUpdateFilm() throws Exception {
        String createJson = "{"
                + "\"name\":\"Matrix\","
                + "\"description\":\"Фильм о реальности\","
                + "\"releaseDate\":\"1999-03-31\","
                + "\"duration\":136"
                + "}";

        String updateJson = "{"
                + "\"id\":1,"
                + "\"name\":\"Matrix Updated\","
                + "\"description\":\"Изменённое описание\","
                + "\"releaseDate\":\"1999-03-31\","
                + "\"duration\":140"
                + "}";

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk());

        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Matrix Updated"))
                .andExpect(jsonPath("$.description").value("Изменённое описание"));
    }

    @Test
    @DisplayName("Ошибка обновления: фильм не найден")
    void shouldReturnNotFoundWhenUpdatingNonExistingFilm() throws Exception {
        String updateJson = "{"
                + "\"id\":999,"
                + "\"name\":\"Неизвестный фильм\","
                + "\"description\":\"Нет такого\","
                + "\"releaseDate\":\"2000-01-01\","
                + "\"duration\":120"
                + "}";

        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Фильм не найден"));
    }

    @Test
    @DisplayName("Добавление лайка фильму")
    void shouldAddLikeToFilm() throws Exception {
        String filmJson = "{"
                + "\"name\":\"Test Film\","
                + "\"description\":\"Description\","
                + "\"releaseDate\":\"2020-01-01\","
                + "\"duration\":120"
                + "}";

        String response = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filmJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Integer filmId = extractFilmIdFromJson(response);

        mockMvc.perform(put("/films/{id}/like/{userId}", filmId, 101))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Получение списка популярных фильмов")
    void shouldGetPopularFilms() throws Exception {
        String film1Json = "{\"name\":\"Film1\",\"description\":\"Desc1\",\"releaseDate\":\"2000-01-01\",\"duration\":120}";
        String film2Json = "{\"name\":\"Film2\",\"description\":\"Desc2\",\"releaseDate\":\"2000-01-01\",\"duration\":120}";

        String response1 = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(film1Json))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String response2 = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(film2Json))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Integer film1Id = extractFilmIdFromJson(response1);
        Integer film2Id = extractFilmIdFromJson(response2);

        mockMvc.perform(put("/films/{id}/like/{userId}", film1Id, 101));
        mockMvc.perform(put("/films/{id}/like/{userId}", film1Id, 102));

        mockMvc.perform(put("/films/{id}/like/{userId}", film2Id, 103));

        mockMvc.perform(get("/films/popular?count=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(film1Id))
                .andExpect(jsonPath("$[1].id").value(film2Id));
    }

    private Integer extractFilmIdFromJson(String jsonResponse) throws Exception {
        return JsonPath.parse(jsonResponse).read("$.id");
    }
}