-- +goose Up
CREATE TABLE training_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    starts_at TIMESTAMPTZ NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 90 CHECK (duration_minutes > 0),
    capacity INT NOT NULL CHECK (capacity > 0),
    free_spots INT NOT NULL CHECK (free_spots >= 0),
    training_price NUMERIC(10, 2) NOT NULL CHECK (training_price >= 0),
    rental_tariff NUMERIC(10, 2) CHECK (rental_tariff IS NULL OR rental_tariff >= 0),
    slot_status TEXT NOT NULL CHECK (slot_status IN ('active', 'cancelled_by_gym')),
    address TEXT NOT NULL,
    zone_id UUID NOT NULL REFERENCES training_zones (id),
    instructor_id UUID NOT NULL REFERENCES instructors (id),
    venue_id UUID NOT NULL REFERENCES gym_venues (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (free_spots <= capacity)
);

CREATE INDEX idx_training_slots_starts_at ON training_slots (starts_at);
CREATE INDEX idx_training_slots_status_starts_at ON training_slots (slot_status, starts_at);

-- +goose Down
DROP TABLE IF EXISTS training_slots;
