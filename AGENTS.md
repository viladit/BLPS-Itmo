# Project instructions

## Stack
- Java 21
- Spring Boot 3.3.x
- Maven
- PostgreSQL
- WildFly
- Jakarta EE JTA / Spring JTA
- Spring Web
- Spring Data JPA
- Spring Security
- Bean Validation
- Lombok
- JUnit 5
- Spring Boot Test
- MockMvc

## Scope
Spring Boot backend for the OZON seller-side order workflow:
order creation, seller acceptance, packing, handoff to delivery, delivery completion, and cancellation.

The original university task mentions a 10-step business process for BPMN. In the current codebase the runtime model is intentionally simplified into a smaller set of persisted states. BPMN may stay detailed, but the code models only the key control points of the process.

## Current project state

As of 2026-05-13 the project contains the work for the first two labs plus the current distributed-transaction demo. The repository has many uncommitted changes from previous work; do not assume a clean git tree and do not revert unrelated files.

Lab 3 work adds asynchronous RabbitMQ/JMS notifications, scheduled reminders for orders waiting in `CREATED`, and a JCA-based Bitrix24 EIS adapter. The old two-database JTA rollback demo is still present, but the main lab 3 notification path is asynchronous: OZON publishes an `OrderStatusNotification` to RabbitMQ and the separate `blps_notifications` service consumes it.

### Business model implemented in code
Current order statuses in code:
- `CREATED`
- `ACCEPTED`
- `PACKED`
- `IN_DELIVERY`
- `DELIVERED`
- `CANCELLED`

Current happy path in code:
1. Create order
2. Accept order
3. Pack order
4. Hand off to delivery
5. Mark delivered

Cancellation is allowed before `IN_DELIVERY`.

If stock is unavailable during order creation, the service marks the order as cancelled and throws a business exception.

### Short lab summaries

Lab 1 summary:
- Implemented the simplified seller-side OZON order workflow as a Spring Boot REST backend.
- Main flow: create order -> accept -> pack -> hand off to delivery -> mark delivered.
- Added cancellation before delivery, validation of request DTOs, JPA entities/repositories, service-layer business rules, REST error handling, curl scripts, and unit/controller tests.
- The BPMN/theory can remain more detailed than the runtime code; the code intentionally persists only the key control states.

Lab 2 summary:
- Added security and role-based access for the workflow.
- API uses HTTP Basic with users stored in the database.
- Roles/privileges:
  - `ROLE_MANAGER`: `ORDER_CREATE`, `ORDER_READ`, `ORDER_ACCEPT`, `ORDER_CANCEL`
  - `ROLE_WAREHOUSE`: `ORDER_READ`, `ORDER_PACK`
  - `ROLE_DELIVERY`: `ORDER_READ`, `ORDER_HANDOFF`, `ORDER_DELIVER`
  - `ROLE_ADMIN`: all privileges
- Bootstrap users can be seeded for demo: `manager/manager123`, `warehouse/warehouse123`, `delivery/delivery123`, `admin/admin123`.
- Added integration tests for security, roles, and HTTP Basic access control.

### Distributed transaction demo

The project now demonstrates a distributed transaction between two PostgreSQL databases:
- `orders-db` stores orders and order items through JPA.
- `notifications-db` stores notification events through `NotificationService`/`NotificationEventRepository`.

`OrderService.createOrder(...)` is `@Transactional(rollbackFor = Exception.class)`.

Important current flow for `POST /api/orders?notificationPauseSeconds=10`:
1. The order is saved and flushed to the main orders DB with `orderRepository.saveAndFlush(order)`.
2. The service sleeps inside the still-open transaction for the requested number of seconds.
3. After the timer, the service writes/updates the notification in the second DB.
4. If `notifications-db` was stopped during the timer, the second DB operation fails and JTA rolls back the order from the first DB.

This is the intended defense/demo scenario: enable the frontend checkbox `10 секунд таймер`, click create order, stop `notifications-db` during the timer, then show that the order is not committed.

The old artificial rollback path still exists for scripts/tests:
- `POST /api/orders?failNotification=true`
- It throws `NotificationDeliveryException` after writing to the notifications flow and should roll back both resources.

PostgreSQL sequences/identity values are not transactional. A rolled-back insert may still consume an ID, but the order row itself must not remain visible in `/api/orders`.

### Main packages
- `controller` - REST endpoints
- `service` - business rules and status transitions
- `repository` - Spring Data JPA repositories
- `entity` - JPA entities
- `dto` - request/response DTOs
- `exception` - domain exceptions and REST error handling
- `config` - Spring beans, JTA datasource configuration, `Clock`
- `notification` - second database notification writes and reads
- `eis` - Bitrix24 JCA adapter for external EIS integration
- `security` - HTTP Basic, roles, privileges, seeded users
- `facade` - method-security boundary around service calls

### Main entry points
- Application: `src/main/java/ru/itmo/blps/ozon/OzonSellerApplication.java`
- REST controller: `src/main/java/ru/itmo/blps/ozon/controller/OrderController.java`
- Main business logic: `src/main/java/ru/itmo/blps/ozon/service/OrderService.java`
- Persistence: `src/main/java/ru/itmo/blps/ozon/repository/OrderRepository.java`
- Notifications: `src/main/java/ru/itmo/blps/ozon/notification/NotificationService.java`
- Security config: `src/main/java/ru/itmo/blps/ozon/security/SecurityConfig.java`
- Frontend demo: `src/main/resources/static/index.html` and `src/main/resources/static/app.js`
- Docker/WildFly: `docker-compose.yml`, `Dockerfile`, `docker/wildfly/configure-datasources.cli`

