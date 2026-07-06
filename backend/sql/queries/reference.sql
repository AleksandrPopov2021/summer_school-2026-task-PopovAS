-- name: GetSystemConfig :one
SELECT
    reminder_hours_before,
    visits_for_loyalty,
    violations_for_sanctions,
    booking_cutoff_minutes,
    cancellation_forbidden_minutes
FROM system_config
ORDER BY updated_at DESC
LIMIT 1;

-- name: ListRentalEquipmentTypes :many
SELECT id, code, name, default_price
FROM rental_equipment_types
ORDER BY code;
