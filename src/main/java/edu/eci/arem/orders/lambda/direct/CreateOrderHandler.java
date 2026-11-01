package edu.eci.arem.orders.lambda.direct;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import edu.eci.arem.orders.application.OrderService;
import edu.eci.arem.orders.domain.CreateOrderRequest;
import edu.eci.arem.orders.domain.Order;
import edu.eci.arem.orders.lambda.OrderServiceProvider;

/**
 * Lambda entry point for "create order" (Architecture A — non-proxy integration).
 *
 * <p>Handler string: {@code edu.eci.arem.orders.lambda.direct.CreateOrderHandler::handleRequest}.
 * The Lambda runtime deserializes the event JSON into {@link CreateOrderRequest} and serializes the returned
 * {@link Order} back to JSON. Domain exceptions propagate and become a Lambda error
 * ({@code errorMessage}/{@code errorType}) that API Gateway maps to a status code.
 */
public class CreateOrderHandler implements RequestHandler<CreateOrderRequest, Order> {

    private final OrderService service;

    public CreateOrderHandler() {
        this(OrderServiceProvider.orderService());
    }

    CreateOrderHandler(OrderService service) {
        this.service = service;
    }

    @Override
    public Order handleRequest(CreateOrderRequest input, Context context) {
        return service.createOrder(input);
    }
}
