package ru.itmo.blps.ozon.service;

import ru.itmo.blps.ozon.dto.OrderStatusNotification;

public interface OrderNotificationSender {

    void send(OrderStatusNotification notification);
}
