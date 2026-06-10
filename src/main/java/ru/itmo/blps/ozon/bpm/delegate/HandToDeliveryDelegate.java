package ru.itmo.blps.ozon.bpm.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.itmo.blps.ozon.dto.HandToDeliveryRequest;
import ru.itmo.blps.ozon.service.OrderService;

@Component("handToDeliveryDelegate")
public class HandToDeliveryDelegate implements JavaDelegate {

    private final OrderService orderService;

    public HandToDeliveryDelegate(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        HandToDeliveryRequest request = HandToDeliveryRequest.builder()
                .carrierName(BpmVariableReader.stringValue(execution, "carrierName"))
                .trackingNumber(BpmVariableReader.stringValue(execution, "trackingNumber"))
                .build();
        orderService.handToDelivery(BpmVariableReader.longValue(execution, "orderId"), request);
    }
}
