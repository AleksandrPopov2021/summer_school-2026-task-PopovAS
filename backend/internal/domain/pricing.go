package domain

import "github.com/shopspring/decimal"

type RentalLinePrice struct {
	Quantity  int32
	UnitPrice decimal.Decimal
}

type LoyaltyInfo struct {
	IsLoyalClient bool
	DiscountRate  *decimal.Decimal
}

type PaymentCalculation struct {
	TrainingAmount decimal.Decimal
	RentalAmount   decimal.Decimal
	DiscountAmount *decimal.Decimal
	TotalAmount    decimal.Decimal
}

func CalculatePayment(
	trainingPrice decimal.Decimal,
	rentalLines []RentalLinePrice,
	loyalty LoyaltyInfo,
) PaymentCalculation {
	rentalAmount := decimal.Zero
	for _, line := range rentalLines {
		rentalAmount = rentalAmount.Add(
			line.UnitPrice.Mul(decimal.NewFromInt32(line.Quantity)),
		)
	}

	trainingAmount := trainingPrice
	total := trainingAmount.Add(rentalAmount)

	var discountAmount *decimal.Decimal
	if loyalty.IsLoyalClient && loyalty.DiscountRate != nil && loyalty.DiscountRate.GreaterThan(decimal.Zero) {
		discount := total.Mul(*loyalty.DiscountRate)
		discountAmount = &discount
		total = total.Sub(discount)
	}

	if total.IsNegative() {
		total = decimal.Zero
	}

	return PaymentCalculation{
		TrainingAmount: trainingAmount,
		RentalAmount:   rentalAmount,
		DiscountAmount: discountAmount,
		TotalAmount:    total,
	}
}
