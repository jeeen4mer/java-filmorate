package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Создание пользователя с будущей датой рождения")
    void createUserWithFutureBirthday_ShouldReturnBadRequest() throws Exception {
        String userJson = String.format("{\"email\":\"test@test.ru\",\"login\":\"login\",\"name\":\"name\",\"birthday\":\"%s\"}",
                LocalDate.now().plusDays(1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessages[0]").value("Дата рождения не может быть в будущем"));
    }

    @Test
    @DisplayName("Создание пользователя с пустым email")
    void createUserWithEmptyEmail_ShouldReturnBadRequest() throws Exception {
        String userJson = "{ \"email\": \"\", \"login\": \"login\", \"name\": \"name\", \"birthday\": \"1990-01-01\"}";

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessages[0]").value("Электронная почта не указана"));
    }

    @Test
    @DisplayName("Создание пользователя с пробелами в логине")
    void createUserWithSpacesInLogin_ShouldReturnBadRequest() throws Exception {
        String userJson = "{\"email\":\"test@test.ru\",\"login\":\"log in\",\"name\":\"name\",\"birthday\":\"1990-01-01\"}";

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessages[0]").value("Логин не должен содержать пробелы"));
    }
}