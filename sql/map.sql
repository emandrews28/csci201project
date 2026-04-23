-- Migration for the map feature.
-- Safe to run multiple times.

ALTER TABLE restaurants
    ADD COLUMN IF NOT EXISTS address TEXT,
    ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS cuisine TEXT,
    ADD COLUMN IF NOT EXISTS price_tier INT,
    ADD COLUMN IF NOT EXISTS is_open_now BOOLEAN,
    ADD COLUMN IF NOT EXISTS avg_rating NUMERIC(3, 2),
    ADD COLUMN IF NOT EXISTS review_count INT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_restaurants_lat_lng ON restaurants(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_restaurants_cuisine ON restaurants(cuisine);
CREATE INDEX IF NOT EXISTS idx_restaurants_price_tier ON restaurants(price_tier);
