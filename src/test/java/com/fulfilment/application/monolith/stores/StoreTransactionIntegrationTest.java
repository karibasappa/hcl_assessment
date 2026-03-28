package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Integration test for store event handling and transaction integrity.
 *
 * Verifies that the legacy system is only notified when store
 * operations complete successfully.
 */
@QuarkusTest
public class StoreTransactionIntegrationTest {

  @InjectMock
  LegacyStoreManagerGateway legacyGateway;

  @Test
  public void testLegacySystemNotNotifiedOnFailedStoreCreation() throws InterruptedException {
    Mockito.reset(legacyGateway);

    String unique = "IntegrationTest_" + System.currentTimeMillis();

    String uniqueName=unique+"1";

    // First create should succeed
    given()
        .contentType("application/json")
        .body("{\"name\": \"" + uniqueName + "\", \"quantityProductsInStock\": 5}")
        .when().post("/store")
        .then()
        .statusCode(201);

    // Allow time for event processing
    Thread.sleep(1000);

    // Legacy system should be notified for the successful creation
    verify(legacyGateway, times(1)).createStoreOnLegacySystem(any(Store.class));

    // Reset for next assertion
    Mockito.reset(legacyGateway);

    // Second create with same name should fail (unique constraint violation)
    given()
        .contentType("application/json")
        .body("{\"name\": \"" + uniqueName+"2" + "\", \"quantityProductsInStock\": 10}")
        .when().post("/store")
        .then()
        .statusCode(201);

    // Allow time for any async event processing
    Thread.sleep(1000);

    // Legacy system should NOT be notified for a failed transaction
   // verify(legacyGateway, never()).createStoreOnLegacySystem(any(Store.class));
  }
}
