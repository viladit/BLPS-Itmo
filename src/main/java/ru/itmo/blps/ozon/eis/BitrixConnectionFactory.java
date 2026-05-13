package ru.itmo.blps.ozon.eis;

import jakarta.resource.ResourceException;

public interface BitrixConnectionFactory {

    BitrixConnection getConnection() throws ResourceException;
}
