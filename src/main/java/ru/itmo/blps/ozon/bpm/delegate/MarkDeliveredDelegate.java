package ru.itmo.blps.ozon.bpm.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.itmo.blps.ozon.service.OrderService;

@Component("markDeliveredDelegate")
public class MarkDeliveredDelegate implements JavaDelegate {

    private final OrderService orderService;

    public MarkDeliveredDelegate(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        orderService.markDelivered(BpmVariableReader.longValue(execution, "orderId"));
    }
}
