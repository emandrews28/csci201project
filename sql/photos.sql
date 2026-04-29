-- Photos feature schema
-- Run once against the Supabase Postgres database.

CREATE TABLE IF NOT EXISTS photos (
    photo_id      BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(restaurant_id) ON DELETE CASCADE,
    review_id     BIGINT REFERENCES reviews(review_id) ON DELETE SET NULL,
    image_url     TEXT NOT NULL,
    caption       TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_photos_restaurant ON photos(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_photos_user       ON photos(user_id);
CREATE INDEX IF NOT EXISTS idx_photos_review     ON photos(review_id);

-- Optional tag extension
CREATE TABLE IF NOT EXISTS photo_tags (
    tag_id   BIGSERIAL PRIMARY KEY,
    photo_id BIGINT NOT NULL REFERENCES photos(photo_id) ON DELETE CASCADE,
    tag      TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_photo_tags_photo ON photo_tags(photo_id);
