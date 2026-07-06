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

CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL UNIQUE REFERENCES clients (id) ON DELETE CASCADE,
    booking_confirmation_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    rating_invitation_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    gym_cancellation_enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE device_push_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    token TEXT NOT NULL,
    platform TEXT NOT NULL CHECK (platform IN ('ios', 'android')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (client_id, platform)
);

CREATE INDEX idx_device_push_tokens_client_id ON device_push_tokens (client_id);
