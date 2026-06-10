package ru.itmo.blps.ozon.bpm.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.itmo.blps.ozon.service.PendingOrderReminderService;

@Component("pendingReminderDelegate")
public class PendingReminderDelegate implements JavaDelegate {

    private final PendingOrderReminderService pendingOrderReminderService;

    public PendingReminderDelegate(PendingOrderReminderService pendingOrderReminderService) {
        this.pendingOrderReminderService = pendingOrderReminderService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        pendingOrderReminderService.processPendingOrder(BpmVariableReader.longValue(execution, "orderId"));
    }
}
