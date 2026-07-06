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
