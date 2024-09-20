package org.shvk.orderprocessingservice.service;

import lombok.extern.log4j.Log4j2;
import org.shvk.orderprocessingservice.entity.Order;
import org.shvk.orderprocessingservice.model.OrderRequest;
import org.shvk.orderprocessingservice.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Log4j2
public class OrderProcessingServiceImpl implements OrderProcessingService {

    private OrderRepository orderRepository;

    public OrderProcessingServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public long placeOrder(OrderRequest orderRequest) {
        log.info("Placing order request: {}", orderRequest);

        Order order = Order.builder()
                .amount(orderRequest.getTotalAmount())
                .orderStatus("CREATED")
                .productId(orderRequest.getProductId())
                .orderDateTime(Instant.now())
                .quantity(orderRequest.getQuantity())
                .build();
        order = orderRepository.save(order);

        log.info("Order created successfully: {}", order.getOrderId());
        return order.getOrderId();
    }
}
