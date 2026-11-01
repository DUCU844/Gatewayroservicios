package edu.eci.arem.orders.lambda.direct;

/** Event built by the API Gateway mapping template from {@code ?customerId=}: {@code {"customerId": "CUST-42"}}. */
public class CustomerQueryInput {

    private String customerId;

    public CustomerQueryInput() {
    }

    public CustomerQueryInput(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
}
