package ru.yandex.practicum.filmorate.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.http.MediaType.APPLICATION_JSON;
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
                        .contentType(APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessages[0]").value("Дата рождения не может быть в будущем"));
    }

    @Test
    @DisplayName("Создание пользователя с пустым email")
    void createUserWithEmptyEmail_ShouldReturnBadRequest() throws Exception {
        String userJson = "{ \"email\": \"\", \"login\": \"login\", \"name\": \"name\", \"birthday\": \"1990-01-01\"}";
        mockMvc.perform(post("/users")
                        .contentType(APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessages[0]").value("Электронная почта не указана"));
    }

    @Test
    @DisplayName("Создание пользователя с пробелами в логине")
    void createUserWithSpacesInLogin_ShouldReturnBadRequest() throws Exception {
        String userJson = "{\"email\":\"test@test.ru\",\"login\":\"log in\",\"name\":\"name\",\"birthday\":\"1990-01-01\"}";
        mockMvc.perform(post("/users")
                        .contentType(APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessages[0]").value("Логин не должен содержать пробелы"));
    }

    @Test
    @DisplayName("Добавление в друзья без подтверждения")
    void shouldAddFriendImmediately() throws Exception {

        String user1Json = "{\"email\":\"u1@test.ru\",\"login\":\"u1\",\"birthday\":\"1990-01-01\"}";
        String user2Json = "{\"email\":\"u2@test.ru\",\"login\":\"u2\",\"birthday\":\"1990-01-01\"}";

        MvcResult r1 = mockMvc.perform(post("/users").contentType(APPLICATION_JSON).content(user1Json)).andExpect(status().isOk()).andReturn();
        MvcResult r2 = mockMvc.perform(post("/users").contentType(APPLICATION_JSON).content(user2Json)).andExpect(status().isOk()).andReturn();

        long id1 = extractIdFromJson(r1.getResponse().getContentAsString());
        long id2 = extractIdFromJson(r2.getResponse().getContentAsString());

        mockMvc.perform(put("/users/{id}/friends/{friendId}", id1, id2))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/{id}/friends", id1))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id2));

        mockMvc.perform(get("/users/{id}/friends", id2))
                .andExpect(jsonPath("$.length()").value(0));
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
                        .contentType(APPLICATION_JSON)
                        .content(user1Json))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult result2 = mockMvc.perform(post("/users")
                        .contentType(APPLICATION_JSON)
                        .content(user2Json))
                .andExpect(status().isOk())
                .andReturn();

        Integer userId1 = extractIdFromJson(result1.getResponse().getContentAsString());
        Integer userId2 = extractIdFromJson(result2.getResponse().getContentAsString());

        mockMvc.perform(put("/users/{id}/friends/{friendId}", userId1, userId2))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/users/{id}/friends/{friendId}", userId1, userId2))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/{id}/friends", userId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Получение общих друзей")
    void shouldGetCommonFriends() throws Exception {

        String user1Json = "{\"email\":\"u1@test.ru\",\"login\":\"u1\",\"birthday\":\"1990-01-01\"}";
        String user2Json = "{\"email\":\"u2@test.ru\",\"login\":\"u2\",\"birthday\":\"1990-01-01\"}";
        String user3Json = "{\"email\":\"u3@test.ru\",\"login\":\"u3\",\"birthday\":\"1990-01-01\"}";

        MvcResult r1 = mockMvc.perform(post("/users").contentType(APPLICATION_JSON).content(user1Json)).andExpect(status().isOk()).andReturn();
        MvcResult r2 = mockMvc.perform(post("/users").contentType(APPLICATION_JSON).content(user2Json)).andExpect(status().isOk()).andReturn();
        MvcResult r3 = mockMvc.perform(post("/users").contentType(APPLICATION_JSON).content(user3Json)).andExpect(status().isOk()).andReturn();

        long id1 = extractIdFromJson(r1.getResponse().getContentAsString());
        long id2 = extractIdFromJson(r2.getResponse().getContentAsString());
        long id3 = extractIdFromJson(r3.getResponse().getContentAsString());

        mockMvc.perform(put("/users/{id}/friends/{friendId}", id1, id3)).andExpect(status().isNoContent());
        mockMvc.perform(put("/users/{id}/friends/{friendId}", id2, id3)).andExpect(status().isNoContent());

        mockMvc.perform(get("/users/{id}/friends/common/{otherId}", id1, id2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id3));
    }
}