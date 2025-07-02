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
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private UserController controller;
    private User user;
    private User user1;
    private User user2;

    @BeforeEach
    public void beforeEach() {
        controller = new UserController();
        user = User.builder()
                .email("email@mail.ru")
                .login("Login")
                .name("Name")
                .birthday(LocalDate.of(2000, 12, 12))
                .build();
        user1 = User.builder()
                .id(1L)
                .email("email1995@mail.ru")
                .login("Login95")
                .name("NameBeauty")
                .birthday(LocalDate.of(1999, 12, 12))
                .build();
        user2 = User.builder()
                .id(555L)
                .email("email1989@mail.ru")
                .login("Login89")
                .name("NameCool")
                .birthday(LocalDate.of(1989, 12, 12))
                .build();
    }

    @Test
    @DisplayName("тест создания пользователя")
    void testCreate() {
        controller.add(user);
        final Collection<User> users = new ArrayList<>(controller.findAll());

        assertNotNull(users, "Пользователь не найден");
        assertEquals(1, users.size(), "Неверное количество пользователей");
    }

    @Test
    @DisplayName("тест создания пользователя с отсутствующим логином")
    void testCreateNullLogin() throws Exception {
        String userJson = "{ " +
                "\"email\": \"email@mail.ru\", " +
                "\"login\": \"\", " +
                "\"name\": \"NameUser\", " +
                "\"birthday\": \"2000-02-02\"" +
                "}";
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessages").isArray())
                .andExpect(jsonPath("$.errorMessages[0]").value(
                        "Логин не указан"
                ));
    }

    @Test
    @DisplayName("тест создания пользователя с неправильной датой рождения")
    void testCreateFailBirthday() throws Exception {
        String userJson = "{ " +
                "\"email\": \"email@mail.ru\", " +
                "\"login\": \"Login\", " +
                "\"name\": \"NameUser\", " +
                "\"birthday\": \"2026-02-02\"" +
                "}";
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessages").isArray())
                .andExpect(jsonPath("$.errorMessages[0]").value(
                        "Дата рождения не может быть в будущем"
                ));
    }

    @Test
    @DisplayName("тест создания пользователя с отсутствующей почтой")
    void testCreateNullEmail() throws Exception {
        String userJson = "{ " +
                "\"email\": \"\", " +
                "\"login\": \"Login\", " +
                "\"name\": \"NameUser\", " +
                "\"birthday\": \"2000-02-02\"" +
                "}";
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessages").isArray())
                .andExpect(jsonPath("$.errorMessages[0]").value(
                        "Электронная почта не указана"
                ));
    }

    @Test
    @DisplayName("тест на обновление пользователя")
    void testUpdateUser() {
        controller.add(user);
        controller.update(user1);
        Map<Long, User> users = controller.findAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        assertNotNull(users, "Пользователь не найден");
        assertEquals(1, users.size(), "Неверное количество пользователей");
        assertTrue(users.containsKey(1L), "Пользователь не совпадает");
        assertEquals(user1, users.get(1L), "Пользователь не совпадает");
    }

    @Test
    @DisplayName("тест обновления когда номер id некорректен")
    void testUpdateFail() {
        controller.add(user);
        final NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> controller.update(user2));
        assertEquals("Пользователь с id=555 не найден", exception.getMessage());

        Map<Long, User> users = controller.findAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        assertNotNull(users, "Пользователь не найден");
        assertEquals(1, users.size(), "Неверное количество пользователей");
        assertTrue(users.containsKey(1L), "Пользователь не совпадает");
    }
}