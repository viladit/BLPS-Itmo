package ru.itmo.blps.ozon.notification;

import org.springframework.stereotype.Service;
import ru.itmo.blps.ozon.entity.Order;

@Service
public class NotificationService {

    private final NotificationEventRepository notificationEventRepository;

    public NotificationService(NotificationEventRepository notificationEventRepository) {
        this.notificationEventRepository = notificationEventRepository;
    }

    public NotificationDraft prepareOrderCreated(String customerName, java.time.LocalDateTime createdAt, boolean failAfterWrite) {
        Long notificationId = notificationEventRepository.prepareOrderCreated(customerName, createdAt);
        if (failAfterWrite) {
            throw new NotificationDeliveryException("Notification database failed during distributed transaction");
        }
        return new NotificationDraft(notificationId);
    }

    public void completeOrderCreated(NotificationDraft draft, Order order) {
        notificationEventRepository.attachOrder(draft.id(), order.getId(), order.getCustomerName());
    }

    public java.util.List<NotificationResponse> findAll() {
        return notificationEventRepository.findAll();
    }
}
