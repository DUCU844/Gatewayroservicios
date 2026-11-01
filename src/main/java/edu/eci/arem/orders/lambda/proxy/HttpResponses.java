package edu.eci.arem.orders.lambda.proxy;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Builds the API Gateway proxy responses: status code, headers and JSON body. */
final class HttpResponses {

    static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private HttpResponses() {
    }

    static APIGatewayProxyResponseEvent json(int status, Object body) {
        return json(status, body, Map.of());
    }

    static APIGatewayProxyResponseEvent json(int status, Object body, Map<String, String> extraHeaders) {
        Map<String, String> headers = new HashMap<>(extraHeaders);
        headers.put("Content-Type", "application/json");
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(status)
                .withHeaders(headers)
                .withBody(write(body));
    }

    static APIGatewayProxyResponseEvent noContent() {
        return new APIGatewayProxyResponseEvent().withStatusCode(204).withHeaders(Map.of());
    }

    static APIGatewayProxyResponseEvent error(int status, String code, String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", message);
        return json(status, body);
    }

    private static String write(Object body) {
        try {
            return JSON.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize response body", e);
        }
    }
}
