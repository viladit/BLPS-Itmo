package ru.itmo.blps.ozon.config;

import com.arjuna.ats.jdbc.TransactionalDriver;
import java.io.File;
import java.util.Properties;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.postgresql.xa.PGXADataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
@Profile("!wildfly")
@EnableConfigurationProperties(DistributedTransactionProperties.class)
public class NarayanaJtaConfig {

    @Bean
    @Primary
    public DataSource dataSource(DistributedTransactionProperties properties) {
        return transactionalDataSource(
                properties.getVendor(),
                properties.getOrders()
        );
    }

    @Bean
    public DataSource notificationDataSource(DistributedTransactionProperties properties) {
        return transactionalDataSource(
                properties.getVendor(),
                properties.getNotifications()
        );
    }

    @Bean
    public JdbcTemplate notificationJdbcTemplate(@Qualifier("notificationDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @Primary
    public JtaTransactionManager transactionManager(DistributedTransactionProperties properties) {
        configureObjectStore(properties);
        return new JtaTransactionManager(
                com.arjuna.ats.jta.UserTransaction.userTransaction(),
                com.arjuna.ats.jta.TransactionManager.transactionManager()
        );
    }

    private DataSource transactionalDataSource(String vendor, DistributedTransactionProperties.Database database) {
        XADataSource xaDataSource = xaDataSource(vendor, database);
        Properties driverProperties = new Properties();
        driverProperties.put(TransactionalDriver.XADataSource, xaDataSource);
        driverProperties.setProperty(TransactionalDriver.poolConnections, "true");
        driverProperties.setProperty(TransactionalDriver.maxConnections, "10");

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(TransactionalDriver.class.getName());
        dataSource.setUrl("jdbc:arjuna:" + database.getUniqueName());
        dataSource.setConnectionProperties(driverProperties);
        return dataSource;
    }

    private XADataSource xaDataSource(String vendor, DistributedTransactionProperties.Database database) {
        if ("postgresql".equalsIgnoreCase(vendor)) {
            PGXADataSource dataSource = new PGXADataSource();
            dataSource.setUrl(database.getUrl());
            dataSource.setUser(database.getUsername());
            dataSource.setPassword(database.getPassword());
            return dataSource;
        }

        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(database.getUrl());
        dataSource.setUser(database.getUsername());
        dataSource.setPassword(database.getPassword());
        return dataSource;
    }

    private void configureObjectStore(DistributedTransactionProperties properties) {
        File objectStore = new File(properties.getObjectStoreDir());
        if (!objectStore.exists()) {
            objectStore.mkdirs();
        }
        System.setProperty("ObjectStoreEnvironmentBean.objectStoreDir", objectStore.getAbsolutePath());
        System.setProperty("CoordinatorEnvironmentBean.transactionStatusManagerEnable", "false");
        System.setProperty("RecoveryEnvironmentBean.recoveryListener", "false");
    }
}
