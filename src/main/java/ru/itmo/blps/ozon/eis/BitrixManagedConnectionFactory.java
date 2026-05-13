package ru.itmo.blps.ozon.eis;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;
import java.io.PrintWriter;
import java.io.Serial;
import java.net.http.HttpClient;
import java.util.Objects;
import java.util.Set;
import javax.security.auth.Subject;

public class BitrixManagedConnectionFactory implements ManagedConnectionFactory {

    @Serial
    private static final long serialVersionUID = 1L;

    private boolean enabled;
    private String webhookUrl;
    private long responsibleId = 1L;
    private transient PrintWriter logWriter;

    @Override
    public Object createConnectionFactory(ConnectionManager connectionManager) throws ResourceException {
        return new BitrixConnectionFactoryImpl(enabled, webhookUrl, responsibleId, HttpClient.newHttpClient());
    }

    @Override
    public Object createConnectionFactory() throws ResourceException {
        return new BitrixConnectionFactoryImpl(enabled, webhookUrl, responsibleId, HttpClient.newHttpClient());
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo)
            throws ResourceException {
        return new BitrixManagedConnection(enabled, webhookUrl, responsibleId, HttpClient.newHttpClient());
    }

    @Override
    public ManagedConnection matchManagedConnections(Set connectionSet,
                                                     Subject subject,
                                                     ConnectionRequestInfo connectionRequestInfo) {
        for (Object connection : connectionSet) {
            if (connection instanceof BitrixManagedConnection managedConnection) {
                return managedConnection;
            }
        }
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter logWriter) {
        this.logWriter = logWriter;
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public long getResponsibleId() {
        return responsibleId;
    }

    public void setResponsibleId(long responsibleId) {
        this.responsibleId = responsibleId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, webhookUrl, responsibleId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof BitrixManagedConnectionFactory other)) {
            return false;
        }
        return enabled == other.enabled
                && responsibleId == other.responsibleId
                && Objects.equals(webhookUrl, other.webhookUrl);
    }
}
