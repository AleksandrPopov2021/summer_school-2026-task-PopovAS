-- +goose Up
CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    slot_id UUID NOT NULL REFERENCES training_slots (id),
    booking_status TEXT NOT NULL CHECK (
        booking_status IN ('booked', 'cancelled_by_client', 'cancelled_by_gym', 'completed', 'no_show')
    ),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    cancelled_at TIMESTAMPTZ,
    uses_own_equipment BOOLEAN NOT NULL,
    cancellation_reason_id UUID REFERENCES cancellation_reasons (id),
    rebooking_forbidden BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_bookings_client_id ON bookings (client_id);
CREATE INDEX idx_bookings_slot_id ON bookings (slot_id);
CREATE INDEX idx_bookings_client_slot ON bookings (client_id, slot_id);

-- +goose Down
DROP TABLE IF EXISTS bookings;
