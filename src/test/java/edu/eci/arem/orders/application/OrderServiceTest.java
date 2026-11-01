package edu.eci.arem.orders.application;

import edu.eci.arem.orders.domain.CreateOrderRequest;
import edu.eci.arem.orders.domain.InvalidOrderException;
import edu.eci.arem.orders.domain.Order;
import edu.eci.arem.orders.domain.OrderNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderServiceTest {

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(InMemoryOrderRepository.withSampleData());
    }

    @Test
    void createsValidOrderWithGeneratedId() {
        Order created = service.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 1));

        assertEquals("ORD-43", created.getId());
        assertEquals("CUST-42", created.getCustomerId());
        assertEquals("MacBook Air", created.getProduct());
        assertEquals(1, created.getQuantity());
    }

    @Test
    void createdOrderCanBeRetrieved() {
        Order created = service.createOrder(new CreateOrderRequest("CUST-9", "Mouse", 3));

        assertEquals(created, service.getOrder(created.getId()));
    }

    @Test
    void generatesDifferentIdsForConsecutiveOrders() {
        Order first = service.createOrder(new CreateOrderRequest("CUST-1", "A", 1));
        Order second = service.createOrder(new CreateOrderRequest("CUST-1", "B", 1));

        assertTrue(!first.getId().equals(second.getId()));
    }

    @Test
    void retrievesExistingOrder() {
        Order order = service.getOrder("ORD-42");

        assertEquals(new Order("ORD-42", "CUST-7", "Keyboard", 2), order);
    }

    @Test
    void failsWhenOrderDoesNotExist() {
        OrderNotFoundException ex = assertThrows(OrderNotFoundException.class, () -> service.getOrder("ORD-999"));

        assertEquals("ORD-999", ex.getOrderId());
        assertEquals("ORDER_NOT_FOUND: Order ORD-999 does not exist", ex.getMessage());
    }

    @Test
    void findsOrdersOfCustomer() {
        List<Order> orders = service.getOrdersByCustomer("CUST-42");

        assertEquals(2, orders.size());
        assertTrue(orders.stream().allMatch(o -> o.getCustomerId().equals("CUST-42")));
    }

    @Test
    void returnsEmptyListForCustomerWithoutOrders() {
        List<Order> orders = service.getOrdersByCustomer("CUST-100");

        assertNotNull(orders);
        assertTrue(orders.isEmpty());
    }

    @Test
    void rejectsMissingCustomerIdInSearch() {
        InvalidOrderException ex = assertThrows(InvalidOrderException.class, () -> service.getOrdersByCustomer(" "));

        assertEquals(InvalidOrderException.MISSING_CUSTOMER_ID, ex.getCode());
    }

    @Test
    void rejectsNullRequest() {
        assertThrows(InvalidOrderException.class, () -> service.createOrder(null));
    }

    @Test
    void rejectsBlankCustomer() {
        InvalidOrderException ex = assertThrows(InvalidOrderException.class,
                () -> service.createOrder(new CreateOrderRequest("  ", "MacBook Air", 1)));

        assertEquals("INVALID_ORDER", ex.getCode());
        assertEquals("customerId is required", ex.getDescription());
    }

    @Test
    void rejectsMissingProduct() {
        assertThrows(InvalidOrderException.class,
                () -> service.createOrder(new CreateOrderRequest("CUST-42", null, 1)));
    }

    @Test
    void rejectsMissingQuantity() {
        InvalidOrderException ex = assertThrows(InvalidOrderException.class,
                () -> service.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", null)));

        assertEquals("quantity is required", ex.getDescription());
    }

    @Test
    void rejectsNonPositiveQuantity() {
        InvalidOrderException ex = assertThrows(InvalidOrderException.class,
                () -> service.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 0)));

        assertEquals("INVALID_ORDER: quantity must be at least 1", ex.getMessage());
    }

    @Test
    void deletesExistingOrder() {
        service.deleteOrder("ORD-42");

        assertThrows(OrderNotFoundException.class, () -> service.getOrder("ORD-42"));
        assertTrue(service.getOrdersByCustomer("CUST-7").isEmpty());
    }

    @Test
    void deletingMissingOrderFails() {
        OrderNotFoundException ex = assertThrows(OrderNotFoundException.class, () -> service.deleteOrder("ORD-999"));

        assertEquals("ORD-999", ex.getOrderId());
    }

    @Test
    void deletingTwiceFailsTheSecondTime() {
        service.deleteOrder("ORD-001");

        assertThrows(OrderNotFoundException.class, () -> service.deleteOrder("ORD-001"));
    }

    @Test
    void invalidOrderIsNotStored() {
        assertThrows(InvalidOrderException.class,
                () -> service.createOrder(new CreateOrderRequest("CUST-55", "Phone", -1)));

        assertTrue(service.getOrdersByCustomer("CUST-55").isEmpty());
    }
}
