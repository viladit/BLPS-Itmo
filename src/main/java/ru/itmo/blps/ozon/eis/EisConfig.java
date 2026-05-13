package ru.itmo.blps.ozon.eis;

import jakarta.resource.ResourceException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BitrixProperties.class)
public class EisConfig {

    @Bean
    public BitrixManagedConnectionFactory bitrixManagedConnectionFactory(BitrixProperties properties) {
        BitrixManagedConnectionFactory factory = new BitrixManagedConnectionFactory();
        factory.setEnabled(properties.isEnabled());
        factory.setWebhookUrl(properties.getWebhookUrl());
        factory.setResponsibleId(properties.getResponsibleId());
        return factory;
    }

    @Bean
    public BitrixConnectionFactory bitrixConnectionFactory(BitrixManagedConnectionFactory managedConnectionFactory)
            throws ResourceException {
        return (BitrixConnectionFactory) managedConnectionFactory.createConnectionFactory(new BitrixConnectionManager());
    }
}
