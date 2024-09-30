package org.shvk.orderprocessingservice.exception;

public class PaymentDetailsNotFoundException extends RuntimeException {
    public PaymentDetailsNotFoundException(String message) {
        super(message);
    }
}