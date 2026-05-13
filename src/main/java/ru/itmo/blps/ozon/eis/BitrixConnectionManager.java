package ru.itmo.blps.ozon.eis;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;
import java.io.Serial;

class BitrixConnectionManager implements ConnectionManager {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public Object allocateConnection(ManagedConnectionFactory managedConnectionFactory,
                                     ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
        ManagedConnection managedConnection = managedConnectionFactory.createManagedConnection(null, connectionRequestInfo);
        return managedConnection.getConnection(null, connectionRequestInfo);
    }
}
