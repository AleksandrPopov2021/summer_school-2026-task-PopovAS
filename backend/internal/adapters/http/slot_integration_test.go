package httpx_test

import (
	"context"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"

	jwtauth "github.com/vertical-climbing/backend/internal/adapters/auth"
	"github.com/vertical-climbing/backend/internal/adapters/http/api"
	"github.com/vertical-climbing/backend/internal/adapters/postgres"
)

func newTestServer(t *testing.T) *httptest.Server {
	server, _ := newIntegrationServer(t)
	return server
}

func TestListSlotsIntegration(t *testing.T) {
	server := newTestServer(t)
	defer server.Close()

	resp := doJSONRequest(t, server.Client(), http.MethodGet, server.URL+"/v1/slots", "", nil)
	if resp.StatusCode != http.StatusOK {
		t.Fatalf("list slots status: %d body=%s", resp.StatusCode, readBody(t, resp))
	}

	var payload api.TrainingSlotList
	decodeJSONResponse(t, resp, &payload)
	if len(payload.Items) == 0 {
		t.Fatalf("expected seeded slots")
	}

	hasCancelled := false
	for _, item := range payload.Items {
		if item.SlotStatus == api.SlotStatusCancelledByGym {
			hasCancelled = true
		}
		if item.Zone.Id == "" || item.Instructor.Id == "" {
			t.Fatalf("expected zone and instructor in slot summary")
		}
	}
	if !hasCancelled {
		t.Fatalf("expected cancelled_by_gym slots in schedule")
	}
}

func TestGetSlotDetailIntegration(t *testing.T) {
	server := newTestServer(t)
	defer server.Close()

	listResp := doJSONRequest(t, server.Client(), http.MethodGet, server.URL+"/v1/slots", "", nil)
	if listResp.StatusCode != http.StatusOK {
		t.Fatalf("list slots status: %d", listResp.StatusCode)
	}

	var list api.TrainingSlotList
	decodeJSONResponse(t, listResp, &list)
	if len(list.Items) == 0 {
		t.Fatalf("expected slots")
	}

	slotID := list.Items[0].Id
	detailResp := doJSONRequest(t, server.Client(), http.MethodGet, server.URL+"/v1/slots/"+slotID, "", nil)
	if detailResp.StatusCode != http.StatusOK {
		t.Fatalf("get slot status: %d body=%s", detailResp.StatusCode, readBody(t, detailResp))
	}

	var detail api.TrainingSlotDetail
	decodeJSONResponse(t, detailResp, &detail)
	if len(detail.RentalAvailability) == 0 {
		t.Fatalf("expected rental availability")
	}
}

func TestClientClearancesIntegration(t *testing.T) {
	server := newTestServer(t)
	defer server.Close()

	registerBody := map[string]string{
		"phone":      "+79008887777",
		"full_name":  "Допуск Тест",
		"birth_date": "1992-05-10",
	}
	registerResp := doJSONRequest(t, server.Client(), http.MethodPost, server.URL+"/v1/clients", "", registerBody)
	if registerResp.StatusCode != http.StatusCreated {
		t.Fatalf("register status: %d body=%s", registerResp.StatusCode, readBody(t, registerResp))
	}

	var registration api.ClientRegistrationResponse
	decodeJSONResponse(t, registerResp, &registration)

	clearancesResp := doJSONRequest(
		t,
		server.Client(),
		http.MethodGet,
		server.URL+"/v1/clients/me/clearances",
		registration.AccessToken,
		nil,
	)
	if clearancesResp.StatusCode != http.StatusOK {
		t.Fatalf("clearances status: %d body=%s", clearancesResp.StatusCode, readBody(t, clearancesResp))
	}

	var clearances api.InstructorClearanceList
	decodeJSONResponse(t, clearancesResp, &clearances)
	if clearances.Items == nil {
		t.Fatalf("expected items array")
	}

	t.Cleanup(func() {
		ctx := context.Background()
		pool, err := postgres.NewPool(ctx, os.Getenv("DATABASE_URL"))
		if err != nil {
			return
		}
		defer pool.Close()
		_, _ = pool.Exec(ctx, "DELETE FROM clients WHERE phone = $1", "+79008887777")
	})
}

func TestListSlotsWithAuthReflectsClearance(t *testing.T) {
	databaseURL := os.Getenv("DATABASE_URL")
	if databaseURL == "" {
		t.Skip("DATABASE_URL is not set")
	}

	tokenService, err := jwtauth.NewService("integration-test-secret")
	if err != nil {
		t.Fatalf("init jwt: %v", err)
	}

	server := newTestServer(t)
	defer server.Close()

	token, err := tokenService.Issue("10000000-0000-4000-8000-000000000001")
	if err != nil {
		t.Fatalf("issue token: %v", err)
	}

	slotsResp := doJSONRequest(t, server.Client(), http.MethodGet, server.URL+"/v1/slots", token, nil)
	if slotsResp.StatusCode != http.StatusOK {
		t.Fatalf("slots status: %d body=%s", slotsResp.StatusCode, readBody(t, slotsResp))
	}

	var payload api.TrainingSlotList
	decodeJSONResponse(t, slotsResp, &payload)

	foundRope := false
	for _, item := range payload.Items {
		if item.Zone.FormatType == api.FormatTypeRopeRoutes {
			foundRope = true
			if !item.Availability.ClearanceGranted {
				t.Fatalf("expected clearance granted for seeded test client on rope slot")
			}
		}
	}
	if !foundRope {
		t.Skip("no future rope slots in seed; run make seed")
	}
}
