package edu.eci.arem.orders.lambda.direct;

import edu.eci.arem.orders.application.InMemoryOrderRepository;
import edu.eci.arem.orders.application.OrderService;
import edu.eci.arem.orders.domain.CreateOrderRequest;
import edu.eci.arem.orders.domain.InvalidOrderException;
import edu.eci.arem.orders.domain.Order;
import edu.eci.arem.orders.domain.OrderNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The direct handlers are thin: these tests check the delegation and the error-message contract used by API Gateway. */
class DirectHandlersTest {

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(InMemoryOrderRepository.withSampleData());
    }

    @Test
    void createHandlerReturnsCreatedOrder() {
        Order order = new CreateOrderHandler(service)
                .handleRequest(new CreateOrderRequest("CUST-42", "MacBook Air", 1), null);

        assertEquals("ORD-43", order.getId());
    }

    @Test
    void getHandlerReturnsOrder() {
        assertEquals("Keyboard", new GetOrderHandler(service).handleRequest(new OrderIdInput("ORD-42"), null).getProduct());
    }

    @Test
    void getHandlerErrorMessageMatchesGatewayRegex() {
        OrderNotFoundException ex = assertThrows(OrderNotFoundException.class,
                () -> new GetOrderHandler(service).handleRequest(new OrderIdInput("ORD-999"), null));

        assertTrue(ex.getMessage().matches("(?s)ORDER_NOT_FOUND:.*"));
    }

    @Test
    void findHandlerFiltersByCustomer() {
        assertEquals(2, new FindOrdersByCustomerHandler(service)
                .handleRequest(new CustomerQueryInput("CUST-42"), null).size());
    }

    @Test
    void findHandlerWithoutCustomerIsBadRequest() {
        InvalidOrderException ex = assertThrows(InvalidOrderException.class,
                () -> new FindOrdersByCustomerHandler(service).handleRequest(new CustomerQueryInput(""), null));

        assertTrue(ex.getMessage().matches("(?s)(INVALID_ORDER|MISSING_CUSTOMER_ID):.*"));
    }
}
