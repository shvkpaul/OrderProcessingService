package org.shvk.orderprocessingservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.log4j.Log4j2;
import org.shvk.orderprocessingservice.entity.Order;
import org.shvk.orderprocessingservice.exception.OrderNotFoundException;
import org.shvk.orderprocessingservice.exception.PaymentDetailsNotFoundException;
import org.shvk.orderprocessingservice.exception.ProductCatalogNotFoundException;
import org.shvk.orderprocessingservice.exception.ProductQuantityException;
import org.shvk.orderprocessingservice.model.*;
import org.shvk.orderprocessingservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@Log4j2
public class OrderProcessingServiceImpl implements OrderProcessingService {

//    @Value("${api-gateway.url}")
//    private String apiGatewayUrl;

    private OrderRepository orderRepository;
    private final WebClient webClient;

    public OrderProcessingServiceImpl(
            OrderRepository orderRepository,
            WebClient.Builder webClientBuilder
    ) {
        this.orderRepository = orderRepository;
        this.webClient = webClientBuilder.build();
    }

    @Override
    public Mono<OrderResponse> getOrderDetails(long orderId) {
        log.info("Get order details for the orderId: {}", orderId);

        // Fetch order details from the database asynchronously
        return Mono.fromSupplier(() -> orderRepository.findById(orderId)
                        .orElseThrow(() -> new OrderNotFoundException("OrderId not found: " + orderId)))
                .flatMap(order -> {
                    // Fetch product details and payment details concurrently
                    Mono<ProductDetails> productDetailsMono = getProductDetailsByProductId(order.getProductId());
                    Mono<PaymentDetails> paymentDetailsMono = getPaymentDetailsByOrderId(orderId);

                    return Mono.zip(productDetailsMono, paymentDetailsMono)
                            .map(tuple -> {
                                ProductDetails productDetails = tuple.getT1();
                                PaymentDetails paymentDetails = tuple.getT2();

                                // Construct and return the order response
                                return new OrderResponse(
                                        order.getOrderId(),
                                        order.getOrderDateTime(),
                                        order.getOrderStatus(),
                                        order.getAmount(),
                                        productDetails,
                                        paymentDetails
                                );
                            });
                });
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

        String orderStatus;

        try {
            webClient.post()
                    .uri("http://PAYMENT-SERVICE/payment")
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
                    .uri("http://PRODUCT-CATALOG-SERVICE/product/reduceQuantity/{id}?quantity={quantity}", productId, quantity)
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

    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackPaymentDetails")
    private Mono<PaymentDetails> getPaymentDetailsByOrderId(long orderId) {
        return webClient.get()
                .uri("http://PAYMENT-SERVICE/payment/order/{orderId}", orderId)
                .header("accept", "*/*")
                .retrieve()
                .bodyToMono(PaymentDetails.class)
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode().value() == 404) {
                        return Mono.error(new PaymentDetailsNotFoundException("Payment details not found for orderId: " + orderId));
                    }
                    return Mono.error(e);
                })
                .onErrorResume(throwable -> {
                    log.error("Payment service failed, fallback executed: {}", throwable.getMessage());
                    return Mono.just(new PaymentDetails(0L, "Unavailable", null, 0L, Instant.now(), orderId));
                });
    }

    private Mono<ProductDetails> getProductDetailsByProductId(long productId) {
        return webClient.get()
                .uri("http://PRODUCT-CATALOG-SERVICE/product/{id}", productId)
                .header("accept", "*/*")
                .retrieve()
                .bodyToMono(ProductDetails.class)
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode().value() == 404) {
                        return Mono.error(new ProductCatalogNotFoundException("Product not found"));
                    }
                    return Mono.error(e);
                })
                .onErrorResume(throwable -> {
                    log.error("Product service failed, fallback executed: {}", throwable.getMessage());
                    // Return default product details in case of failure
                    return Mono.just(new ProductDetails(productId, "Unavailable Product", 0L, 0));
                });
    }

    private PaymentDetails fallbackPaymentDetails(long orderId, Throwable throwable) {
        // Provide a default or fallback response
        return new PaymentDetails(
                0L,
                "Unavailable",
                null,
                0L,
                Instant.now(),
                orderId
        );
    }
}