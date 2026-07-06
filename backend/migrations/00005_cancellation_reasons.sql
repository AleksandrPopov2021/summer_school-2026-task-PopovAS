-- +goose Up
CREATE TABLE cancellation_reasons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    apology_text TEXT NOT NULL
);

-- +goose Down
DROP TABLE IF EXISTS cancellation_reasons;
