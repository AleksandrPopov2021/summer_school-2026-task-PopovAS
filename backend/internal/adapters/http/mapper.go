package httpx

import (
	"github.com/vertical-climbing/backend/internal/adapters/http/api"
	"github.com/vertical-climbing/backend/internal/domain"
	portclearance "github.com/vertical-climbing/backend/internal/ports/clearance"
	portbooking "github.com/vertical-climbing/backend/internal/ports/booking"
	portclient "github.com/vertical-climbing/backend/internal/ports/client"
	portnotification "github.com/vertical-climbing/backend/internal/ports/notification"
	portrating "github.com/vertical-climbing/backend/internal/ports/rating"
	portref "github.com/vertical-climbing/backend/internal/ports/reference"
	portslot "github.com/vertical-climbing/backend/internal/ports/slot"
)

func ToAPISystemConfig(cfg portref.SystemConfig) api.SystemConfig {
	return api.SystemConfig{
		ReminderHoursBefore:          int(cfg.ReminderHoursBefore),
		VisitsForLoyalty:             int(cfg.VisitsForLoyalty),
		ViolationsForSanctions:       int(cfg.ViolationsForSanctions),
		BookingCutoffMinutes:         int(cfg.BookingCutoffMinutes),
		CancellationForbiddenMinutes: int(cfg.CancellationForbiddenMinutes),
	}
}

func ToAPIRentalEquipmentType(item portref.RentalEquipmentType) api.RentalEquipmentType {
	return api.RentalEquipmentType{
		Id:           item.ID,
		Code:         api.RentalEquipmentCode(item.Code),
		Name:         item.Name,
		DefaultPrice: item.DefaultPrice,
	}
}

func ToAPIClient(client portclient.Client) api.Client {
	apiClient := api.Client{
		Id:                    client.ID,
		FullName:              client.FullName,
		Phone:                 client.Phone,
		BirthDate:             client.BirthDate.Format("2006-01-02"),
		RiskConsentAccepted:   client.RiskConsentAccepted,
		CompletedVisitsCount:  int(client.CompletedVisitsCount),
		IsLoyalClient:         client.IsLoyalClient,
		LoyaltyDiscount:       client.LoyaltyDiscount,
		LateCancellationCount: int(client.LateCancellationCount),
		NoShowCount:           int(client.NoShowCount),
	}
	return apiClient
}

func ToAPINotificationPreferences(prefs portnotification.Preferences) api.NotificationPreferences {
	return api.NotificationPreferences{
		Id:                         prefs.ID,
		ClientId:                   prefs.ClientID,
		BookingConfirmationEnabled: prefs.BookingConfirmationEnabled,
		RatingInvitationEnabled:    prefs.RatingInvitationEnabled,
		RemindersEnabled:           prefs.RemindersEnabled,
		GymCancellationEnabled:     prefs.GymCancellationEnabled,
	}
}

func ToAPIBookingAvailability(availability domain.BookingAvailability) api.BookingAvailability {
	freeSpots := int(availability.FreeSpots)
	return api.BookingAvailability{
		CanBook:             availability.CanBook,
		HasFreeSpots:        availability.HasFreeSpots,
		FreeSpots:           &freeSpots,
		WithinBookingWindow: availability.WithinBookingWindow,
		ClearanceRequired:   availability.ClearanceRequired,
		ClearanceGranted:    availability.ClearanceGranted,
	}
}

func ToAPITrainingSlotSummary(slot portslot.Slot, availability domain.BookingAvailability) api.TrainingSlotSummary {
	summary := api.TrainingSlotSummary{
		Id:              slot.ID,
		StartsAt:        slot.StartsAt,
		DurationMinutes: int(slot.DurationMinutes),
		Capacity:        int(slot.Capacity),
		FreeSpots:       int(slot.FreeSpots),
		TrainingPrice:   slot.TrainingPrice,
		RentalTariff:    slot.RentalTariff,
		SlotStatus:      api.SlotStatus(slot.SlotStatus),
		Address:         slot.Address,
		Zone: api.TrainingZone{
			Id:           slot.Zone.ID,
			Name:         slot.Zone.Name,
			FormatType:   api.FormatType(slot.Zone.FormatType),
			Difficulty:   api.Difficulty(slot.Zone.Difficulty),
			MaxGroupSize: int(slot.Zone.MaxGroupSize),
		},
		Instructor: api.Instructor{
			Id:            slot.Instructor.ID,
			FullName:      slot.Instructor.FullName,
			AverageRating: slot.Instructor.AverageRating,
		},
		Venue: &api.GymVenue{
			Id:      slot.Venue.ID,
			Name:    slot.Venue.Name,
			Address: slot.Venue.Address,
		},
		Availability: ToAPIBookingAvailability(availability),
	}
	return summary
}

func ToAPITrainingSlotDetail(detail portslot.SlotDetail, availability domain.BookingAvailability) api.TrainingSlotDetail {
	result := api.TrainingSlotDetail{
		TrainingSlotSummary: ToAPITrainingSlotSummary(detail.Slot, availability),
		RentalAvailability:  make([]api.SlotRentalAvailability, 0, len(detail.RentalAvailability)),
	}
	for _, item := range detail.RentalAvailability {
		result.RentalAvailability = append(result.RentalAvailability, api.SlotRentalAvailability{
			Id:                item.ID,
			SlotId:            item.SlotID,
			EquipmentTypeId:   item.EquipmentTypeID,
			AvailableQuantity: int(item.AvailableQuantity),
			EquipmentType: api.RentalEquipmentType{
				Id:           item.EquipmentTypeID,
				Code:         api.RentalEquipmentCode(item.EquipmentCode),
				Name:         item.EquipmentName,
				DefaultPrice: item.EquipmentPrice,
			},
		})
	}
	return result
}

