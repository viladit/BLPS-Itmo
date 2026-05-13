package ru.itmo.blps.ozon.notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long orderId,
        String type,
        String message,
        LocalDateTime createdAt
) {
}
