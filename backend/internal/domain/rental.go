package domain

// RentalStockCovers checks whether slot rental availability satisfies required quantities.
func RentalStockCovers(availability map[string]int32, required map[string]int32) bool {
	if len(required) == 0 {
		return true
	}
	for equipmentID, quantity := range required {
		if availability[equipmentID] < quantity {
			return false
		}
	}
	return true
}