func ToAPIInstructorClearance(item portclearance.Clearance) api.InstructorClearance {
	return api.InstructorClearance{
		Id:           item.ID,
		ClientId:     item.ClientID,
		InstructorId: item.InstructorID,
		IsGranted:    item.IsGranted,
		GrantedAt:    item.GrantedAt,
	}
}

func ToAPICancellationPolicy(policy domain.CancellationPolicy) api.CancellationPolicy {
	return api.CancellationPolicy{
		CanCancel:         policy.CanCancel,
		MinutesUntilStart: policy.MinutesUntilStart,
		WarningLevel:      api.CancellationPolicyWarningLevel(policy.WarningLevel),
	}
}

func ToAPIBookingSummary(
	booking portbooking.Booking,
	availability domain.BookingAvailability,
	policy domain.CancellationPolicy,
) api.BookingSummary {
	policyAPI := ToAPICancellationPolicy(policy)
	return api.BookingSummary{
		Id:                 booking.ID,
		SlotId:             booking.SlotID,
		BookingStatus:      api.BookingStatus(booking.BookingStatus),
		CreatedAt:          booking.CreatedAt,
		CancelledAt:        booking.CancelledAt,
		UsesOwnEquipment:   booking.UsesOwnEquipment,
		RebookingForbidden: booking.RebookingForbidden,
		Slot:               ToAPITrainingSlotSummary(booking.Slot, availability),
		Payment: api.PaymentInfo{
			Id:             booking.Payment.ID,
			BookingId:      booking.Payment.BookingID,
			TrainingAmount: booking.Payment.TrainingAmount,
			RentalAmount:   booking.Payment.RentalAmount,
			DiscountAmount: booking.Payment.DiscountAmount,
			TotalAmount:    booking.Payment.TotalAmount,
			PaymentStatus:  api.PaymentStatus(booking.Payment.PaymentStatus),
		},
		CancellationPolicy: &policyAPI,
	}
}

func ToAPIBookingDetail(
	booking portbooking.Booking,
	availability domain.BookingAvailability,
	policy domain.CancellationPolicy,
) api.BookingDetail {
	rentalLines := make([]api.BookingRentalLine, 0, len(booking.RentalLines))
	for _, line := range booking.RentalLines {
		rentalLines = append(rentalLines, api.BookingRentalLine{
			Id:              line.ID,
			BookingId:       line.BookingID,
			EquipmentTypeId: line.EquipmentTypeID,
			Quantity:        int(line.Quantity),
			UnitPrice:       line.UnitPrice,
			EquipmentType: api.RentalEquipmentType{
				Id:           line.EquipmentTypeID,
				Code:         api.RentalEquipmentCode(line.EquipmentCode),
				Name:         line.EquipmentName,
				DefaultPrice: line.UnitPrice,
			},
		})
	}

	detail := api.BookingDetail{
		BookingSummary: ToAPIBookingSummary(booking, availability, policy),
		RentalLines:    rentalLines,
	}
	if booking.CancellationReason != nil {
		detail.CancellationReason = &api.CancellationReason{
			Id:          booking.CancellationReason.ID,
			Code:        booking.CancellationReason.Code,
			Title:       booking.CancellationReason.Title,
			ApologyText: booking.CancellationReason.ApologyText,
		}
	}
	return detail
}

func ToAPIAlternativeSlotResponse(slot portslot.Slot, availability domain.BookingAvailability) api.AlternativeSlotResponse {
	summary := ToAPITrainingSlotSummary(slot, availability)
	return api.AlternativeSlotResponse{
		Found:           true,
		AlternativeSlot: &summary,
	}
}

func ToAPISlotRentalAvailabilityList(slotID string, items []portslot.RentalAvailability) api.SlotRentalAvailabilityList {
	result := api.SlotRentalAvailabilityList{
		SlotId: slotID,
		Items:  make([]api.SlotRentalAvailability, 0, len(items)),
	}
	for _, item := range items {
		result.Items = append(result.Items, api.SlotRentalAvailability{
			Id:                item.ID,
			SlotId:            item.SlotID,
			EquipmentTypeId:   item.EquipmentTypeID,
			AvailableQuantity: int(item.AvailableQuantity),
			EquipmentType: api.RentalEquipmentType{
				Id:           item.EquipmentTypeID,
				Code:         api.RentalEquipmentCode(item.EquipmentCode),
				Name:         item.EquipmentName,
				DefaultPrice: item.EquipmentPrice,
			},
		})
	}
	return result
}

func ToAPIInstructorRating(rating portrating.Rating) api.InstructorRating {
	return api.InstructorRating{
		Id:           rating.ID,
		ClientId:     rating.ClientID,
		InstructorId: rating.InstructorID,
		BookingId:    rating.BookingID,
		Stars:        int(rating.Stars),
		RatedAt:      rating.RatedAt,
	}
}
