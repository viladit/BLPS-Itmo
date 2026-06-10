package ru.itmo.blps.ozon.bpm;

import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.spring.boot.starter.configuration.CamundaProcessEngineConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamundaEngineConfig {

    @Bean
    @ConditionalOnProperty(name = "app.camunda.skip-isolation-level-check", havingValue = "true")
    public CamundaProcessEngineConfiguration skipIsolationLevelCheckConfiguration() {
        return new CamundaProcessEngineConfiguration() {
            @Override
            public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
                processEngineConfiguration.setSkipIsolationLevelCheck(true);
            }
        };
    }
}
