package ru.itmo.blps.ozon.bpm.delegate;

import java.math.BigDecimal;
import org.camunda.bpm.engine.delegate.DelegateExecution;

final class BpmVariableReader {

    private BpmVariableReader() {
    }

    static String stringValue(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    static boolean booleanValue(DelegateExecution execution, String name, boolean defaultValue) {
        Object value = execution.getVariable(name);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(value.toString());
    }

    static int intValue(DelegateExecution execution, String name, int defaultValue) {
        Object value = execution.getVariable(name);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    static long longValue(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    static BigDecimal decimalValue(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(value.toString());
    }
}
