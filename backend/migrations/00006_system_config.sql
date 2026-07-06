-- +goose Up
CREATE TABLE system_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reminder_hours_before INT NOT NULL CHECK (reminder_hours_before > 0),
    visits_for_loyalty INT NOT NULL CHECK (visits_for_loyalty > 0),
    violations_for_sanctions INT NOT NULL CHECK (violations_for_sanctions > 0),
    booking_cutoff_minutes INT NOT NULL CHECK (booking_cutoff_minutes > 0),
    cancellation_forbidden_minutes INT NOT NULL CHECK (cancellation_forbidden_minutes > 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- +goose Down
DROP TABLE IF EXISTS system_config;
