-- name: HasRebookingForbidden :one
SELECT EXISTS (
    SELECT 1
    FROM bookings
    WHERE client_id = $1
      AND slot_id = $2
      AND rebooking_forbidden = TRUE
) AS forbidden;

-- name: GetEquipmentTypePrice :one
SELECT id, code, name, default_price::text
FROM rental_equipment_types
WHERE id = $1;

-- name: ListBookingsByClient :many
SELECT
    b.id,
    b.client_id,
    b.slot_id,
    b.booking_status,
    b.created_at,
    b.cancelled_at,
    b.uses_own_equipment,
    b.rebooking_forbidden,
    b.cancellation_reason_id,
    cr.code AS cancellation_reason_code,
    cr.title AS cancellation_reason_title,
    cr.apology_text AS cancellation_reason_apology,
    p.id AS payment_id,
    p.training_amount::text AS payment_training_amount,
    p.rental_amount::text AS payment_rental_amount,
    p.discount_amount::text AS payment_discount_amount,
    p.total_amount::text AS payment_total_amount,
    p.payment_status,
    s.starts_at,
    s.duration_minutes,
    s.capacity,
    s.free_spots,
    s.training_price::text AS training_price,
    s.rental_tariff::text AS rental_tariff,
    s.slot_status,
    s.address,
    z.id AS zone_id,
    z.name AS zone_name,
    z.format_type AS zone_format_type,
    z.difficulty AS zone_difficulty,
    z.max_group_size AS zone_max_group_size,
    i.id AS instructor_id,
    i.full_name AS instructor_full_name,
    i.average_rating::text AS instructor_average_rating,
    v.id AS venue_id,
    v.name AS venue_name,
    v.address AS venue_address
FROM bookings b
JOIN training_slots s ON s.id = b.slot_id
JOIN training_zones z ON z.id = s.zone_id
JOIN instructors i ON i.id = s.instructor_id
JOIN gym_venues v ON v.id = s.venue_id
JOIN payments p ON p.booking_id = b.id
LEFT JOIN cancellation_reasons cr ON cr.id = b.cancellation_reason_id
WHERE b.client_id = $1
  AND b.booking_status = $2
ORDER BY s.starts_at ASC;

-- name: GetBookingByIDForClient :one
SELECT
    b.id,
    b.client_id,
    b.slot_id,
    b.booking_status,
    b.created_at,
    b.cancelled_at,
    b.uses_own_equipment,
    b.rebooking_forbidden,
    b.cancellation_reason_id,
    cr.code AS cancellation_reason_code,
    cr.title AS cancellation_reason_title,
    cr.apology_text AS cancellation_reason_apology,
    p.id AS payment_id,
    p.training_amount::text AS payment_training_amount,
    p.rental_amount::text AS payment_rental_amount,
    p.discount_amount::text AS payment_discount_amount,
    p.total_amount::text AS payment_total_amount,
    p.payment_status,
    s.starts_at,
    s.duration_minutes,
    s.capacity,
    s.free_spots,
    s.training_price::text AS training_price,
    s.rental_tariff::text AS rental_tariff,
    s.slot_status,
    s.address,
    z.id AS zone_id,
    z.name AS zone_name,
    z.format_type AS zone_format_type,
    z.difficulty AS zone_difficulty,
    z.max_group_size AS zone_max_group_size,
    i.id AS instructor_id,
    i.full_name AS instructor_full_name,
    i.average_rating::text AS instructor_average_rating,
    v.id AS venue_id,
    v.name AS venue_name,
    v.address AS venue_address
FROM bookings b
JOIN training_slots s ON s.id = b.slot_id
JOIN training_zones z ON z.id = s.zone_id
JOIN instructors i ON i.id = s.instructor_id
JOIN gym_venues v ON v.id = s.venue_id
JOIN payments p ON p.booking_id = b.id
LEFT JOIN cancellation_reasons cr ON cr.id = b.cancellation_reason_id
WHERE b.id = $1
  AND b.client_id = $2;

-- name: ListBookingRentalLinesByBookingID :many
SELECT
    brl.id,
    brl.booking_id,
    brl.equipment_type_id,
    brl.quantity,
    brl.unit_price::text AS unit_price,
    ret.code AS equipment_code,
    ret.name AS equipment_name
FROM booking_rental_lines brl
JOIN rental_equipment_types ret ON ret.id = brl.equipment_type_id
WHERE brl.booking_id = $1
ORDER BY ret.code;

-- name: ListActiveBookingsBySlotID :many
SELECT
    b.id,
    b.client_id,
    p.payment_status
FROM bookings b
JOIN payments p ON p.booking_id = b.id
WHERE b.slot_id = $1
  AND b.booking_status = 'booked';

-- name: CancellationReasonExists :one
SELECT EXISTS (
    SELECT 1 FROM cancellation_reasons WHERE id = $1
) AS exists;
