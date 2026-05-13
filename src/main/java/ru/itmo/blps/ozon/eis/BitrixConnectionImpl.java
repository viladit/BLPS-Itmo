package ru.itmo.blps.ozon.eis;

import jakarta.resource.ResourceException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BitrixConnectionImpl implements BitrixConnection {

    private static final Logger log = LoggerFactory.getLogger(BitrixConnectionImpl.class);

    private final boolean enabled;
    private final String webhookUrl;
    private final long responsibleId;
    private final HttpClient httpClient;

    BitrixConnectionImpl(boolean enabled, String webhookUrl, long responsibleId, HttpClient httpClient) {
        this.enabled = enabled;
        this.webhookUrl = webhookUrl;
        this.responsibleId = responsibleId;
        this.httpClient = httpClient;
    }

    @Override
    public void createManagerTask(BitrixTaskRequest request) throws ResourceException {
        if (!enabled || webhookUrl == null || webhookUrl.isBlank()) {
            log.info("Bitrix24 JCA adapter is disabled, skipped EIS task for order {}", request.orderId());
            return;
        }

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(request)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ResourceException("Bitrix24 EIS returned HTTP " + response.statusCode());
            }
        } catch (IOException exception) {
            throw new ResourceException("Bitrix24 EIS request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResourceException("Bitrix24 EIS request was interrupted", exception);
        }
    }

    @Override
    public void close() {
    }

    private String toJson(BitrixTaskRequest request) {
        if (webhookUrl.contains("task.item.add")) {
            return """
                    {
                      "fields": {
                        "TITLE": "Проверить ожидающий заказ #%d",
                        "RESPONSIBLE_ID": %d,
                        "DESCRIPTION": "%s"
                      }
                    }
                    """.formatted(
                    request.orderId(),
                    responsibleId,
                    escapeJson(request.description())
            );
        }

        return """
                {
                  "fields": {
                      "TITLE": "Проверить ожидающий заказ #%d",
                      "RESPONSIBLE_ID": %d,
                      "DESCRIPTION": "%s",
                      "UF_CRM_TASK": ["ORDER_%d"]
                    }
                }
                """.formatted(
                request.orderId(),
                responsibleId,
                escapeJson(request.description()),
                request.orderId()
        );
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
