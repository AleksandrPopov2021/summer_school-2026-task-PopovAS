package rating

import (
	"context"
	"time"
)

type Rating struct {
	ID           string
	ClientID     string
	InstructorID string
	BookingID    string
	Stars        int32
	RatedAt      time.Time
}

type Repository interface {
	Create(ctx context.Context, clientID, bookingID string, stars int32) (Rating, error)
}
