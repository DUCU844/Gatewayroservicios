package edu.eci.arem.orders.domain;

/**
 * Thrown when an order id does not identify any existing order.
 *
 * <p>The message always starts with the error code ({@code ORDER_NOT_FOUND: ...}). That prefix is what the
 * API Gateway non-proxy integration matches with its "Lambda error regex" to choose the 404 response.
 */
public class OrderNotFoundException extends RuntimeException {

    public static final String CODE = "ORDER_NOT_FOUND";

    private final String orderId;

    public OrderNotFoundException(String orderId) {
        super(CODE + ": Order " + orderId + " does not exist");
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }

    /** Message without the code prefix, suitable for the {@code message} field of an error response. */
    public String getDescription() {
        return "Order " + orderId + " does not exist";
    }
}
