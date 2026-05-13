package ru.itmo.blps.ozon.eis;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ManagedConnectionMetaData;

class BitrixManagedConnectionMetaData implements ManagedConnectionMetaData {

    @Override
    public String getEISProductName() {
        return "Bitrix24 CRM";
    }

    @Override
    public String getEISProductVersion() {
        return "REST";
    }

    @Override
    public int getMaxConnections() {
        return 10;
    }

    @Override
    public String getUserName() throws ResourceException {
        return "webhook";
    }
}
