package ru.itmo.blps.ozon.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
@Profile("wildfly")
@EnableConfigurationProperties(DistributedTransactionProperties.class)
public class WildFlyDataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(DistributedTransactionProperties properties) {
        return jndiDataSource(properties.getOrders().getJndiName());
    }

    @Bean
    public DataSource notificationDataSource(DistributedTransactionProperties properties) {
        return jndiDataSource(properties.getNotifications().getJndiName());
    }

    @Bean
    public JdbcTemplate notificationJdbcTemplate(@Qualifier("notificationDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public JtaTransactionManager transactionManager() {
        return new JtaTransactionManager();
    }

    private DataSource jndiDataSource(String jndiName) {
        JndiDataSourceLookup lookup = new JndiDataSourceLookup();
        return lookup.getDataSource(jndiName);
    }
}
