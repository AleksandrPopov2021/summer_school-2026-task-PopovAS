package httpx

import (
	"net/http"

	"github.com/vertical-climbing/backend/internal/adapters/http/api"
	appbooking "github.com/vertical-climbing/backend/internal/application/booking"
	appclearance "github.com/vertical-climbing/backend/internal/application/clearance"
	appclient "github.com/vertical-climbing/backend/internal/application/client"
	appdevice "github.com/vertical-climbing/backend/internal/application/device"
	appnotification "github.com/vertical-climbing/backend/internal/application/notification"
	apprating "github.com/vertical-climbing/backend/internal/application/rating"
	appref "github.com/vertical-climbing/backend/internal/application/reference"
	appslot "github.com/vertical-climbing/backend/internal/application/slot"
)

type Handler struct {
	reference    *appref.Service
	client       *appclient.Service
	notification *appnotification.Service
	devices      *appdevice.Service
	slots        *appslot.Service
	clearances   *appclearance.Service
	bookings     *appbooking.Service
	ratings      *apprating.Service
}

func NewHandler(
	reference *appref.Service,
	client *appclient.Service,
	notification *appnotification.Service,
	devices *appdevice.Service,
	slots *appslot.Service,
	clearances *appclearance.Service,
	bookings *appbooking.Service,
	ratings *apprating.Service,
) *Handler {
	return &Handler{
		reference:    reference,
		client:       client,
		notification: notification,
		devices:      devices,
		slots:        slots,
		clearances:   clearances,
		bookings:     bookings,
		ratings:      ratings,
	}
}

func (h *Handler) GetSystemConfig(w http.ResponseWriter, r *http.Request) {
	cfg, err := h.reference.GetSystemConfig(r.Context())
	if err != nil {
		WriteError(w, err)
		return
	}

	WriteJSON(w, http.StatusOK, ToAPISystemConfig(cfg))
}

func (h *Handler) ListRentalEquipmentTypes(w http.ResponseWriter, r *http.Request) {
	items, err := h.reference.ListRentalEquipmentTypes(r.Context())
	if err != nil {
		WriteError(w, err)
		return
	}

	response := api.RentalEquipmentTypeList{
		Items: make([]api.RentalEquipmentType, 0, len(items)),
	}
	for _, item := range items {
		response.Items = append(response.Items, ToAPIRentalEquipmentType(item))
	}

	WriteJSON(w, http.StatusOK, response)
}

var _ api.ServerInterface = (*Handler)(nil)
