package edu.eci.arem.orders.application;

import edu.eci.arem.orders.domain.CreateOrderRequest;
import edu.eci.arem.orders.domain.InvalidOrderException;
import edu.eci.arem.orders.domain.Order;
import edu.eci.arem.orders.domain.OrderNotFoundException;

import java.util.List;

/**
 * Application logic of the Orders API.
 *
 * <p>Ordinary Java: it knows nothing about HTTP, status codes, API Gateway or Lambda. Outcomes are expressed
 * with return values and domain exceptions; translating them to HTTP is the job of the adapters.
 */
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public Order createOrder(CreateOrderRequest request) {
        if (request == null) {
            throw new InvalidOrderException("request body is required");
        }
        String customerId = requireText(request.getCustomerId(), "customerId");
        String product = requireText(request.getProduct(), "product");
        if (request.getQuantity() == null) {
            throw new InvalidOrderException("quantity is required");
        }
        if (request.getQuantity() < 1) {
            throw new InvalidOrderException("quantity must be at least 1");
        }
        Order order = new Order(repository.nextId(), customerId, product, request.getQuantity());
        return repository.save(order);
    }

    public Order getOrder(String id) {
        if (id == null || id.isBlank()) {
            throw new InvalidOrderException("order id is required");
        }
        return repository.findById(id.trim()).orElseThrow(() -> new OrderNotFoundException(id.trim()));
    }

    public List<Order> getOrdersByCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new InvalidOrderException(InvalidOrderException.MISSING_CUSTOMER_ID,
                    "query parameter customerId is required");
        }
        return repository.findByCustomerId(customerId.trim());
    }

    public void deleteOrder(String id) {
        if (id == null || id.isBlank()) {
            throw new InvalidOrderException("order id is required");
        }
        if (!repository.deleteById(id.trim())) {
            throw new OrderNotFoundException(id.trim());
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidOrderException(field + " is required");
        }
        return value.trim();
    }
}
