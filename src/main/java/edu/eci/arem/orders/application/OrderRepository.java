package edu.eci.arem.orders.application;

import edu.eci.arem.orders.domain.Order;

import java.util.List;
import java.util.Optional;

/**
 * Storage port. The workshop uses an in-memory implementation; a DynamoDB one could replace it without
 * touching {@link OrderService}.
 */
public interface OrderRepository {

    String nextId();

    Order save(Order order);

    Optional<Order> findById(String id);

    List<Order> findByCustomerId(String customerId);

    /** @return {@code true} if an order was removed, {@code false} if no order had that id. */
    boolean deleteById(String id);
}
