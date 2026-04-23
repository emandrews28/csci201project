-- Wishlist feature: restaurants a user wants to try.
-- Each entry links a user to a restaurant with optional notes and a timestamp.

CREATE TABLE IF NOT EXISTS wishlist (
    wishlist_id   BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(restaurant_id) ON DELETE CASCADE,
    notes         TEXT,
    added_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, restaurant_id)
);

CREATE INDEX IF NOT EXISTS idx_wishlist_user_id ON wishlist(user_id);
