package edu.eci.arem.orders.lambda.proxy;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import edu.eci.arem.orders.application.InMemoryOrderRepository;
import edu.eci.arem.orders.application.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Exercises the HTTP adapter locally with hand-built API Gateway proxy events: no AWS needed. */
class ApiGatewayProxyHandlerTest {

    private ApiGatewayProxyHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ApiGatewayProxyHandler(new OrderService(InMemoryOrderRepository.withSampleData()));
    }

    @Test
    void postCreatesOrderWith201AndLocation() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("POST", "/orders", "/orders").withBody("{\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":1}"),
                null);

        assertEquals(201, response.getStatusCode());
        assertEquals("/orders/ORD-43", response.getHeaders().get("Location"));
        assertEquals("ORD-43", json(response).get("id").asText());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
    }

    @Test
    void postWithInvalidQuantityIs400() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("POST", "/orders", "/orders").withBody("{\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":0}"),
                null);

        assertEquals(400, response.getStatusCode());
        assertEquals("INVALID_ORDER", json(response).get("error").asText());
        assertEquals("quantity must be at least 1", json(response).get("message").asText());
    }

    @Test
    void postWithMalformedJsonIs400() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("POST", "/orders", "/orders").withBody("{not json"), null);

        assertEquals(400, response.getStatusCode());
        assertEquals("INVALID_ORDER", json(response).get("error").asText());
    }

    @Test
    void postWithoutBodyIs400() {
        assertEquals(400, handler.handleRequest(event("POST", "/orders", "/orders"), null).getStatusCode());
    }

    @Test
    void postAcceptsBase64EncodedBody() {
        String body = java.util.Base64.getEncoder()
                .encodeToString("{\"customerId\":\"CUST-1\",\"product\":\"Pen\",\"quantity\":5}".getBytes());
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("POST", "/orders", "/orders").withBody(body).withIsBase64Encoded(true), null);

        assertEquals(201, response.getStatusCode());
    }

    @Test
    void getExistingOrderIs200() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("GET", "/orders/{id}", "/orders/ORD-42").withPathParameters(Map.of("id", "ORD-42")), null);

        assertEquals(200, response.getStatusCode());
        JsonNode body = json(response);
        assertEquals("ORD-42", body.get("id").asText());
        assertEquals("CUST-7", body.get("customerId").asText());
        assertEquals("Keyboard", body.get("product").asText());
        assertEquals(2, body.get("quantity").asInt());
    }

    @Test
    void getMissingOrderIs404WithErrorBody() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("GET", "/orders/{id}", "/orders/ORD-999").withPathParameters(Map.of("id", "ORD-999")), null);

        assertEquals(404, response.getStatusCode());
        assertEquals("ORDER_NOT_FOUND", json(response).get("error").asText());
        assertEquals("Order ORD-999 does not exist", json(response).get("message").asText());
    }

    @Test
    void searchByCustomerIs200WithArray() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("GET", "/orders", "/orders").withQueryStringParameters(Map.of("customerId", "CUST-42")), null);

        assertEquals(200, response.getStatusCode());
        assertEquals(2, json(response).size());
    }

    @Test
    void searchForCustomerWithoutOrdersIsEmptyArray() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("GET", "/orders", "/orders").withQueryStringParameters(Map.of("customerId", "CUST-100")), null);

        assertEquals(200, response.getStatusCode());
        assertEquals("[]", response.getBody());
    }

    @Test
    void searchWithoutCustomerIdIs400() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(event("GET", "/orders", "/orders"), null);

        assertEquals(400, response.getStatusCode());
        assertEquals("MISSING_CUSTOMER_ID", json(response).get("error").asText());
    }

    @Test
    void deleteExistingOrderIs204WithEmptyBody() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("DELETE", "/orders/{id}", "/orders/ORD-42").withPathParameters(Map.of("id", "ORD-42")), null);

        assertEquals(204, response.getStatusCode());
        assertNull(response.getBody());
        assertEquals(404, handler.handleRequest(
                event("GET", "/orders/{id}", "/orders/ORD-42").withPathParameters(Map.of("id", "ORD-42")), null)
                .getStatusCode());
    }

    @Test
    void deleteMissingOrderIs404() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                event("DELETE", "/orders/{id}", "/orders/ORD-999").withPathParameters(Map.of("id", "ORD-999")), null);

        assertEquals(404, response.getStatusCode());
        assertEquals("ORDER_NOT_FOUND", json(response).get("error").asText());
    }

    @Test
    void unknownRouteIs404() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(event("PATCH", "/orders/{id}", "/orders/ORD-1"), null);

        assertEquals(404, response.getStatusCode());
        assertEquals("ROUTE_NOT_FOUND", json(response).get("error").asText());
    }

    @Test
    void unexpectedFailureIs500WithoutInternalDetails() throws Exception {
        OrderService broken = new OrderService(new InMemoryOrderRepository(1) {
            @Override
            public java.util.Optional<edu.eci.arem.orders.domain.Order> findById(String id) {
                throw new IllegalStateException("storage unavailable");
            }
        });
        APIGatewayProxyResponseEvent response = new ApiGatewayProxyHandler(broken).handleRequest(
                event("GET", "/orders/{id}", "/orders/ORD-1").withPathParameters(Map.of("id", "ORD-1")), null);

        assertEquals(500, response.getStatusCode());
        assertEquals("INTERNAL_ERROR", json(response).get("error").asText());
        assertFalse(response.getBody().contains("storage unavailable"));
    }

    private static APIGatewayProxyRequestEvent event(String method, String resource, String path) {
        return new APIGatewayProxyRequestEvent().withHttpMethod(method).withResource(resource).withPath(path);
    }

    private static JsonNode json(APIGatewayProxyResponseEvent response) throws Exception {
        return HttpResponses.JSON.readTree(response.getBody());
    }
}
