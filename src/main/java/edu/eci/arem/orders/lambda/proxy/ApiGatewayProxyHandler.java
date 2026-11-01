package edu.eci.arem.orders.lambda.proxy;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.eci.arem.orders.application.OrderService;
import edu.eci.arem.orders.domain.CreateOrderRequest;
import edu.eci.arem.orders.domain.InvalidOrderException;
import edu.eci.arem.orders.domain.Order;
import edu.eci.arem.orders.domain.OrderNotFoundException;
import edu.eci.arem.orders.lambda.OrderServiceProvider;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * HTTP adapter for Architecture B (Lambda proxy integration). One function serves the whole Orders API.
 *
 * <p>This is the only class that knows about API Gateway events, paths, query strings, headers and status
 * codes. It extracts the input, calls {@link OrderService} and translates results/exceptions to HTTP:
 *
 * <pre>
 *   OrderNotFoundException -> 404    InvalidOrderException / bad JSON -> 400
 *   unknown route          -> 404    anything else                    -> 500 (details only in the logs)
 * </pre>
 */
public class ApiGatewayProxyHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final OrderService service;

    public ApiGatewayProxyHandler() {
        this(OrderServiceProvider.orderService());
    }

    ApiGatewayProxyHandler(OrderService service) {
        this.service = service;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        String method = event.getHttpMethod();
        String resource = event.getResource();
        try {
            return switch (method + " " + resource) {
                case "POST /orders" -> createOrder(event);
                case "GET /orders" -> findOrdersByCustomer(event);
                case "GET /orders/{id}" -> getOrder(event);
                default -> HttpResponses.error(404, "ROUTE_NOT_FOUND", "No route for " + method + " " + event.getPath());
            };
        } catch (InvalidOrderException e) {
            return HttpResponses.error(400, e.getCode(), e.getDescription());
        } catch (OrderNotFoundException e) {
            return HttpResponses.error(404, OrderNotFoundException.CODE, e.getDescription());
        } catch (RuntimeException e) {
            log(context, "Unexpected error handling " + method + " " + event.getPath() + ": " + e);
            return HttpResponses.error(500, "INTERNAL_ERROR", "Unexpected error");
        }
    }

    private APIGatewayProxyResponseEvent createOrder(APIGatewayProxyRequestEvent event) {
        Order created = service.createOrder(readBody(event));
        return HttpResponses.json(201, created, Map.of("Location", "/orders/" + created.getId()));
    }

    private APIGatewayProxyResponseEvent getOrder(APIGatewayProxyRequestEvent event) {
        return HttpResponses.json(200, service.getOrder(pathParameter(event, "id")));
    }

    private APIGatewayProxyResponseEvent findOrdersByCustomer(APIGatewayProxyRequestEvent event) {
        return HttpResponses.json(200, service.getOrdersByCustomer(queryParameter(event, "customerId")));
    }

    private static CreateOrderRequest readBody(APIGatewayProxyRequestEvent event) {
        String body = event.getBody();
        if (body != null && Boolean.TRUE.equals(event.getIsBase64Encoded())) {
            body = new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
        }
        if (body == null || body.isBlank()) {
            throw new InvalidOrderException("request body is required");
        }
        try {
            return HttpResponses.JSON.readValue(body, CreateOrderRequest.class);
        } catch (JsonProcessingException e) {
            throw new InvalidOrderException("request body is not valid JSON or has fields of the wrong type");
        }
    }

    private static String pathParameter(APIGatewayProxyRequestEvent event, String name) {
        Map<String, String> params = event.getPathParameters();
        return params == null ? null : params.get(name);
    }

    private static String queryParameter(APIGatewayProxyRequestEvent event, String name) {
        Map<String, String> params = event.getQueryStringParameters();
        return params == null ? null : params.get(name);
    }

    private static void log(Context context, String message) {
        if (context != null && context.getLogger() != null) {
            context.getLogger().log(message);
        } else {
            System.err.println(message);
        }
    }
}
