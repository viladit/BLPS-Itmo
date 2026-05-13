package ru.itmo.blps.ozon.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import ru.itmo.blps.ozon.dto.OrderStatusNotification;
import ru.itmo.blps.ozon.entity.Order;

@Service
public class NotificationProducer {

    private final JmsTemplate jmsTemplate;
    @Value("${app.jms.queue-name}")
    private String queueName;

    public NotificationProducer(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void sendStatusChange(Order order, String message) {
        OrderStatusNotification notification = OrderStatusNotification.builder()
                .orderId(order.getId())
                .customerName(order.getCustomerName())
                .newStatus(order.getStatus().name())
                .message(message)
                .build();

        jmsTemplate.convertAndSend(queueName, notification);
    }
}