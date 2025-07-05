package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @Test
    @DisplayName("Добавление и удаление друзей")
    void shouldAddAndRemoveFriend() throws Exception {
        String user1 = "{\"email\":\"user1@test.ru\",\"login\":\"user1\",\"name\":\"User One\",\"birthday\":\"1990-01-01\"}";
        String user2 = "{\"email\":\"user2@test.ru\",\"login\":\"user2\",\"name\":\"User Two\",\"birthday\":\"1990-01-01\"}";

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(user1));
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(user2));

        mockMvc.perform(put("/users/1/friends/2")).andExpect(status().isOk());

        mockMvc.perform(get("/users/1/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2));

        mockMvc.perform(delete("/users/1/friends/2")).andExpect(status().isOk());

        mockMvc.perform(get("/users/1/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Получение общих друзей")
    void shouldGetCommonFriends() throws Exception {
        String user1 = "{\"email\":\"user1@test.ru\",\"login\":\"user1\",\"name\":\"User One\",\"birthday\":\"1990-01-01\"}";
        String user2 = "{\"email\":\"user2@test.ru\",\"login\":\"user2\",\"name\":\"User Two\",\"birthday\":\"1990-01-01\"}";
        String user3 = "{\"email\":\"user3@test.ru\",\"login\":\"user3\",\"name\":\"User Three\",\"birthday\":\"1990-01-01\"}";

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(user1));
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(user2));
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(user3));

        mockMvc.perform(put("/users/1/friends/2"));
        mockMvc.perform(put("/users/1/friends/3"));
        mockMvc.perform(put("/users/2/friends/3"));

        mockMvc.perform(get("/users/1/friends/common/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(3));
    }
}