## REST API actually present in this branch
- `POST /api/orders`
  - optional query params:
    - `failNotification=true` for artificial rollback test
    - `notificationPauseSeconds=10` for real DB-down rollback demo
- `GET /api/orders/{orderId}`
- `GET /api/orders`
  - optional query param:
    - `status=CREATED` to list orders waiting for manager acceptance
- `POST /api/orders/{orderId}/accept`
- `POST /api/orders/{orderId}/pack`
- `POST /api/orders/{orderId}/handoff`
- `POST /api/orders/{orderId}/deliver`
- `POST /api/orders/{orderId}/cancel`
- `POST /api/orders/pending-reminders/run`
- `GET /api/notifications`

Do not assume older endpoints like `check-stock`, `reserve`, `confirm`, `picked`, or `picking-task` still exist. They were part of an earlier version and were removed when the process was simplified.

## Data access notes
- Hibernate schema generation is enabled via `spring.jpa.hibernate.ddl-auto=update`
- `schema.sql` is not used
- `spring.jpa.open-in-view=false`
- The current repository uses `@EntityGraph` for `findById` and list loading

This means:
- single-order reads eagerly load `items` and `delivery`
- list reads also eagerly load `items` and `delivery`

If you change repository fetching, re-check DTO mapping in `OrderService.toResponse(...)`, because it accesses `order.getItems()` and `order.getDelivery()`.

## Coding rules
- Keep controllers thin
- Keep business rules in the service layer
- Validate request DTOs
- Keep the code simple and suitable for a university lab
- Prefer constructor injection
- Before major changes, inspect the current branch first; this repository changed noticeably over time
- Do not assume old documentation is still correct without checking the code

## Hard constraints
- Do NOT add Flyway
- Do NOT add Testcontainers
- Do NOT add Insomnia files
- Docker/WildFly files already exist for the current distributed transaction demo; do not add unrelated Docker complexity unless explicitly asked
- Use curl examples for API testing
- Keep the project simple and readable

## Testing

### Automated tests
Current tests:
- `src/test/java/ru/itmo/blps/ozon/service/OrderServiceTest.java`
- `src/test/java/ru/itmo/blps/ozon/controller/OrderControllerTest.java`
- `src/test/java/ru/itmo/blps/ozon/security/DatabaseUserDetailsServiceTest.java`
- `src/test/java/ru/itmo/blps/ozon/security/OrderSecurityIntegrationTest.java`

Run:
```bash
mvn test
```

### Manual API testing
curl scripts are in `scripts/curl`:
- `create_order.sh`
- `create_order_notification_failure.sh`
- `list_orders.sh`
- `list_notifications.sh`
- `get_order.sh`
- `accept_order.sh`
- `pack_order.sh`
- `handoff_to_delivery.sh`
- `mark_delivered.sh`
- `cancel_order.sh`
- `run_happy_path.sh`
- `run_notification_db_down_rollback.sh`
- `list_pending_orders.sh`
- `run_pending_reminders.sh`

`run_happy_path.sh` reflects the current simplified process:
create -> accept -> pack -> handoff -> deliver -> get order

For the frontend distributed transaction demo:
1. Run `docker compose up --build`.
2. Open `http://localhost:8080/`.
3. Use `manager` / `manager123`.
4. Enable the checkbox `10 секунд таймер`.
5. Click create order.
6. During the 10-second timer run `docker compose stop notifications-db`.
7. The request should fail and `/api/orders` should not show the new order.
8. Restore the DB with `docker compose up -d notifications-db`.

## Environment and database
- Main config: `src/main/resources/application.properties`
- Test config: `src/test/resources/application-test.properties`
- Docker config: `src/main/resources/application-docker.properties`
- WildFly config: `src/main/resources/application-wildfly.properties`
- Local H2-style config: `src/main/resources/application-local.properties`

Default runtime database config points to a PostgreSQL database reached through a local SSH tunnel to the university server.

Default values:
- DB URL: `jdbc:postgresql://127.0.0.1:${DB_PORT:5434}/studs`
- username: `s368925`
- password is currently stored in `application.properties`

When working on infra/config, preserve support for overriding with:
- `DB_URL`
- `DB_PORT`
- `DB_USERNAME`
- `DB_PASSWORD`
- `ORDERS_DB_URL`
- `ORDERS_DB_PORT`
- `ORDERS_DB_USERNAME`
- `ORDERS_DB_PASSWORD`
- `NOTIFICATION_DB_URL`
- `NOTIFICATION_DB_PORT`
- `NOTIFICATION_DB_USERNAME`
- `NOTIFICATION_DB_PASSWORD`

For the Docker/WildFly demo:
- `orders-db` is exposed on host port `15432`
- `notifications-db` is exposed on host port `15433`
- `app` is exposed on host port `8080`
- both PostgreSQL containers use `max_prepared_transactions=100` for XA/two-phase commit
- WildFly JNDI datasources:
  - `java:/PostgresDS`
  - `java:/NotificationDS`

## Important context for future agents
- The BPMN description may still mention 10 business stages; that is intentional for documentation/theory
- The codebase currently implements a shorter state machine
- Documentation in old commits/messages may mention endpoints and statuses that no longer exist
- Before changing statuses or endpoints, verify both tests and scripts in `scripts/curl`
- If changing the distributed transaction demo, preserve the intended order: save/flush order in DB1 -> optional timer -> touch DB2 -> rollback DB1 if DB2 is unavailable
- If changing the frontend demo, remember that the checkbox must not send `failNotification=true`; it should send only `notificationPauseSeconds=10`
- After frontend/backend changes in Docker mode, rebuild/recreate the app with `docker compose up -d --build app` and hard-refresh the browser
