package org.shvk.orderprocessingservice.service;


import org.shvk.orderprocessingservice.model.OrderRequest;
import org.shvk.orderprocessingservice.model.OrderResponse;

public interface OrderProcessingService {
    OrderResponse getOrderDetails(long orderId) ;

    long placeOrder(OrderRequest orderRequest);
}
