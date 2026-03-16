# Project instructions

## Stack
- Java 21
- Spring Boot
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
Build a Spring Boot backend for the OZON seller-side business process:
order processing and handoff to delivery.

## Hard constraints
- Do NOT add Flyway
- Do NOT add Testcontainers
- Do NOT add Insomnia files
- Do NOT add Docker unless explicitly asked
- Use schema.sql for database schema if needed
- Use curl examples for API testing
- Keep the project simple and suitable for a university lab

## Architecture
Use package structure:
- controller
- service
- repository
- entity
- dto
- exception
- config

## Coding rules
- Keep controllers thin
- Put business rules in service layer
- Validate request DTOs
- Add meaningful exception handling
- Prefer simple readable code over overengineering
- Before major changes, inspect existing files and adapt to current project structure

## Testing
- Add unit tests for business status transitions
- Add controller tests with MockMvc when endpoints are added

## Process model
The business process has 10 stages:
1. Order received
2. Order contents loaded
3. Stock availability checked
4. Items reserved
5. Seller confirms order
6. Picking task created
7. Order picked
8. Order packed
9. Order handed to delivery
10. Order delivered and closed