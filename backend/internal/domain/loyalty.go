package domain

import "github.com/shopspring/decimal"

var DefaultLoyaltyDiscountRate = decimal.RequireFromString("0.10")

type LoyaltyStatus struct {
	IsLoyalClient   bool
	LoyaltyDiscount *decimal.Decimal
}

func BuildLoyaltyStatus(completedVisits, visitsForLoyalty int32) LoyaltyStatus {
	if completedVisits < visitsForLoyalty {
		return LoyaltyStatus{}
	}

	discount := DefaultLoyaltyDiscountRate
	return LoyaltyStatus{
		IsLoyalClient:   true,
		LoyaltyDiscount: &discount,
	}
}
