-- Search Filters feature schema
-- Run once in the Supabase SQL editor.
-- Extends the existing restaurants table; adds cuisine lookup + junction tables.

-- 1. Add missing columns to the existing restaurants table
--    NOTE: address, cuisine_type, avg_rating, review_count already exist — do NOT re-add them.
ALTER TABLE restaurants
    ADD COLUMN IF NOT EXISTS latitude        DECIMAL(9, 6),
    ADD COLUMN IF NOT EXISTS longitude       DECIMAL(9, 6),
    ADD COLUMN IF NOT EXISTS price_tier      INT CHECK (price_tier BETWEEN 1 AND 4),
    ADD COLUMN IF NOT EXISTS google_place_id TEXT,
    ADD COLUMN IF NOT EXISTS created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- NOTE: cuisine_type (varchar) already exists on restaurants and is kept for
-- backward compatibility. The cuisines + restaurant_cuisines tables below power
-- proper multi-cuisine filtering.

-- 2. Cuisines lookup table
CREATE TABLE IF NOT EXISTS cuisines (
    cuisine_id BIGSERIAL PRIMARY KEY,
    name       TEXT NOT NULL,
    slug       TEXT NOT NULL UNIQUE,
    parent_id  BIGINT REFERENCES cuisines(cuisine_id) ON DELETE SET NULL
);

-- 3. Junction table for multi-cuisine tagging per restaurant
CREATE TABLE IF NOT EXISTS restaurant_cuisines (
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(restaurant_id) ON DELETE CASCADE,
    cuisine_id    BIGINT NOT NULL REFERENCES cuisines(cuisine_id) ON DELETE CASCADE,
    is_primary    BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (restaurant_id, cuisine_id)
);

-- 4. Indexes
CREATE INDEX IF NOT EXISTS idx_restaurants_location        ON restaurants(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_restaurants_price_tier      ON restaurants(price_tier);
CREATE INDEX IF NOT EXISTS idx_restaurant_cuisines_cuisine ON restaurant_cuisines(cuisine_id);
-- follows table (follower_id / following_id) already exists — no changes needed.

-- 5. Seed cuisines
INSERT INTO cuisines (name, slug, parent_id) VALUES
    ('American',  'american',  NULL),
    ('Italian',   'italian',   NULL),
    ('Japanese',  'japanese',  NULL),
    ('Mexican',   'mexican',   NULL),
    ('Chinese',   'chinese',   NULL),
    ('Indian',    'indian',    NULL),
    ('Thai',      'thai',      NULL),
    ('French',    'french',    NULL),
    ('Korean',    'korean',    NULL),
    ('Greek',     'greek',     NULL),
    ('Sushi',     'sushi',     (SELECT cuisine_id FROM cuisines WHERE slug = 'japanese')),
    ('Ramen',     'ramen',     (SELECT cuisine_id FROM cuisines WHERE slug = 'japanese')),
    ('Pizza',     'pizza',     (SELECT cuisine_id FROM cuisines WHERE slug = 'italian')),
    ('Burgers',   'burgers',   (SELECT cuisine_id FROM cuisines WHERE slug = 'american')),
    ('Tacos',     'tacos',     (SELECT cuisine_id FROM cuisines WHERE slug = 'mexican'))
ON CONFLICT (slug) DO NOTHING;
