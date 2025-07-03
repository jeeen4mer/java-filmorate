package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.filmorate.validation.CreateValidationGroup;
import ru.yandex.practicum.filmorate.validation.UpdateValidationGroup;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
    @Null(groups = CreateValidationGroup.class, message = "ID должен быть null при создании")
    @NotNull(groups = UpdateValidationGroup.class, message = "ID не может быть null при обновлении")
    Long id;

    @NotBlank(message = "Электронная почта не указана")
    @Email(message = "Некорректный формат электронной почты")
    String email;

    @NotBlank(message = "Логин не указан")
    @Pattern(regexp = "\\S+", message = "Логин не должен содержать пробелы")
    String login;

    String name;

    @PastOrPresent(message = "Дата рождения не может быть в будущем")
    LocalDate birthday;

    public String getName() {
        return name == null || name.isBlank() ? login : name;
    }
}