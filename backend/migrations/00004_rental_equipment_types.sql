-- +goose Up
CREATE TABLE rental_equipment_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE CHECK (code IN ('shoes', 'harness', 'helmet', 'chalk')),
    name TEXT NOT NULL,
    default_price NUMERIC(10, 2) NOT NULL CHECK (default_price >= 0)
);

-- +goose Down
DROP TABLE IF EXISTS rental_equipment_types;
