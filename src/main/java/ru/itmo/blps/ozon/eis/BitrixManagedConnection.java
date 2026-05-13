package ru.itmo.blps.ozon.eis;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;
import java.io.PrintWriter;
import java.net.http.HttpClient;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

class BitrixManagedConnection implements ManagedConnection {

    private final boolean enabled;
    private final String webhookUrl;
    private final long responsibleId;
    private final HttpClient httpClient;
    private PrintWriter logWriter;

    BitrixManagedConnection(boolean enabled, String webhookUrl, long responsibleId, HttpClient httpClient) {
        this.enabled = enabled;
        this.webhookUrl = webhookUrl;
        this.responsibleId = responsibleId;
        this.httpClient = httpClient;
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
        return new BitrixConnectionImpl(enabled, webhookUrl, responsibleId, httpClient);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void associateConnection(Object connection) throws ResourceException {
        if (!(connection instanceof BitrixConnection)) {
            throw new ResourceException("Unsupported Bitrix connection handle");
        }
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
    }

    @Override
    public XAResource getXAResource() {
        return null;
    }

    @Override
    public LocalTransaction getLocalTransaction() {
        return null;
    }

    @Override
    public ManagedConnectionMetaData getMetaData() {
        return new BitrixManagedConnectionMetaData();
    }

    @Override
    public void setLogWriter(PrintWriter logWriter) {
        this.logWriter = logWriter;
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }
}
