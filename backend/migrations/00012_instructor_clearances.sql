-- +goose Up
CREATE TABLE instructor_clearances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    instructor_id UUID REFERENCES instructors (id),
    is_granted BOOLEAN NOT NULL,
    granted_at TIMESTAMPTZ
);

CREATE INDEX idx_instructor_clearances_client_id ON instructor_clearances (client_id);

-- +goose Down
DROP TABLE IF EXISTS instructor_clearances;
