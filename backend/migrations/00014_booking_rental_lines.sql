-- +goose Up
CREATE TABLE booking_rental_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    equipment_type_id UUID NOT NULL REFERENCES rental_equipment_types (id),
    quantity INT NOT NULL CHECK (quantity >= 1),
    unit_price NUMERIC(10, 2) NOT NULL CHECK (unit_price >= 0)
);

CREATE INDEX idx_booking_rental_lines_booking_id ON booking_rental_lines (booking_id);

-- +goose Down
DROP TABLE IF EXISTS booking_rental_lines;
