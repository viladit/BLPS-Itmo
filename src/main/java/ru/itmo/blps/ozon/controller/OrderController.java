package ru.itmo.blps.ozon.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.blps.ozon.dto.CancelOrderRequest;
import ru.itmo.blps.ozon.dto.CreateOrderRequest;
import ru.itmo.blps.ozon.dto.HandToDeliveryRequest;
import ru.itmo.blps.ozon.dto.OrderResponse;
import ru.itmo.blps.ozon.facade.OrderAccessFacade;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderAccessFacade orderAccessFacade;

    public OrderController(OrderAccessFacade orderAccessFacade) {
        this.orderAccessFacade = orderAccessFacade;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderAccessFacade.createOrder(request);
        return ResponseEntity.created(URI.create("/api/orders/" + response.getId())).body(response);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrderById(@PathVariable Long orderId) {
        return orderAccessFacade.getOrderById(orderId);
    }

    @GetMapping
    public List<OrderResponse> listOrders() {
        return orderAccessFacade.getAllOrders();
    }


    @PostMapping("/{orderId}/accept")
    public OrderResponse acceptOrder(@PathVariable Long orderId) {
        return orderAccessFacade.acceptOrder(orderId);
    }

    @PostMapping("/{orderId}/pack")
    public OrderResponse packOrder(@PathVariable Long orderId) {
        return orderAccessFacade.packOrder(orderId);
    }

    @PostMapping("/{orderId}/handoff")
    public OrderResponse handToDelivery(@PathVariable Long orderId,
                                        @Valid @RequestBody HandToDeliveryRequest request) {
        return orderAccessFacade.handToDelivery(orderId, request);
    }

    @PostMapping("/{orderId}/deliver")
    public OrderResponse markDelivered(@PathVariable Long orderId) {
        return orderAccessFacade.markDelivered(orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long orderId,
                                     @Valid @RequestBody CancelOrderRequest request) {
        return orderAccessFacade.cancelOrder(orderId, request);
    }
}
