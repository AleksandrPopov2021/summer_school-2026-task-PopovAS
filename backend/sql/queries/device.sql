-- name: UpsertDevicePushToken :exec
INSERT INTO device_push_tokens (client_id, token, platform, updated_at)
VALUES ($1, $2, $3, NOW())
ON CONFLICT (client_id, platform) DO UPDATE
SET token = EXCLUDED.token,
    updated_at = NOW();

-- name: ListDevicePushTokensByClientID :many
SELECT token, platform
FROM device_push_tokens
WHERE client_id = $1;
