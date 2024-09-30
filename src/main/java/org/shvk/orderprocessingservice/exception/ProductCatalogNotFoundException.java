package org.shvk.orderprocessingservice.exception;

public class ProductCatalogNotFoundException extends RuntimeException {
    public ProductCatalogNotFoundException(String message) {
        super(message);
    }
}