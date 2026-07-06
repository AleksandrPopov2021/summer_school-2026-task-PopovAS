-- +goose Up
CREATE TABLE instructor_ratings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    instructor_id UUID NOT NULL REFERENCES instructors (id),
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings (id) ON DELETE CASCADE,
    stars INT NOT NULL CHECK (stars BETWEEN 1 AND 5),
    rated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_instructor_ratings_instructor_id ON instructor_ratings (instructor_id);
CREATE INDEX idx_instructor_ratings_client_id ON instructor_ratings (client_id);

ALTER TABLE sent_push_notifications
    DROP CONSTRAINT IF EXISTS sent_push_notifications_notification_type_check;

ALTER TABLE sent_push_notifications
    ADD CONSTRAINT sent_push_notifications_notification_type_check CHECK (
        notification_type IN (
            'booking_confirmed',
            'reminder_day_before',
            'reminder_hours_before',
            'gym_cancellation',
            'rating_invitation'
        )
    );

-- +goose Down
ALTER TABLE sent_push_notifications
    DROP CONSTRAINT IF EXISTS sent_push_notifications_notification_type_check;

ALTER TABLE sent_push_notifications
    ADD CONSTRAINT sent_push_notifications_notification_type_check CHECK (
        notification_type IN (
            'booking_confirmed',
            'reminder_day_before',
            'reminder_hours_before',
            'gym_cancellation'
        )
    );

DROP TABLE IF EXISTS instructor_ratings;
