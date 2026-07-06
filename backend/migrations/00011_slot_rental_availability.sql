-- +goose Up
CREATE TABLE slot_rental_availability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slot_id UUID NOT NULL REFERENCES training_slots (id) ON DELETE CASCADE,
    equipment_type_id UUID NOT NULL REFERENCES rental_equipment_types (id),
    available_quantity INT NOT NULL CHECK (available_quantity >= 0),
    UNIQUE (slot_id, equipment_type_id)
);

CREATE INDEX idx_slot_rental_availability_slot_id ON slot_rental_availability (slot_id);

-- +goose Down
DROP TABLE IF EXISTS slot_rental_availability;
