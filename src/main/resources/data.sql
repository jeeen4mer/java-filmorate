INSERT INTO mpa_ratings (rating_id, name) VALUES
(1, 'G'),
(2, 'PG'),
(3, 'PG-13'),
(4, 'R'),
(5, 'NC-17');

INSERT INTO genres (genre_id, name) VALUES
(1, 'Комедия'),
(2, 'Драма'),
(3, 'Мультфильм'),
(4, 'Триллер'),
(5, 'Документальный'),
(6, 'Боевик');

INSERT INTO users (user_id, email, login, name, birthday) VALUES
(1, 'user1@yandex.ru', 'user1login', 'User One', '1990-01-01'),
(2, 'user2@yandex.ru', 'user2login', 'User Two', '1990-01-01'),
(3, 'user3@yandex.ru', 'user3login', 'User Three', '1990-01-01');

INSERT INTO films (film_id, name, description, release_date, duration, rating_id) VALUES
(1, 'Film 1', 'Description 1', '2020-01-01', 120, 1),
(2, 'Film 2', 'Description 2', '2020-01-02', 130, 2);