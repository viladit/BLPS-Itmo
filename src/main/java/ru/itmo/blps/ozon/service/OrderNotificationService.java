package ru.itmo.blps.ozon.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.itmo.blps.ozon.dto.OrderStatusNotification;
import ru.itmo.blps.ozon.entity.Order;

@Service
public class OrderNotificationService {

    private final OrderNotificationSender notificationSender;

    public OrderNotificationService(OrderNotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    public void publishAfterCommit(Order order, String message) {
        OrderStatusNotification notification = OrderStatusNotification.builder()
                .orderId(order.getId())
                .customerName(order.getCustomerName())
                .newStatus(order.getStatus().name())
                .message(message)
                .build();

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notificationSender.send(notification);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationSender.send(notification);
            }
        });
    }
}
