package ru.itmo.blps.ozon.bpm.delegate;

import java.util.List;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.itmo.blps.ozon.dto.CreateOrderRequest;
import ru.itmo.blps.ozon.dto.OrderItemRequest;
import ru.itmo.blps.ozon.dto.OrderResponse;
import ru.itmo.blps.ozon.service.OrderService;

@Component("createOrderDelegate")
public class CreateOrderDelegate implements JavaDelegate {

    private final OrderService orderService;

    public CreateOrderDelegate(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerName(BpmVariableReader.stringValue(execution, "customerName"))
                .deliveryAddress(BpmVariableReader.stringValue(execution, "deliveryAddress"))
                .items(List.of(OrderItemRequest.builder()
                        .sku(BpmVariableReader.stringValue(execution, "sku"))
                        .productName(BpmVariableReader.stringValue(execution, "productName"))
                        .quantity(BpmVariableReader.intValue(execution, "quantity", 1))
                        .unitPrice(BpmVariableReader.decimalValue(execution, "unitPrice"))
                        .build()))
                .build();

        boolean failNotification = BpmVariableReader.booleanValue(execution, "failNotification", false);
        int notificationPauseSeconds = BpmVariableReader.intValue(execution, "notificationPauseSeconds", 0);

        OrderResponse response = orderService.createOrder(request, failNotification, notificationPauseSeconds);
        execution.setVariable("orderId", response.getId());
    }
}
