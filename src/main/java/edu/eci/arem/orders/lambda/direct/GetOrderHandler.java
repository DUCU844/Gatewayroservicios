package edu.eci.arem.orders.lambda.direct;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import edu.eci.arem.orders.application.OrderService;
import edu.eci.arem.orders.domain.Order;
import edu.eci.arem.orders.lambda.OrderServiceProvider;

/** Lambda entry point for "get order by id" (Architecture A). Input: {@code {"id": "ORD-42"}}. */
public class GetOrderHandler implements RequestHandler<OrderIdInput, Order> {

    private final OrderService service;

    public GetOrderHandler() {
        this(OrderServiceProvider.orderService());
    }

    GetOrderHandler(OrderService service) {
        this.service = service;
    }

    @Override
    public Order handleRequest(OrderIdInput input, Context context) {
        return service.getOrder(input == null ? null : input.getId());
    }
}
