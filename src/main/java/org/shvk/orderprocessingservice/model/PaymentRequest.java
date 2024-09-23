package org.shvk.orderprocessingservice.model;

public record PaymentRequest(
        long orderId,
        long amount,
        String referenceNumber,
        PaymentMode paymentMode
) {
}
