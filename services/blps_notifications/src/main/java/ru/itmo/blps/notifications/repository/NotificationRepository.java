package ru.itmo.blps.notifications.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.blps.notifications.entity.NotificationHistory;

public interface NotificationRepository extends JpaRepository<NotificationHistory, Long> {
}