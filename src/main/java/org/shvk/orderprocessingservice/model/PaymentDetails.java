package org.shvk.orderprocessingservice.model;

import java.time.Instant;

public record PaymentDetails(
        long paymentId,
        String status,
        PaymentMode paymentMode,
        long amount,
        Instant paymentDate,
        long orderId
) {
}
