package push

import (
	"fmt"
	"strings"

	portpush "github.com/vertical-climbing/backend/internal/ports/push"
)

func NewSender(mode string) (portpush.Sender, error) {
	switch strings.ToLower(strings.TrimSpace(mode)) {
	case "", "logging":
		return NewLoggingSender(), nil
	case "noop":
		return NewNoopSender(), nil
	case "fcm", "fcm_apns", "production":
		return nil, fmt.Errorf("FCM/APNs sender is not configured in MVP; use PUSH_SENDER=logging or noop")
	default:
		return nil, fmt.Errorf("unknown PUSH_SENDER: %s", mode)
	}
}
