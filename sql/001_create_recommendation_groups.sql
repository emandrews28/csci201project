-- Migration: create recommendation groups and members tables

CREATE TABLE IF NOT EXISTS recommendation_groups (
    group_id SERIAL PRIMARY KEY,
    created_by BIGINT NOT NULL REFERENCES users(user_id) ON DELETE SET NULL,
    group_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS recommendation_group_members (
    group_id INT NOT NULL REFERENCES recommendation_groups(group_id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, user_id)
);
