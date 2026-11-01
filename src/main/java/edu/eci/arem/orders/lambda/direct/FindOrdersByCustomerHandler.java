package edu.eci.arem.orders.lambda.direct;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import edu.eci.arem.orders.application.OrderService;
import edu.eci.arem.orders.domain.Order;
import edu.eci.arem.orders.lambda.OrderServiceProvider;

import java.util.List;

/** Lambda entry point for "orders of a customer" (Architecture A). Input: {@code {"customerId": "CUST-42"}}. */
public class FindOrdersByCustomerHandler implements RequestHandler<CustomerQueryInput, List<Order>> {

    private final OrderService service;

    public FindOrdersByCustomerHandler() {
        this(OrderServiceProvider.orderService());
    }

    FindOrdersByCustomerHandler(OrderService service) {
        this.service = service;
    }

    @Override
    public List<Order> handleRequest(CustomerQueryInput input, Context context) {
        return service.getOrdersByCustomer(input == null ? null : input.getCustomerId());
    }
}
