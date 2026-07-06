package api

import (
	"net/http"

	"github.com/go-chi/chi/v5"
)

// ServerInterface is generated from OpenAPI via oapi-codegen (chi-server).
type ServerInterface interface {
	RegisterClient(w http.ResponseWriter, r *http.Request)
	GetCurrentClient(w http.ResponseWriter, r *http.Request)
	UpdateCurrentClient(w http.ResponseWriter, r *http.Request)
	GetClientClearances(w http.ResponseWriter, r *http.Request)
	GetNotificationPreferences(w http.ResponseWriter, r *http.Request)
	UpdateNotificationPreferences(w http.ResponseWriter, r *http.Request)
	ListSlots(w http.ResponseWriter, r *http.Request)
	FindAlternativeSlot(w http.ResponseWriter, r *http.Request)
	GetSlot(w http.ResponseWriter, r *http.Request)
	GetSlotRentalAvailability(w http.ResponseWriter, r *http.Request)
	ListBookings(w http.ResponseWriter, r *http.Request)
	CreateBooking(w http.ResponseWriter, r *http.Request)
	GetBooking(w http.ResponseWriter, r *http.Request)
	CancelBooking(w http.ResponseWriter, r *http.Request)
	UpdateBookingRental(w http.ResponseWriter, r *http.Request)
	ListRentalEquipmentTypes(w http.ResponseWriter, r *http.Request)
	GetSystemConfig(w http.ResponseWriter, r *http.Request)
	RegisterPushToken(w http.ResponseWriter, r *http.Request)
	CreateInstructorRating(w http.ResponseWriter, r *http.Request)
}

func RegisterRoutes(
	router chi.Router,
	si ServerInterface,
	auth func(http.Handler) http.Handler,
	optionalAuth func(http.Handler) http.Handler,
) {
	router.Get("/config", si.GetSystemConfig)
	router.Get("/rental-equipment-types", si.ListRentalEquipmentTypes)
	router.Post("/clients", si.RegisterClient)

	router.Group(func(r chi.Router) {
		r.Use(optionalAuth)
		r.Get("/slots", si.ListSlots)
		r.Get("/slots/{slotId}", si.GetSlot)
	})

	router.Group(func(r chi.Router) {
		r.Use(auth)
		r.Get("/clients/me", si.GetCurrentClient)
		r.Patch("/clients/me", si.UpdateCurrentClient)
		r.Get("/clients/me/clearances", si.GetClientClearances)
		r.Get("/clients/me/notification-preferences", si.GetNotificationPreferences)
		r.Patch("/clients/me/notification-preferences", si.UpdateNotificationPreferences)
		r.Get("/slots/alternatives", si.FindAlternativeSlot)
		r.Get("/slots/{slotId}/rental-availability", si.GetSlotRentalAvailability)
		r.Get("/bookings", si.ListBookings)
		r.Post("/bookings", si.CreateBooking)
		r.Get("/bookings/{bookingId}", si.GetBooking)
		r.Delete("/bookings/{bookingId}", si.CancelBooking)
		r.Patch("/bookings/{bookingId}/rental", si.UpdateBookingRental)
		r.Put("/devices/push-token", si.RegisterPushToken)
		r.Post("/ratings", si.CreateInstructorRating)
	})
}

// RegisterHandlers keeps backward compatibility with iteration 0 flat routing.
func RegisterHandlers(router chi.Router, si ServerInterface) {
	RegisterRoutes(router, si, func(next http.Handler) http.Handler { return next }, func(next http.Handler) http.Handler { return next })
}
