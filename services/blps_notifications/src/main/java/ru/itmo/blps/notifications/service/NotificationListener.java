package ru.itmo.blps.notifications.service;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import ru.itmo.blps.notifications.dto.OrderStatusNotification;;

@Component
public class NotificationListener {

    private final NotificationService notificationService;

    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @JmsListener(destination = "${app.jms.queue-name}")
    public void receiveNotification(OrderStatusNotification notification) {
        notificationService.processAndSendNotification(notification);
    }
}