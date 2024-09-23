package org.shvk.orderprocessingservice.service;

import lombok.extern.log4j.Log4j2;
import org.shvk.orderprocessingservice.entity.Order;
import org.shvk.orderprocessingservice.exception.ProductCatalogNotFoundException;
import org.shvk.orderprocessingservice.exception.ProductQuantityException;
import org.shvk.orderprocessingservice.model.OrderRequest;
import org.shvk.orderprocessingservice.model.PaymentRequest;
import org.shvk.orderprocessingservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.UUID;

@Service
@Log4j2
public class OrderProcessingServiceImpl implements OrderProcessingService {

    private OrderRepository orderRepository;
    private WebClient webClient;

    public OrderProcessingServiceImpl(
            OrderRepository orderRepository,
            WebClient webClient
    ) {
        this.orderRepository = orderRepository;
        this.webClient = webClient;
    }

    @Override
    public long placeOrder(OrderRequest orderRequest) {
        log.info("Placing order request: {}", orderRequest);

        reduceProductQuantity(orderRequest.getProductId(), orderRequest.getQuantity());

        log.info("Reduced quantity by calling product service");

        Order order = Order.builder()
                .amount(orderRequest.getTotalAmount())
                .orderStatus("CREATED")
                .productId(orderRequest.getProductId())
                .orderDateTime(Instant.now())
                .quantity(orderRequest.getQuantity())
                .build();

        log.info("Creating order request {}", order);

        order = orderRepository.save(order);

        log.info("Calling payment service to complete the payment");

        PaymentRequest paymentRequest
                = new PaymentRequest(
                order.getOrderId(),
                orderRequest.getTotalAmount(),
                UUID.randomUUID().toString(),
                orderRequest.getPaymentMode());

        String orderStatus = null;

        try {
            webClient.post()
                    .uri("http://localhost:8082/payment")
                    .header("accept", "*/*")
                    .header("Content-Type", "application/json")
                    .bodyValue(paymentRequest)
                    .retrieve()
                    .bodyToMono(Long.class)
                    .block();

            log.info("Payment done successfully, changing order status to PLACED");
            orderStatus = "PLACED";
        } catch (Exception e) {
            log.error("Payment failed, changing order status to PAYMENT_FAILED");
            orderStatus = "PAYMENT_FAILED";
        }

        order.setOrderStatus(orderStatus);

        orderRepository.save(order);

        log.info("Order created successfully: {}", order.getOrderId());

        return order.getOrderId();
    }

    private String reduceProductQuantity(long productId, long quantity) {
        try {
            return webClient.put()
                    .uri("http://localhost:8080/product/reduceQuantity/{id}?quantity={quantity}", productId, quantity)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 400) {
                throw new ProductQuantityException("Product does not have sufficient quantity");
            } else if (e.getStatusCode().value() == 404) {
                throw new ProductCatalogNotFoundException("Product not found");
            }
            throw e;
        }
    }
}