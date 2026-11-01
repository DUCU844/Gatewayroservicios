package edu.eci.arem.orders.domain;

import java.util.Objects;

/**
 * An order of a product placed by a customer.
 *
 * <p>Plain JavaBean (no-arg constructor + getters/setters) on purpose: it is the shape the AWS Lambda
 * Java runtime serializer and Jackson can map to and from JSON without any annotation.
 */
public class Order {

    private String id;
    private String customerId;
    private String product;
    private int quantity;

    public Order() {
    }

    public Order(String id, String customerId, String product, int quantity) {
        this.id = id;
        this.customerId = customerId;
        this.product = product;
        this.quantity = quantity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Order other)) {
            return false;
        }
        return quantity == other.quantity
                && Objects.equals(id, other.id)
                && Objects.equals(customerId, other.customerId)
                && Objects.equals(product, other.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, customerId, product, quantity);
    }

    @Override
    public String toString() {
        return "Order{id='" + id + "', customerId='" + customerId + "', product='" + product
                + "', quantity=" + quantity + '}';
    }
}
