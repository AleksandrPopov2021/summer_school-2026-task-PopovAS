-- +goose Up
CREATE TABLE training_zones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    format_type TEXT NOT NULL CHECK (format_type IN ('bouldering_instruction', 'rope_routes')),
    difficulty TEXT NOT NULL CHECK (difficulty IN ('beginner', 'experienced')),
    max_group_size INT NOT NULL CHECK (max_group_size > 0)
);

-- +goose Down
DROP TABLE IF EXISTS training_zones;
