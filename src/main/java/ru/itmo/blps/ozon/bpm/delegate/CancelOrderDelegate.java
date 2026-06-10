package ru.itmo.blps.ozon.bpm.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.itmo.blps.ozon.dto.CancelOrderRequest;
import ru.itmo.blps.ozon.service.OrderService;

@Component("cancelOrderDelegate")
public class CancelOrderDelegate implements JavaDelegate {

    private final OrderService orderService;

    public CancelOrderDelegate(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String reason = BpmVariableReader.stringValue(execution, "cancellationReason");
        if (reason == null) {
            reason = "Заказ отменен в BPMN-процессе";
        }
        orderService.cancelOrder(
                BpmVariableReader.longValue(execution, "orderId"),
                CancelOrderRequest.builder().reason(reason).build()
        );
    }
}
