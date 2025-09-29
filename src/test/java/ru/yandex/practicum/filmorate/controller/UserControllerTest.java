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
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc

class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    public void beforeEach() throws Exception {
        mockMvc.perform(delete("/test/clear"));
    }

    @Test
    @DisplayName("Создание пользователя с будущей датой рождения")
    void createUserWithFutureBirthday_ShouldReturnBadRequest() throws Exception {
        String userJson = String.format("{\"email\":\"test@test.ru\",\"login\":\"login\",\"name\":\"name\",\"birthday\":\"%s\"}",
                java.time.LocalDate.now().plusDays(1));
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
    @DisplayName("Добавление и подтверждение дружбы")
    void shouldAddAndConfirmFriend() throws Exception {

        String user1Json = "{\"email\":\"user1@test.ru\",\"login\":\"user1\",\"name\":\"User One\",\"birthday\":\"1990-01-01\"}";
        String user2Json = "{\"email\":\"user2@test.ru\",\"login\":\"user2\",\"name\":\"User Two\",\"birthday\":\"1990-01-01\"}";

        MvcResult result1 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(user1Json))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult result2 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(user2Json))
                .andExpect(status().isOk())
                .andReturn();

        Integer userId1 = extractIdFromJson(result1.getResponse().getContentAsString());
        Integer userId2 = extractIdFromJson(result2.getResponse().getContentAsString());

        mockMvc.perform(put("/users/{id}/friends/{friendId}", userId1, userId2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/{id}/friends", userId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(put("/users/{id}/friends/confirm/{friendId}", userId2, userId1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/{id}/friends", userId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(userId2));
    }

    private Integer extractIdFromJson(String jsonResponse) throws Exception {
        return JsonPath.parse(jsonResponse).read("$.id", Integer.class);
    }

    @Test
    @DisplayName("Удаление из друзей")
    void shouldRemoveFriend() throws Exception {

        String user1Json = "{\"email\":\"user1@test.ru\",\"login\":\"user1\",\"birthday\":\"1990-01-01\"}";
        String user2Json = "{\"email\":\"user2@test.ru\",\"login\":\"user2\",\"birthday\":\"1990-01-01\"}";

        MvcResult result1 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(user1Json))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult result2 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(user2Json))
                .andExpect(status().isOk())
                .andReturn();

        Integer userId1 = extractIdFromJson(result1.getResponse().getContentAsString());
        Integer userId2 = extractIdFromJson(result2.getResponse().getContentAsString());

        mockMvc.perform(put("/users/{id}/friends/{friendId}", userId1, userId2))
                .andExpect(status().isOk());

        mockMvc.perform(put("/users/{id}/friends/confirm/{friendId}", userId2, userId1))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/users/{id}/friends/{friendId}", userId1, userId2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/{id}/friends", userId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Получение общих друзей")
    void shouldGetCommonFriends() throws Exception {

        String user1Json = "{\"email\":\"user1@test.ru\",\"login\":\"user1\",\"birthday\":\"1990-01-01\"}";
        String user2Json = "{\"email\":\"user2@test.ru\",\"login\":\"user2\",\"birthday\":\"1990-01-01\"}";
        String user3Json = "{\"email\":\"user3@test.ru\",\"login\":\"user3\",\"birthday\":\"1990-01-01\"}";

        MvcResult result1 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(user1Json))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult result2 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(user2Json))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult result3 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(user3Json))
                .andExpect(status().isOk())
                .andReturn();

        Integer userId1 = extractIdFromJson(result1.getResponse().getContentAsString());
        Integer userId2 = extractIdFromJson(result2.getResponse().getContentAsString());
        Integer userId3 = extractIdFromJson(result3.getResponse().getContentAsString());

        mockMvc.perform(put("/users/{id}/friends/{friendId}", userId1, userId3))
                .andExpect(status().isOk());

        mockMvc.perform(put("/users/{id}/friends/confirm/{friendId}", userId3, userId1))
                .andExpect(status().isOk());

        mockMvc.perform(put("/users/{id}/friends/{friendId}", userId2, userId3))
                .andExpect(status().isOk());

        mockMvc.perform(put("/users/{id}/friends/confirm/{friendId}", userId3, userId2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/{id}/friends/common/{otherId}", userId1, userId2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(userId3));
    }
}