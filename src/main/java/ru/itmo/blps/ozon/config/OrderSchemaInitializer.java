package ru.itmo.blps.ozon.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public OrderSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                alter table if exists orders
                add column if not exists pending_eis_task_created boolean not null default false
                """);
    }
}
