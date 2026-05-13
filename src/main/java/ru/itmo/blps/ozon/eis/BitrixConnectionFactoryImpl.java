package ru.itmo.blps.ozon.eis;

import jakarta.resource.ResourceException;
import java.net.http.HttpClient;

class BitrixConnectionFactoryImpl implements BitrixConnectionFactory {

    private final boolean enabled;
    private final String webhookUrl;
    private final long responsibleId;
    private final HttpClient httpClient;

    BitrixConnectionFactoryImpl(boolean enabled, String webhookUrl, long responsibleId, HttpClient httpClient) {
        this.enabled = enabled;
        this.webhookUrl = webhookUrl;
        this.responsibleId = responsibleId;
        this.httpClient = httpClient;
    }

    @Override
    public BitrixConnection getConnection() throws ResourceException {
        return new BitrixConnectionImpl(enabled, webhookUrl, responsibleId, httpClient);
    }
}
