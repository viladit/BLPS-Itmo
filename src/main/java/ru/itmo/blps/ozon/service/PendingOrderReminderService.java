package ru.itmo.blps.ozon.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.blps.ozon.eis.BitrixEisService;
import ru.itmo.blps.ozon.entity.Order;
import ru.itmo.blps.ozon.entity.OrderStatus;
import ru.itmo.blps.ozon.exception.OrderNotFoundException;
import ru.itmo.blps.ozon.repository.OrderRepository;

@Service
public class PendingOrderReminderService {

    private final OrderRepository orderRepository;
    private final OrderNotificationService orderNotificationService;
    private final BitrixEisService bitrixEisService;
    private final Clock clock;
    private final Duration pendingAge;
    private final Duration repeatInterval;

    public PendingOrderReminderService(OrderRepository orderRepository,
                                       OrderNotificationService orderNotificationService,
                                       BitrixEisService bitrixEisService,
                                       Clock clock,
                                       @Value("${app.orders.pending-reminder.age}") Duration pendingAge,
                                       @Value("${app.orders.pending-reminder.repeat-interval}") Duration repeatInterval) {
        this.orderRepository = orderRepository;
        this.orderNotificationService = orderNotificationService;
        this.bitrixEisService = bitrixEisService;
        this.clock = clock;
        this.pendingAge = pendingAge;
        this.repeatInterval = repeatInterval;
    }

    @Transactional(rollbackFor = Exception.class)
    public int processPendingOrders() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Order> pendingOrders = orderRepository.findPendingOrdersForReminder(
                OrderStatus.CREATED,
                now.minus(pendingAge),
                now.minus(repeatInterval)
        );

        for (Order order : pendingOrders) {
            if (!order.isPendingEisTaskCreated()) {
                bitrixEisService.createPendingOrderTask(order);
                order.markPendingEisTaskCreated();
            }
            orderNotificationService.publishAfterCommit(
                    order,
                    "Заказ ожидает подтверждения менеджером дольше " + formatDuration(pendingAge)
            );
            order.markPendingReminderSent(now);
            order.touch(now);
        }

        return pendingOrders.size();
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean processPendingOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (order.getStatus() != OrderStatus.CREATED) {
            return false;
        }
        sendReminder(order, LocalDateTime.now(clock));
        return true;
    }

    private void sendReminder(Order order, LocalDateTime now) {
        if (!order.isPendingEisTaskCreated()) {
            bitrixEisService.createPendingOrderTask(order);
            order.markPendingEisTaskCreated();
        }
        orderNotificationService.publishAfterCommit(
                order,
                "Заказ ожидает подтверждения менеджером дольше " + formatDuration(pendingAge)
        );
        order.markPendingReminderSent(now);
        order.touch(now);
    }

    private String formatDuration(Duration duration) {
        long minutes = duration.toMinutes();
        if (minutes > 0) {
            return minutes + " мин.";
        }
        return duration.toSeconds() + " сек.";
    }
}
