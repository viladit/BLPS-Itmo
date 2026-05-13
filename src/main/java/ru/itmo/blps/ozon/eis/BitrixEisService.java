package ru.itmo.blps.ozon.eis;

import jakarta.resource.ResourceException;
import org.springframework.stereotype.Service;
import ru.itmo.blps.ozon.entity.Order;

@Service
public class BitrixEisService {

    private final BitrixConnectionFactory connectionFactory;

    public BitrixEisService(BitrixConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void createPendingOrderTask(Order order) {
        BitrixTaskRequest request = new BitrixTaskRequest(
                order.getId(),
                order.getCustomerName(),
                order.getDeliveryAddress(),
                order.getStatus().name(),
                "Заказ ожидает подтверждения менеджером. Клиент: "
                        + order.getCustomerName()
                        + ", адрес доставки: "
                        + order.getDeliveryAddress()
        );

        try (BitrixConnection connection = connectionFactory.getConnection()) {
            connection.createManagerTask(request);
        } catch (ResourceException exception) {
            throw new EisIntegrationException("Не удалось создать задачу во внешней EIS Bitrix24", exception);
        }
    }
}
