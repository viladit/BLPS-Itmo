package ru.itmo.blps.ozon.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.seed")
public class SecuritySeedProperties {

    private boolean enabled = true;
    private String managerPassword = "manager123";
    private String warehousePassword = "warehouse123";
    private String deliveryPassword = "delivery123";
    private String adminPassword = "admin123";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getManagerPassword() {
        return managerPassword;
    }

    public void setManagerPassword(String managerPassword) {
        this.managerPassword = managerPassword;
    }

    public String getWarehousePassword() {
        return warehousePassword;
    }

    public void setWarehousePassword(String warehousePassword) {
        this.warehousePassword = warehousePassword;
    }

    public String getDeliveryPassword() {
        return deliveryPassword;
    }

    public void setDeliveryPassword(String deliveryPassword) {
        this.deliveryPassword = deliveryPassword;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }
}
