package client

import (
	"context"
	"time"

	"github.com/shopspring/decimal"
)

type Client struct {
	ID                     string
	FullName               string
	Phone                  string
	BirthDate              time.Time
	RiskConsentAccepted    bool
	CompletedVisitsCount   int32
	IsLoyalClient          bool
	LoyaltyDiscount        *decimal.Decimal
	LateCancellationCount  int32
	NoShowCount            int32
}

type RegisterInput struct {
	Phone     string
	FullName  string
	BirthDate time.Time
}

type RegisterResult struct {
	AccessToken string
	Client      Client
}

type Repository interface {
	ExistsByPhone(ctx context.Context, phone string) (bool, error)
	Register(ctx context.Context, input RegisterInput) (Client, error)
	GetByID(ctx context.Context, id string) (Client, error)
	UpdateRiskConsent(ctx context.Context, id string) (Client, error)
	IncrementLateCancellationCount(ctx context.Context, id string) error
}
