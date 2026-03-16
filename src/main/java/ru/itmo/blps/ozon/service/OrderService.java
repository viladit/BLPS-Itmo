package ru.itmo.blps.ozon.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
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
        order.setStatus(OrderStatus.ORDER_RECEIVED);

        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setSku(itemRequest.getSku());
            item.setProductName(itemRequest.getProductName());
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(itemRequest.getUnitPrice());
            order.addItem(item);
        }

        moveToStatus(order, OrderStatus.ORDER_RECEIVED, OrderStatus.ORDER_CONTENTS_LOADED);

        return toResponse(orderRepository.save(order));
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

    public OrderResponse checkStock(Long orderId) {
        Order order = findOrder(orderId);
        moveToStatus(order, OrderStatus.ORDER_CONTENTS_LOADED, OrderStatus.STOCK_AVAILABILITY_CHECKED);
        order.setStockAvailable(isStockAvailable(order));
        touch(order);
        return toResponse(orderRepository.save(order));
    }

    public OrderResponse reserveItems(Long orderId) {
        Order order = findOrder(orderId);
        ensureStatus(order, OrderStatus.STOCK_AVAILABILITY_CHECKED);
        if (!Boolean.TRUE.equals(order.getStockAvailable())) {
            throw new InvalidOrderStateException("Items cannot be reserved because stock is not available");
        }
        order.setStatus(OrderStatus.ITEMS_RESERVED);
        touch(order);
        return toResponse(orderRepository.save(order));
    }

    public OrderResponse confirmOrder(Long orderId) {
        return updateOrderStatus(orderId, OrderStatus.ITEMS_RESERVED, OrderStatus.SELLER_CONFIRMED);
    }

    public OrderResponse createPickingTask(Long orderId) {
        return updateOrderStatus(orderId, OrderStatus.SELLER_CONFIRMED, OrderStatus.PICKING_TASK_CREATED);
    }

    public OrderResponse markPicked(Long orderId) {
        return updateOrderStatus(orderId, OrderStatus.PICKING_TASK_CREATED, OrderStatus.ORDER_PICKED);
    }

    public OrderResponse packOrder(Long orderId) {
        return updateOrderStatus(orderId, OrderStatus.ORDER_PICKED, OrderStatus.ORDER_PACKED);
    }

    public OrderResponse handToDelivery(Long orderId, HandToDeliveryRequest request) {
        Order order = findOrder(orderId);
        moveToStatus(order, OrderStatus.ORDER_PACKED, OrderStatus.HANDED_TO_DELIVERY);

        Delivery delivery = order.getDelivery();
        if (delivery == null) {
            delivery = new Delivery();
            order.setDelivery(delivery);
        }

        delivery.setCarrierName(request.getCarrierName());
        delivery.setTrackingNumber(request.getTrackingNumber());
        delivery.setHandedAt(now());
        touch(order);

        return toResponse(orderRepository.save(order));
    }

    public OrderResponse markDelivered(Long orderId) {
        Order order = findOrder(orderId);
        moveToStatus(order, OrderStatus.HANDED_TO_DELIVERY, OrderStatus.DELIVERED_AND_CLOSED);

        if (order.getDelivery() == null) {
            throw new InvalidOrderStateException("Order cannot be marked as delivered without delivery information");
        }

        order.getDelivery().setDeliveredAt(now());
        touch(order);

        return toResponse(orderRepository.save(order));
    }

    public OrderResponse cancelOrder(Long orderId, CancelOrderRequest request) {
        Order order = findOrder(orderId);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("Order is already cancelled");
        }
        if (order.getStatus() == OrderStatus.HANDED_TO_DELIVERY
                || order.getStatus() == OrderStatus.DELIVERED_AND_CLOSED) {
            throw new InvalidOrderStateException("Order cannot be cancelled after handoff to delivery");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(request.getReason());
        touch(order);

        return toResponse(orderRepository.save(order));
    }

    private OrderResponse updateOrderStatus(Long orderId, OrderStatus expectedStatus, OrderStatus nextStatus) {
        Order order = findOrder(orderId);
        moveToStatus(order, expectedStatus, nextStatus);
        touch(order);
        return toResponse(orderRepository.save(order));
    }

    private void moveToStatus(Order order, OrderStatus expectedStatus, OrderStatus nextStatus) {
        ensureStatus(order, expectedStatus);
        order.setStatus(nextStatus);
    }

    private void ensureStatus(Order order, OrderStatus expectedStatus) {
        if (order.getStatus() != expectedStatus) {
            throw new InvalidOrderStateException(
                    "Expected order status " + expectedStatus + " but was " + order.getStatus()
            );
        }
    }

    private boolean isStockAvailable(Order order) {
        return order.getItems().stream().allMatch(item -> item.getQuantity() <= 100);
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
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getSku(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .toList();

        DeliveryResponse deliveryResponse = null;
        if (order.getDelivery() != null) {
            deliveryResponse = new DeliveryResponse(
                    order.getDelivery().getId(),
                    order.getDelivery().getCarrierName(),
                    order.getDelivery().getTrackingNumber(),
                    order.getDelivery().getHandedAt(),
                    order.getDelivery().getDeliveredAt()
            );
        }

        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getDeliveryAddress(),
                order.getStatus(),
                order.getStockAvailable(),
                order.getCancellationReason(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemResponses,
                deliveryResponse
        );
    }
}
