package httpx_test

import (
	"fmt"
	"net/http"
	"testing"
	"time"

	"github.com/vertical-climbing/backend/internal/adapters/http/api"
)

func TestMVPContractIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	phone := fmt.Sprintf("+7920%07d", time.Now().UnixNano()%10_000_000)
	t.Cleanup(func() { cleanupBookingClient(t, pool, phone) })

	assertStatus(t, server, http.MethodGet, server.URL+"/healthz", "", nil, http.StatusOK)
	assertStatus(t, server, http.MethodGet, server.URL+"/v1/config", "", nil, http.StatusOK)
	assertStatus(t, server, http.MethodGet, server.URL+"/v1/rental-equipment-types", "", nil, http.StatusOK)
	assertStatus(t, server, http.MethodGet, server.URL+"/v1/clients/me", "", nil, http.StatusUnauthorized)
	assertStatus(t, server, http.MethodGet, server.URL+"/v1/slots", "", nil, http.StatusOK)

	registerResp := doJSONRequest(t, server.Client(), http.MethodPost, server.URL+"/v1/clients", "", map[string]string{
		"phone":      phone,
		"full_name":  "Contract Test",
		"birth_date": "1993-06-20",
	})
	if registerResp.StatusCode != http.StatusCreated {
		t.Fatalf("register status: %d body=%s", registerResp.StatusCode, readBody(t, registerResp))
	}

	var registration api.ClientRegistrationResponse
	decodeJSONResponse(t, registerResp, &registration)
	token := registration.AccessToken

	assertStatus(t, server, http.MethodGet, server.URL+"/v1/clients/me", token, nil, http.StatusOK)
	assertStatus(t, server, http.MethodPatch, server.URL+"/v1/clients/me", token, map[string]bool{
		"risk_consent_accepted": true,
	}, http.StatusOK)
	assertStatus(t, server, http.MethodGet, server.URL+"/v1/clients/me/clearances", token, nil, http.StatusOK)
	assertStatus(t, server, http.MethodGet, server.URL+"/v1/clients/me/notification-preferences", token, nil, http.StatusOK)
	assertStatus(t, server, http.MethodPatch, server.URL+"/v1/clients/me/notification-preferences", token, map[string]bool{
		"booking_confirmation_enabled": false,
	}, http.StatusOK)
	assertStatus(t, server, http.MethodPut, server.URL+"/v1/devices/push-token", token, map[string]string{
		"token":    "contract-test-token",
		"platform": "android",
	}, http.StatusNoContent)

	listSlotsResp := doJSONRequest(t, server.Client(), http.MethodGet, server.URL+"/v1/slots", token, nil)
	if listSlotsResp.StatusCode != http.StatusOK {
		t.Fatalf("list slots status: %d", listSlotsResp.StatusCode)
	}
	var slots api.TrainingSlotList
	decodeJSONResponse(t, listSlotsResp, &slots)
	if len(slots.Items) == 0 {
		t.Fatal("expected seeded slots")
	}

	slotID := slots.Items[0].Id
	assertStatus(t, server, http.MethodGet, server.URL+"/v1/slots/"+slotID, token, nil, http.StatusOK)
	assertStatus(t, server, http.MethodGet, server.URL+"/v1/slots/"+slotID+"/rental-availability", token, nil, http.StatusOK)
	assertStatus(t, server, http.MethodGet, server.URL+"/v1/bookings", token, nil, http.StatusOK)

	bookResp := doJSONRequest(t, server.Client(), http.MethodPost, server.URL+"/v1/bookings", token, map[string]any{
		"slot_id":            findBoulderSlotWithSpots(t, server),
		"uses_own_equipment": true,
	})
	if bookResp.StatusCode != http.StatusCreated {
		t.Fatalf("create booking status: %d body=%s", bookResp.StatusCode, readBody(t, bookResp))
	}

	var booking api.BookingDetail
	decodeJSONResponse(t, bookResp, &booking)
	assertStatus(t, server, http.MethodGet, server.URL+"/v1/bookings/"+booking.Id, token, nil, http.StatusOK)
	assertStatus(t, server, http.MethodPatch, server.URL+"/v1/bookings/"+booking.Id+"/rental", token, map[string]any{
		"uses_own_equipment": true,
		"rental_lines":       []any{},
	}, http.StatusOK)

	cancelledSlotID := findCancelledGymSlot(t, server)
	assertStatus(t, server, http.MethodGet,
		server.URL+"/v1/slots/alternatives?cancelled_slot_id="+cancelledSlotID,
		token, nil, http.StatusOK)

	assertStatus(t, server, http.MethodDelete, server.URL+"/v1/bookings/"+booking.Id, token, nil, http.StatusOK)

	dupRegisterResp := doJSONRequest(t, server.Client(), http.MethodPost, server.URL+"/v1/clients", "", map[string]string{
		"phone":      phone,
		"full_name":  "Contract Test",
		"birth_date": "1993-06-20",
	})
	if dupRegisterResp.StatusCode != http.StatusConflict {
		t.Fatalf("duplicate register status: %d body=%s", dupRegisterResp.StatusCode, readBody(t, dupRegisterResp))
	}
}
