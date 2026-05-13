package ru.itmo.blps.ozon.eis;

import jakarta.resource.ResourceException;

public interface BitrixConnection extends AutoCloseable {

    void createManagerTask(BitrixTaskRequest request) throws ResourceException;

    @Override
    void close() throws ResourceException;
}
