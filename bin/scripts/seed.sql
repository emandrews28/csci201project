-- Local Postgres schema for smoke-testing the photo-uploads feature.
-- This mirrors the columns referenced by UserDAO / ReviewDAO / RankingDAO.
-- Applied in addition to sql/photos.sql.

CREATE TABLE IF NOT EXISTS users (
    user_id       BIGSERIAL PRIMARY KEY,
    username      TEXT UNIQUE NOT NULL,
    email         TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS restaurants (
    restaurant_id BIGSERIAL PRIMARY KEY,
    name          TEXT NOT NULL,
    avg_rating    NUMERIC(3, 2),
    review_count  INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS reviews (
    review_id     BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(restaurant_id) ON DELETE CASCADE,
    ranking_score INT NOT NULL CHECK (ranking_score BETWEEN 1 AND 10),
    review_text   TEXT,
    timestamp     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, restaurant_id)
);

CREATE TABLE IF NOT EXISTS rankings (
    ranking_id    BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(restaurant_id) ON DELETE CASCADE,
    rank_position INT NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, restaurant_id)
);

INSERT INTO restaurants (restaurant_id, name, avg_rating, review_count)
VALUES (1, 'Test Restaurant', NULL, 0)
ON CONFLICT (restaurant_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('restaurants', 'restaurant_id'),
              GREATEST((SELECT COALESCE(MAX(restaurant_id), 1) FROM restaurants), 1));
