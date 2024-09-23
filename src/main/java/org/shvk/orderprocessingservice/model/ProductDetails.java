package org.shvk.orderprocessingservice.model;

public record ProductDetails(
        long productId,
        String productName,
        long price,
        long quantity
) {
}
