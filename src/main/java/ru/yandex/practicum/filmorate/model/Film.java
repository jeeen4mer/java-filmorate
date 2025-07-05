package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Builder;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(of = {"id"})
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Film {
    static final int MAX_DESCRIPTION_LENGTH = 200;

    Long id;

    @NotBlank(message = "Название фильма не указано")
    String name;

    @Size(max = MAX_DESCRIPTION_LENGTH,
            message = "Описание фильма превышает 200 символов")
    @NotBlank(message = "Описание фильма не указано")
    String description;

    @NotNull(message = "Дата релиза обязательна")
    LocalDate releaseDate;

    @Positive(message = "Продолжительность фильма должна быть более 1 минуты")
    Integer duration;

    @Builder.Default
    Set<Long> likes = new HashSet<>();

    public void addLike(Long userId) {
        if (userId == null) return;
        if (likes == null) {
            likes = new HashSet<>();
        }
        likes.add(userId);
    }

    public void removeLike(Long userId) {
        if (userId == null || likes == null) return;
        likes.remove(userId);
    }

    public int getLikesCount() {
        return likes == null ? 0 : likes.size();
    }
}