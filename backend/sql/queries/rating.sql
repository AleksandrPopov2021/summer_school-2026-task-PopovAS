-- name: GetBookingForRating :one
SELECT
    b.id AS booking_id,
    b.client_id,
    b.booking_status,
    b.slot_id,
    s.instructor_id,
    s.starts_at,
    s.duration_minutes
FROM bookings b
JOIN training_slots s ON s.id = b.slot_id
WHERE b.id = $1
  AND b.client_id = $2;

-- name: RatingExistsForBooking :one
SELECT EXISTS (
    SELECT 1 FROM instructor_ratings WHERE booking_id = $1
) AS exists;

-- name: InsertInstructorRating :one
INSERT INTO instructor_ratings (client_id, instructor_id, booking_id, stars)
VALUES ($1, $2, $3, $4)
RETURNING
    id,
    client_id,
    instructor_id,
    booking_id,
    stars,
    rated_at;

-- name: RecalculateInstructorAverageRating :exec
UPDATE instructors
SET average_rating = (
    SELECT ROUND(AVG(stars)::numeric, 2)
    FROM instructor_ratings
    WHERE instructor_id = $1
)
WHERE id = $1;
