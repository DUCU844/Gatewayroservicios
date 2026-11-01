package edu.eci.arem.orders.domain;

/**
 * Thrown when the input of an operation violates a business rule (blank customer, quantity below 1, ...).
 *
 * <p>Message format: {@code <CODE>: <description>}, e.g. {@code INVALID_ORDER: quantity must be at least 1}.
 */
public class InvalidOrderException extends RuntimeException {

    public static final String INVALID_ORDER = "INVALID_ORDER";
    public static final String MISSING_CUSTOMER_ID = "MISSING_CUSTOMER_ID";

    private final String code;
    private final String description;

    public InvalidOrderException(String code, String description) {
        super(code + ": " + description);
        this.code = code;
        this.description = description;
    }

    public InvalidOrderException(String description) {
        this(INVALID_ORDER, description);
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
