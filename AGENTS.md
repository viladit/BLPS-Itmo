# Project instructions

## Stack
- Java 21
- Spring Boot 3.3.x
- Maven
- PostgreSQL
- Spring Web
- Spring Data JPA
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

### Main packages
- `controller` - REST endpoints
- `service` - business rules and status transitions
- `repository` - Spring Data JPA repositories
- `entity` - JPA entities
- `dto` - request/response DTOs
- `exception` - domain exceptions and REST error handling
- `config` - Spring beans like `Clock`

### Main entry points
- Application: `src/main/java/ru/itmo/blps/ozon/OzonSellerApplication.java`
- REST controller: `src/main/java/ru/itmo/blps/ozon/controller/OrderController.java`
- Main business logic: `src/main/java/ru/itmo/blps/ozon/service/OrderService.java`
- Persistence: `src/main/java/ru/itmo/blps/ozon/repository/OrderRepository.java`

## REST API actually present in this branch
- `POST /api/orders`
- `GET /api/orders/{orderId}`
- `GET /api/orders`
- `POST /api/orders/{orderId}/accept`
- `POST /api/orders/{orderId}/pack`
- `POST /api/orders/{orderId}/handoff`
- `POST /api/orders/{orderId}/deliver`
- `POST /api/orders/{orderId}/cancel`

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
- Do NOT add Docker unless explicitly asked
- Use curl examples for API testing
- Keep the project simple and readable

## Testing

### Automated tests
Current tests:
- `src/test/java/ru/itmo/blps/ozon/service/OrderServiceTest.java`
- `src/test/java/ru/itmo/blps/ozon/controller/OrderControllerTest.java`

Run:
```bash
mvn test
```

### Manual API testing
curl scripts are in `scripts/curl`:
- `create_order.sh`
- `list_orders.sh`
- `get_order.sh`
- `accept_order.sh`
- `pack_order.sh`
- `handoff_to_delivery.sh`
- `mark_delivered.sh`
- `cancel_order.sh`
- `run_happy_path.sh`

`run_happy_path.sh` reflects the current simplified process:
create -> accept -> pack -> handoff -> deliver -> get order

## Environment and database
- Main config: `src/main/resources/application.properties`
- Test config: `src/test/resources/application-test.properties`

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

## Important context for future agents
- The BPMN description may still mention 10 business stages; that is intentional for documentation/theory
- The codebase currently implements a shorter state machine
- Documentation in old commits/messages may mention endpoints and statuses that no longer exist
- Before changing statuses or endpoints, verify both tests and scripts in `scripts/curl`
