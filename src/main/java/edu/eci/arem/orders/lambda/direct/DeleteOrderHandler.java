package edu.eci.arem.orders.lambda.direct;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import edu.eci.arem.orders.application.OrderService;
import edu.eci.arem.orders.lambda.OrderServiceProvider;

/**
 * Lambda entry point for "delete order" (Architecture A). Input: {@code {"id": "ORD-42"}}.
 * Returns nothing on success; API Gateway turns the normal completion into 204 and the
 * {@code ORDER_NOT_FOUND:} error into 404.
 */
public class DeleteOrderHandler implements RequestHandler<OrderIdInput, Void> {

    private final OrderService service;

    public DeleteOrderHandler() {
        this(OrderServiceProvider.orderService());
    }

    DeleteOrderHandler(OrderService service) {
        this.service = service;
    }

    @Override
    public Void handleRequest(OrderIdInput input, Context context) {
        service.deleteOrder(input == null ? null : input.getId());
        return null;
    }
}
