package org.shvk.orderprocessingservice.service;


import org.shvk.orderprocessingservice.model.OrderRequest;

public interface OrderProcessingService {
    long placeOrder(OrderRequest orderRequest);
}
