package org.shvk.orderprocessingservice.model;

import java.time.Instant;

public record OrderResponse(
        long orderId,
        Instant orderDate,
        String orderStatus,
        long amount
) {
}
