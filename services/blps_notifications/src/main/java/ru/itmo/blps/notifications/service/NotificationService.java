package ru.itmo.blps.notifications.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.blps.notifications.dto.OrderStatusNotification;
import ru.itmo.blps.notifications.entity.NotificationHistory;
import ru.itmo.blps.notifications.repository.NotificationRepository;

import java.time.LocalDateTime;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository,
                                      SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void processAndSendNotification(OrderStatusNotification notification) {
        NotificationHistory history = NotificationHistory.builder()
                .orderId(notification.getOrderId())
                .status(notification.getNewStatus())
                .message(notification.getMessage())
                .sentAt(LocalDateTime.now())
                .build();
        notificationRepository.save(history);

        String destination = "/topic/orders/" + notification.getOrderId();
        messagingTemplate.convertAndSend(destination, notification);
        messagingTemplate.convertAndSend("/topic/orders", notification);
    }
}
