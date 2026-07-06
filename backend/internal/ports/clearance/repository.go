package clearance

import (
	"context"
	"time"
)

type Clearance struct {
	ID           string
	ClientID     string
	InstructorID *string
	IsGranted    bool
	GrantedAt    *time.Time
}

type Repository interface {
	ListByClientID(ctx context.Context, clientID string) ([]Clearance, error)
	HasGrantedClearance(ctx context.Context, clientID string) (bool, error)
}
