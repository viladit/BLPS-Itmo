package ru.itmo.blps.ozon.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.itmo.blps.ozon.dto.OrderStatusNotification;

@Service
@ConditionalOnProperty(name = "app.notifications.jms.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpOrderNotificationSender implements OrderNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(NoOpOrderNotificationSender.class);

    @Override
    public void send(OrderStatusNotification notification) {
        log.debug("JMS notifications are disabled, skipped order notification: {}", notification);
    }
}
