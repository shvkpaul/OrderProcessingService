package org.shvk.orderprocessingservice.controller;

import org.shvk.orderprocessingservice.model.OrderRequest;
import org.shvk.orderprocessingservice.model.OrderResponse;
import org.shvk.orderprocessingservice.service.OrderProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderProcessingController {

    @Autowired
    private OrderProcessingService orderProcessingService;

    @PostMapping("/placeOrder")
    public ResponseEntity<Long> placeOrder(@RequestBody OrderRequest orderRequest) {
        long orderId = orderProcessingService.placeOrder(orderRequest);
        return new ResponseEntity<>(orderId, HttpStatus.CREATED);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderDetails(
            @PathVariable long orderId) {
        OrderResponse orderResponse = orderProcessingService.getOrderDetails(orderId);
        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }
}
