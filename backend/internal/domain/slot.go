package domain

import "time"

const (
	SlotStatusActive          = "active"
	SlotStatusCancelledByGym  = "cancelled_by_gym"
	FormatTypeBouldering      = "bouldering_instruction"
	FormatTypeRopeRoutes      = "rope_routes"
)

type BookingAvailability struct {
	CanBook              bool
	HasFreeSpots         bool
	FreeSpots            int32
	WithinBookingWindow  bool
	ClearanceRequired    bool
	ClearanceGranted     bool
}

type AvailabilityInput struct {
	SlotStatus             string
	FormatType             string
	FreeSpots              int32
	StartsAt               time.Time
	BookingCutoffMinutes   int32
	HasClearance           bool
	HasAuthenticatedClient bool
	Now                    time.Time
}

func BuildBookingAvailability(input AvailabilityInput) BookingAvailability {
	hasFreeSpots := input.FreeSpots > 0
	minutesUntilStart := input.StartsAt.Sub(input.Now).Minutes()
	withinWindow := minutesUntilStart >= float64(input.BookingCutoffMinutes)
	clearanceRequired := input.FormatType == FormatTypeRopeRoutes
	clearanceGranted := !clearanceRequired || (input.HasAuthenticatedClient && input.HasClearance)

	canBook := input.SlotStatus == SlotStatusActive &&
		hasFreeSpots &&
		withinWindow &&
		clearanceGranted

	return BookingAvailability{
		CanBook:             canBook,
		HasFreeSpots:        hasFreeSpots,
		FreeSpots:           input.FreeSpots,
		WithinBookingWindow: withinWindow,
		ClearanceRequired:   clearanceRequired,
		ClearanceGranted:    clearanceGranted,
	}
}

func ParseSlotPeriod(fromValue, toValue, dateValue string, now time.Time) (time.Time, time.Time, error) {
	if dateValue != "" {
		day, err := time.Parse("2006-01-02", dateValue)
		if err != nil {
			return time.Time{}, time.Time{}, NewBadRequest("Некорректный параметр date")
		}
		return startOfDay(day), startOfDay(day).AddDate(0, 0, 1), nil
	}

	from := startOfDay(now)
	if fromValue != "" {
		parsed, err := time.Parse("2006-01-02", fromValue)
		if err != nil {
			return time.Time{}, time.Time{}, NewBadRequest("Некорректный параметр from")
		}
		from = startOfDay(parsed)
	}

	to := startOfDay(from).AddDate(0, 0, 8)
	if toValue != "" {
		parsed, err := time.Parse("2006-01-02", toValue)
		if err != nil {
			return time.Time{}, time.Time{}, NewBadRequest("Некорректный параметр to")
		}
		to = startOfDay(parsed).AddDate(0, 0, 1)
	}

	if !to.After(from) {
		return time.Time{}, time.Time{}, NewBadRequest("Параметр to должен быть позже from")
	}

	return from, to, nil
}

func startOfDay(value time.Time) time.Time {
	year, month, day := value.Date()
	return time.Date(year, month, day, 0, 0, 0, 0, value.Location())
}
