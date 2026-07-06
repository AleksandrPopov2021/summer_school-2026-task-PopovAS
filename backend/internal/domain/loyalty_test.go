package domain_test

import (
	"testing"

	"github.com/shopspring/decimal"
	"github.com/vertical-climbing/backend/internal/domain"
)

func TestBuildLoyaltyStatus_BelowThreshold(t *testing.T) {
	status := domain.BuildLoyaltyStatus(9, 10)
	if status.IsLoyalClient {
		t.Fatal("expected not loyal below threshold")
	}
	if status.LoyaltyDiscount != nil {
		t.Fatal("expected no discount below threshold")
	}
}

func TestBuildLoyaltyStatus_AtThreshold(t *testing.T) {
	status := domain.BuildLoyaltyStatus(10, 10)
	if !status.IsLoyalClient {
		t.Fatal("expected loyal client at threshold")
	}
	if status.LoyaltyDiscount == nil || !status.LoyaltyDiscount.Equal(domain.DefaultLoyaltyDiscountRate) {
		t.Fatalf("expected default loyalty discount, got %v", status.LoyaltyDiscount)
	}
}

func TestBuildLoyaltyStatus_AboveThreshold(t *testing.T) {
	status := domain.BuildLoyaltyStatus(15, 10)
	if !status.IsLoyalClient {
		t.Fatal("expected loyal client above threshold")
	}
	expected := decimal.RequireFromString("0.10")
	if status.LoyaltyDiscount == nil || !status.LoyaltyDiscount.Equal(expected) {
		t.Fatalf("unexpected discount: %v", status.LoyaltyDiscount)
	}
}
