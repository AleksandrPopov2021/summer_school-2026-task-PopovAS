-- name: ListClearancesByClientID :many
SELECT
    id,
    client_id,
    instructor_id,
    is_granted,
    granted_at
FROM instructor_clearances
WHERE client_id = $1
ORDER BY granted_at DESC NULLS LAST;

-- name: ClientHasGrantedClearance :one
SELECT EXISTS (
    SELECT 1
    FROM instructor_clearances
    WHERE client_id = $1
      AND is_granted = TRUE
) AS has_clearance;
