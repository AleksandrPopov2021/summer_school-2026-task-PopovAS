-- name: ListSlotsByPeriod :many
SELECT
    s.id,
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
FROM training_slots s
JOIN training_zones z ON z.id = s.zone_id
JOIN instructors i ON i.id = s.instructor_id
JOIN gym_venues v ON v.id = s.venue_id
WHERE s.starts_at >= $1
  AND s.starts_at < $2
ORDER BY s.starts_at ASC;

-- name: GetSlotByID :one
SELECT
    s.id,
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
FROM training_slots s
JOIN training_zones z ON z.id = s.zone_id
JOIN instructors i ON i.id = s.instructor_id
JOIN gym_venues v ON v.id = s.venue_id
WHERE s.id = $1;

-- name: ListSlotRentalAvailability :many
SELECT
    sra.id,
    sra.slot_id,
    sra.equipment_type_id,
    sra.available_quantity,
    ret.code AS equipment_code,
    ret.name AS equipment_name,
    ret.default_price::text AS equipment_default_price
FROM slot_rental_availability sra
JOIN rental_equipment_types ret ON ret.id = sra.equipment_type_id
WHERE sra.slot_id = $1
ORDER BY ret.code;

-- name: FindAlternativeSlots :many
SELECT
    s.id,
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
FROM training_slots s
JOIN training_zones z ON z.id = s.zone_id
JOIN instructors i ON i.id = s.instructor_id
JOIN gym_venues v ON v.id = s.venue_id
WHERE s.zone_id = $1
  AND s.instructor_id = $2
  AND s.slot_status = 'active'
  AND s.free_spots > 0
  AND s.starts_at > $3
  AND s.id != $4
ORDER BY s.starts_at ASC
LIMIT 20;
