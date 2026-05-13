package ru.itmo.blps.ozon.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import ru.itmo.blps.ozon.dto.OrderStatusNotification;

@Service
@ConditionalOnProperty(name = "app.notifications.jms.enabled", havingValue = "true")
public class JmsOrderNotificationSender implements OrderNotificationSender {

    private final JmsTemplate jmsTemplate;
    private final String queueName;

    public JmsOrderNotificationSender(JmsTemplate jmsTemplate,
                                      @Value("${app.jms.queue-name}") String queueName) {
        this.jmsTemplate = jmsTemplate;
        this.queueName = queueName;
    }

    @Override
    public void send(OrderStatusNotification notification) {
        jmsTemplate.convertAndSend(queueName, notification);
    }
}
