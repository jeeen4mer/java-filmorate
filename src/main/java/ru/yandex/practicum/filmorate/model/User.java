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

    @NotBlank(groups = CreateValidationGroup.class, message = "Электронная почта не указана")
    @Email(groups = {CreateValidationGroup.class, UpdateValidationGroup.class}, message = "Некорректный формат электронной почты")
    String email;

    @NotBlank(groups = CreateValidationGroup.class, message = "Логин не указан")
    @Pattern(regexp = "\\S+", groups = {CreateValidationGroup.class, UpdateValidationGroup.class},
            message = "Логин не должен содержать пробелы")
    String login;

    @Size(groups = {CreateValidationGroup.class, UpdateValidationGroup.class},
            min = 1, max = 50, message = "Имя должно быть от 1 до 50 символов")
    String name;

    @PastOrPresent(groups = {CreateValidationGroup.class, UpdateValidationGroup.class},
            message = "Дата рождения не может быть в будущем")
    LocalDate birthday;

    public String getName() {
        return name == null || name.isBlank() ? login : name;
    }
}