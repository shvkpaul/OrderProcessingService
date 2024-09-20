package org.shvk.orderprocessingservice.exception;

public class ProductQuantityException extends RuntimeException {
    public ProductQuantityException(String message) {
        super(message);
    }
}