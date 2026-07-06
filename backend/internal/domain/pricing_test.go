package domain_test

import (
	"testing"

	"github.com/shopspring/decimal"
	"github.com/vertical-climbing/backend/internal/domain"
)

func TestCalculatePayment_WithLoyaltyDiscount(t *testing.T) {
	discountRate := decimal.RequireFromString("0.10")
	result := domain.CalculatePayment(
		decimal.RequireFromString("1000.00"),
		[]domain.RentalLinePrice{
			{Quantity: 1, UnitPrice: decimal.RequireFromString("200.00")},
		},
		domain.LoyaltyInfo{
			IsLoyalClient: true,
			DiscountRate:  &discountRate,
		},
	)

	if !result.TrainingAmount.Equal(decimal.RequireFromString("1000.00")) {
		t.Fatalf("unexpected training amount: %s", result.TrainingAmount)
	}
	if !result.RentalAmount.Equal(decimal.RequireFromString("200.00")) {
		t.Fatalf("unexpected rental amount: %s", result.RentalAmount)
	}
	if result.DiscountAmount == nil || !result.DiscountAmount.Equal(decimal.RequireFromString("120.00")) {
		t.Fatalf("unexpected discount: %v", result.DiscountAmount)
	}
	if !result.TotalAmount.Equal(decimal.RequireFromString("1080.00")) {
		t.Fatalf("unexpected total: %s", result.TotalAmount)
	}
}

func TestCalculatePayment_WithoutDiscount(t *testing.T) {
	result := domain.CalculatePayment(
		decimal.RequireFromString("1500.00"),
		nil,
		domain.LoyaltyInfo{},
	)

	if !result.TotalAmount.Equal(decimal.RequireFromString("1500.00")) {
		t.Fatalf("unexpected total: %s", result.TotalAmount)
	}
	if result.DiscountAmount != nil {
		t.Fatalf("expected no discount")
	}
}
