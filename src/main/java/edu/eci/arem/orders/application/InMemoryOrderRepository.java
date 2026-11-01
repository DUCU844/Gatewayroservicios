package edu.eci.arem.orders.application;

import edu.eci.arem.orders.domain.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory repository.
 *
 * <p><b>Not persistent.</b> On Lambda it lives as long as one execution environment: it is lost on a cold
 * start and it is NOT shared between concurrent environments nor between different Lambda functions.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final AtomicInteger sequence;

    public InMemoryOrderRepository(int firstGeneratedNumber) {
        this.sequence = new AtomicInteger(firstGeneratedNumber);
    }

    /** Repository pre-loaded with the sample data used in the workshop statement. */
    public static InMemoryOrderRepository withSampleData() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository(43);
        repository.save(new Order("ORD-001", "CUST-42", "MacBook Air", 1));
        repository.save(new Order("ORD-002", "CUST-42", "USB-C Hub", 2));
        repository.save(new Order("ORD-42", "CUST-7", "Keyboard", 2));
        return repository;
    }

    @Override
    public String nextId() {
        return "ORD-" + sequence.getAndIncrement();
    }

    @Override
    public Order save(Order order) {
        orders.put(order.getId(), copy(order));
        return copy(order);
    }

    @Override
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(orders.get(id)).map(InMemoryOrderRepository::copy);
    }

    @Override
    public List<Order> findByCustomerId(String customerId) {
        List<Order> result = new ArrayList<>();
        orders.values().stream()
                .filter(order -> order.getCustomerId().equals(customerId))
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .forEach(order -> result.add(copy(order)));
        return result;
    }

    @Override
    public boolean deleteById(String id) {
        return orders.remove(id) != null;
    }

    // Defensive copies: callers (and the serializer) must not mutate stored state.
    private static Order copy(Order o) {
        return new Order(o.getId(), o.getCustomerId(), o.getProduct(), o.getQuantity());
    }
}
