package notification

import "context"

type Preferences struct {
	ID                         string
	ClientID                   string
	BookingConfirmationEnabled bool
	RatingInvitationEnabled    bool
	RemindersEnabled           bool
	GymCancellationEnabled     bool
}

type UpdateInput struct {
	BookingConfirmationEnabled *bool
	RatingInvitationEnabled    *bool
}

type Repository interface {
	GetByClientID(ctx context.Context, clientID string) (Preferences, error)
	Update(ctx context.Context, clientID string, input UpdateInput) (Preferences, error)
}
