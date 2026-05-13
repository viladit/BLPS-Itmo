package ru.itmo.blps.ozon.eis;

public record BitrixTaskRequest(
        Long orderId,
        String customerName,
        String deliveryAddress,
        String status,
        String description
) {
}
