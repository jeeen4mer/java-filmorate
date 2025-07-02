package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    Long id;

    @NotBlank(message = "Электронная почта не указана")
    @Email(message = "Некорректный формат электронной почты")
    String email;

    @NotBlank(message = "Логин не указан")
    String login;

    String name;

    @PastOrPresent(message = "Дата рождения не может быть в будущем")
    LocalDate birthday;
}