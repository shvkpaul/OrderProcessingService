package org.shvk.orderprocessingservice.service;


import org.shvk.orderprocessingservice.model.OrderRequest;
import org.shvk.orderprocessingservice.model.OrderResponse;
import reactor.core.publisher.Mono;

public interface OrderProcessingService {
    Mono<OrderResponse> getOrderDetails(long orderId);

    long placeOrder(OrderRequest orderRequest);
}
