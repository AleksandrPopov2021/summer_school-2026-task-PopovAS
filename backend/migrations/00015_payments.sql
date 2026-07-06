-- +goose Up
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings (id) ON DELETE CASCADE,
    training_amount NUMERIC(10, 2) NOT NULL CHECK (training_amount >= 0),
    rental_amount NUMERIC(10, 2) NOT NULL CHECK (rental_amount >= 0),
    discount_amount NUMERIC(10, 2) CHECK (discount_amount IS NULL OR discount_amount >= 0),
    total_amount NUMERIC(10, 2) NOT NULL CHECK (total_amount >= 0),
    payment_status TEXT NOT NULL CHECK (payment_status IN ('unpaid', 'paid', 'refund'))
);

-- +goose Down
DROP TABLE IF EXISTS payments;
