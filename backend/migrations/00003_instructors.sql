-- +goose Up
CREATE TABLE instructors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name TEXT NOT NULL,
    average_rating NUMERIC(3, 2) CHECK (
        average_rating IS NULL OR (average_rating >= 1 AND average_rating <= 5)
    ),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- +goose Down
DROP TABLE IF EXISTS instructors;
