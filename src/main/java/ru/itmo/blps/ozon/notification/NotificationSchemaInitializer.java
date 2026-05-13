package ru.itmo.blps.ozon.notification;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class NotificationSchemaInitializer implements ApplicationRunner {

    private final NotificationEventRepository notificationEventRepository;

    public NotificationSchemaInitializer(NotificationEventRepository notificationEventRepository) {
        this.notificationEventRepository = notificationEventRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        notificationEventRepository.initializeSchema();
    }
}
