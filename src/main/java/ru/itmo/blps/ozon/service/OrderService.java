package ru.itmo.blps.ozon.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.blps.ozon.dto.CancelOrderRequest;
import ru.itmo.blps.ozon.dto.CreateOrderRequest;
import ru.itmo.blps.ozon.dto.DeliveryResponse;
import ru.itmo.blps.ozon.dto.HandToDeliveryRequest;
import ru.itmo.blps.ozon.dto.OrderItemRequest;
import ru.itmo.blps.ozon.dto.OrderItemResponse;
import ru.itmo.blps.ozon.dto.OrderResponse;
import ru.itmo.blps.ozon.entity.Delivery;
import ru.itmo.blps.ozon.entity.Order;
import ru.itmo.blps.ozon.entity.OrderItem;
import ru.itmo.blps.ozon.entity.OrderStatus;
import ru.itmo.blps.ozon.exception.InvalidOrderStateException;
import ru.itmo.blps.ozon.exception.OrderNotFoundException;
import ru.itmo.blps.ozon.repository.OrderRepository;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final Clock clock;

    public OrderService(OrderRepository orderRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        LocalDateTime now = now();

        Order order = new Order();
        order.setCustomerName(request.getCustomerName());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setStatus(OrderStatus.CREATED);
        order.setStockAvailable(null);

        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setSku(itemRequest.getSku());
            item.setProductName(itemRequest.getProductName());
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(itemRequest.getUnitPrice());
            order.addItem(item);
        }

        if (!isStockAvailable(order)) {
            order.setStockAvailable(false);
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancellationReason("Недостаточно товара на складе");
            touch(order);
            orderRepository.save(order);
            throw new InvalidOrderStateException("Заказ отменен: недостаточно товара на складе");
        }
        order.setStockAvailable(true);

        return toResponse(orderRepository.save(order));
    }

    public OrderResponse acceptOrder(Long orderId) {
        return updateOrderStatus(orderId, OrderStatus.CREATED, OrderStatus.ACCEPTED);
    }

    public OrderResponse packOrder(Long orderId) {
        return updateOrderStatus(orderId, OrderStatus.ACCEPTED, OrderStatus.PACKED);
    }

    public OrderResponse handToDelivery(Long orderId, HandToDeliveryRequest request) {
        return updateOrderStatus(orderId, OrderStatus.PACKED, OrderStatus.IN_DELIVERY, order -> {
            Delivery delivery = order.getDelivery();
            if (delivery == null) {
                delivery = new Delivery();
                order.setDelivery(delivery);
            }
            delivery.setCarrierName(request.getCarrierName());
            delivery.setTrackingNumber(request.getTrackingNumber());
            delivery.setHandedAt(now());
        });
    }

    public OrderResponse markDelivered(Long orderId) {
        return updateOrderStatus(orderId, OrderStatus.IN_DELIVERY, OrderStatus.DELIVERED, order -> {
            order.getDelivery().setDeliveredAt(now());
        });
    }

    public OrderResponse cancelOrder(Long orderId, CancelOrderRequest request) {
        return updateOrderStatus(orderId, null, OrderStatus.CANCELLED, order -> {
            if (order.getStatus() == OrderStatus.CANCELLED) {
                throw new InvalidOrderStateException("Заказ уже отменен");
            }
            if (order.getStatus() == OrderStatus.IN_DELIVERY || order.getStatus() == OrderStatus.DELIVERED) {
                throw new InvalidOrderStateException("Заказ нельзя отменить после передачи в доставку");
            }
            order.setCancellationReason(request.getReason());
        });
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        return toResponse(findOrder(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    private OrderResponse updateOrderStatus(Long orderId, OrderStatus expectedStatus, OrderStatus nextStatus) {
        return updateOrderStatus(orderId, expectedStatus, nextStatus, null);
    }

    private OrderResponse updateOrderStatus(Long orderId, OrderStatus expectedStatus, OrderStatus nextStatus, Consumer<Order> additionalAction) {
        Order order = findOrder(orderId);

        if (expectedStatus != null) {
            ensureStatus(order, expectedStatus);
        }

        if (additionalAction != null) {
            additionalAction.accept(order);
        }

        order.setStatus(nextStatus);
        touch(order);
        return toResponse(orderRepository.save(order));
    }


    private void ensureStatus(Order order, OrderStatus expectedStatus) {
        if (order.getStatus() != expectedStatus) {
            throw new InvalidOrderStateException(
                    "Ожидаемый статус заказа: " + expectedStatus + ", текущий статус: " + order.getStatus()
            );
        }
    }

    private boolean isStockAvailable(Order order) {
        return order.getItems().stream().allMatch(item -> item.getQuantity() > 0);
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void touch(Order order) {
        order.setUpdatedAt(now());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .sku(item.getSku())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .build())
                .toList();

        DeliveryResponse deliveryResponse = null;
        if (order.getDelivery() != null) {
            deliveryResponse = DeliveryResponse.builder()
                    .id(order.getDelivery().getId())
                    .carrierName(order.getDelivery().getCarrierName())
                    .trackingNumber(order.getDelivery().getTrackingNumber())
                    .handedAt(order.getDelivery().getHandedAt())
                    .deliveredAt(order.getDelivery().getDeliveredAt())
                    .build();
        }

        return OrderResponse.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .deliveryAddress(order.getDeliveryAddress())
                .status(order.getStatus())
                .stockAvailable(order.getStockAvailable())
                .cancellationReason(order.getCancellationReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses)
                .delivery(deliveryResponse)
                .build();
    }
}
