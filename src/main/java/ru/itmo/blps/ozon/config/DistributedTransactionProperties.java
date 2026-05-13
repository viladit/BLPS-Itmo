package ru.itmo.blps.ozon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tx")
public class DistributedTransactionProperties {

    private String vendor = "h2";
    private String objectStoreDir = "target/narayana-object-store";
    private final Database orders = new Database();
    private final Database notifications = new Database();

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getObjectStoreDir() {
        return objectStoreDir;
    }

    public void setObjectStoreDir(String objectStoreDir) {
        this.objectStoreDir = objectStoreDir;
    }

    public Database getOrders() {
        return orders;
    }

    public Database getNotifications() {
        return notifications;
    }

    public static class Database {
        private String uniqueName;
        private String url;
        private String username;
        private String password;
        private String jndiName;

        public String getUniqueName() {
            return uniqueName;
        }

        public void setUniqueName(String uniqueName) {
            this.uniqueName = uniqueName;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getJndiName() {
            return jndiName;
        }

        public void setJndiName(String jndiName) {
            this.jndiName = jndiName;
        }
    }
}
