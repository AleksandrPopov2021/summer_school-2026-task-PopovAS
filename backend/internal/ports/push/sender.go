package push

import (
	"context"

	"github.com/vertical-climbing/backend/internal/domain"
)

type DeviceToken struct {
	Token    string
	Platform string
}

type Sender interface {
	Send(ctx context.Context, clientID string, tokens []DeviceToken, payload domain.PushPayload) error
}

type Notifier interface {
	NotifyBookingCreated(ctx context.Context, clientID, bookingID, slotID string)
	NotifyGymCancellation(ctx context.Context, clientID, bookingID, slotID string)
}
