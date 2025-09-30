package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.filmorate.validation.MinReleaseDate;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(of = "id")
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class Film {
    public static final int MAX_DESCRIPTION_LENGTH = 200;

    Long id;

    @NotBlank(message = "Название фильма не указано")
    String name;

    @Size(max = MAX_DESCRIPTION_LENGTH, message = "Описание фильма превышает 200 символов")
    @NotBlank(message = "Описание фильма не указано")
    String description;

    @NotNull(message = "Дата релиза обязательна")
    @MinReleaseDate(message = "Дата релиза должна быть не раньше 28 декабря 1895 года")
    LocalDate releaseDate;

    @Positive(message = "Продолжительность фильма должна быть более 1 минуты")
    Integer duration;

    @Valid
    @NotNull(message = "Рейтинг MPA обязателен")
    MpaRating mpa;

    @Singular
    @Valid
    Set<Genre> genres = new HashSet<>();

    @Builder.Default
    @JsonIgnore
    Set<Long> likes = new HashSet<>();

    transient int likesCount;

    public void addLike(Long userId) {
        if (userId == null || likes == null) return;
        likes.add(userId);
    }

    public void removeLike(Long userId) {
        if (userId == null || likes == null) return;
        likes.remove(userId);
    }

    public int getCurrentLikesCount() {
        return likes == null ? 0 : likes.size();
    }
}