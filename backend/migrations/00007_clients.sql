-- +goose Up
CREATE TABLE clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name TEXT NOT NULL CHECK (char_length(full_name) BETWEEN 1 AND 200),
    phone TEXT NOT NULL UNIQUE,
    birth_date DATE NOT NULL,
    risk_consent_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    completed_visits_count INT NOT NULL DEFAULT 0 CHECK (completed_visits_count >= 0),
    is_loyal_client BOOLEAN NOT NULL DEFAULT FALSE,
    loyalty_discount NUMERIC(10, 2) CHECK (loyalty_discount IS NULL OR loyalty_discount >= 0),
    late_cancellation_count INT NOT NULL DEFAULT 0 CHECK (late_cancellation_count >= 0),
    no_show_count INT NOT NULL DEFAULT 0 CHECK (no_show_count >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- +goose Down
DROP TABLE IF EXISTS clients;
