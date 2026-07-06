-- +goose Up
CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL UNIQUE REFERENCES clients (id) ON DELETE CASCADE,
    booking_confirmation_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    rating_invitation_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    gym_cancellation_enabled BOOLEAN NOT NULL DEFAULT TRUE
);

-- +goose Down
DROP TABLE IF EXISTS notification_preferences;
