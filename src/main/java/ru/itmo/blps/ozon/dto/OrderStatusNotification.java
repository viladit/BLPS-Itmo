package ru.itmo.blps.ozon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusNotification {
    private Long orderId;
    private String customerName;
    private String newStatus;
    private String message;
}
