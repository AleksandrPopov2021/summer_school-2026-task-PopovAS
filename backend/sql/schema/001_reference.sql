-- Reference tables for iteration 0 (DB-0)

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE gym_venues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    address TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE training_zones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    format_type TEXT NOT NULL CHECK (format_type IN ('bouldering_instruction', 'rope_routes')),
    difficulty TEXT NOT NULL CHECK (difficulty IN ('beginner', 'experienced')),
    max_group_size INT NOT NULL CHECK (max_group_size > 0)
);

CREATE TABLE instructors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name TEXT NOT NULL,
    average_rating NUMERIC(3, 2) CHECK (
        average_rating IS NULL OR (average_rating >= 1 AND average_rating <= 5)
    ),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE rental_equipment_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE CHECK (code IN ('shoes', 'harness', 'helmet', 'chalk')),
    name TEXT NOT NULL,
    default_price NUMERIC(10, 2) NOT NULL CHECK (default_price >= 0)
);

CREATE TABLE cancellation_reasons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    apology_text TEXT NOT NULL
);

CREATE TABLE system_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reminder_hours_before INT NOT NULL CHECK (reminder_hours_before > 0),
    visits_for_loyalty INT NOT NULL CHECK (visits_for_loyalty > 0),
    violations_for_sanctions INT NOT NULL CHECK (violations_for_sanctions > 0),
    booking_cutoff_minutes INT NOT NULL CHECK (booking_cutoff_minutes > 0),
    cancellation_forbidden_minutes INT NOT NULL CHECK (cancellation_forbidden_minutes > 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
