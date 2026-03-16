package ru.itmo.blps.ozon.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.itmo.blps.ozon.entity.OrderStatus;

@Getter
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String customerName;
    private String deliveryAddress;
    private OrderStatus status;
    private Boolean stockAvailable;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponse> items;
    private DeliveryResponse delivery;
}
