package edu.eci.arem.orders.lambda;

import edu.eci.arem.orders.application.InMemoryOrderRepository;
import edu.eci.arem.orders.application.OrderService;

/**
 * Composition root shared by every Lambda handler.
 *
 * <p>The service is created once per execution environment (static init runs during the Lambda "Init"
 * phase) and reused by every warm invocation of that environment.
 */
public final class OrderServiceProvider {

    private static final OrderService SERVICE = new OrderService(InMemoryOrderRepository.withSampleData());

    private OrderServiceProvider() {
    }

    public static OrderService orderService() {
        return SERVICE;
    }
}
