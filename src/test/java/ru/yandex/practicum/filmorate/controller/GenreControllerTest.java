package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GenreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    public void beforeEach() throws Exception {
    }

    @Test
    @DisplayName("Получение списка всех жанров")
    void shouldGetAllGenres() throws Exception {
        mockMvc.perform(get("/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Комедия"));
    }

    @Test
    @DisplayName("Получение жанра по ID")
    void shouldGetGenreById() throws Exception {
        mockMvc.perform(get("/genres/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Комедия"));
    }

    @Test
    @DisplayName("Ошибка получения: жанр не найден")
    void shouldReturnNotFoundWhenGenreDoesNotExist() throws Exception {
        mockMvc.perform(get("/genres/999"))
                .andExpect(status().isNotFound());
    }
}