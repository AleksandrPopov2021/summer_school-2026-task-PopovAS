package httpx_test

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/vertical-climbing/backend/internal/adapters/http/api"
)

func assertStatus(t *testing.T, server *httptest.Server, method, url, token string, body any, want int) {
	t.Helper()
	resp := doJSONRequest(t, server.Client(), method, url, token, body)
	if resp.StatusCode != want {
		t.Fatalf("%s %s: expected %d, got %d body=%s", method, url, want, resp.StatusCode, readBody(t, resp))
	}
}

func findCancelledGymSlot(t *testing.T, server *httptest.Server) string {
	t.Helper()
	resp := doJSONRequest(t, server.Client(), http.MethodGet, server.URL+"/v1/slots", "", nil)
	if resp.StatusCode != http.StatusOK {
		t.Fatalf("list slots status: %d", resp.StatusCode)
	}
	var payload api.TrainingSlotList
	decodeJSONResponse(t, resp, &payload)
	for _, item := range payload.Items {
		if item.SlotStatus == api.SlotStatusCancelledByGym {
			return item.Id
		}
	}
	t.Fatalf("expected cancelled_by_gym slot in seed")
	return ""
}

// TestMVPContractPathsRegistered documents the 18 OpenAPI MVP endpoints (+ health).
func TestMVPContractPathsRegistered(t *testing.T) {
	paths := []string{
		"GET /healthz",
		"GET /v1/config",
		"GET /v1/rental-equipment-types",
		"POST /v1/clients",
		"GET /v1/clients/me",
		"PATCH /v1/clients/me",
		"GET /v1/clients/me/clearances",
		"GET /v1/clients/me/notification-preferences",
		"PATCH /v1/clients/me/notification-preferences",
		"GET /v1/slots",
		"GET /v1/slots/{slotId}",
		"GET /v1/slots/alternatives",
		"GET /v1/slots/{slotId}/rental-availability",
		"GET /v1/bookings",
		"POST /v1/bookings",
		"GET /v1/bookings/{bookingId}",
		"DELETE /v1/bookings/{bookingId}",
		"PATCH /v1/bookings/{bookingId}/rental",
		"PUT /v1/devices/push-token",
		"POST /v1/ratings",
	}
	if len(paths) != 20 {
		t.Fatalf("expected 20 routes incl. health and ratings, got %d", len(paths))
	}
}
