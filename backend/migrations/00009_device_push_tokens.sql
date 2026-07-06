-- +goose Up
CREATE TABLE device_push_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    token TEXT NOT NULL,
    platform TEXT NOT NULL CHECK (platform IN ('ios', 'android')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (client_id, platform)
);

CREATE INDEX idx_device_push_tokens_client_id ON device_push_tokens (client_id);

-- +goose Down
DROP TABLE IF EXISTS device_push_tokens;
