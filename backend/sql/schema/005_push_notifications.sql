CREATE TABLE sent_push_notifications (
    booking_id UUID NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    notification_type TEXT NOT NULL CHECK (
        notification_type IN (
            'booking_confirmed',
            'reminder_day_before',
            'reminder_hours_before',
            'gym_cancellation'
        )
    ),
    sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (booking_id, notification_type)
);

CREATE INDEX idx_sent_push_notifications_sent_at ON sent_push_notifications (sent_at);
