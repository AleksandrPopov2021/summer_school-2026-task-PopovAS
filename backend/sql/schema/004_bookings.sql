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

CREATE TABLE booking_rental_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    equipment_type_id UUID NOT NULL REFERENCES rental_equipment_types (id),
    quantity INT NOT NULL CHECK (quantity >= 1),
    unit_price NUMERIC(10, 2) NOT NULL CHECK (unit_price >= 0)
);

CREATE INDEX idx_booking_rental_lines_booking_id ON booking_rental_lines (booking_id);

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings (id) ON DELETE CASCADE,
    training_amount NUMERIC(10, 2) NOT NULL CHECK (training_amount >= 0),
    rental_amount NUMERIC(10, 2) NOT NULL CHECK (rental_amount >= 0),
    discount_amount NUMERIC(10, 2) CHECK (discount_amount IS NULL OR discount_amount >= 0),
    total_amount NUMERIC(10, 2) NOT NULL CHECK (total_amount >= 0),
    payment_status TEXT NOT NULL CHECK (payment_status IN ('unpaid', 'paid', 'refund'))
);
