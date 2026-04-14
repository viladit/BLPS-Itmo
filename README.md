# OZON Seller Backend

Учебный Spring Boot backend для seller-side процесса OZON:
создание заказа, обработка продавцом, упаковка, передача в доставку и завершение доставки.

## Текущее состояние проекта

Проект начинался с более детальной модели процесса, близкой к BPMN-схеме из задания. В текущей ветке процесс в коде упрощён до ключевых контрольных состояний:
- `CREATED`
- `ACCEPTED`
- `PACKED`
- `IN_DELIVERY`
- `DELIVERED`
- `CANCELLED`

Это важно:
- в BPMN можно оставлять 10 этапов как детальное бизнес-описание
- в коде сейчас хранится более компактная и практичная модель состояний

## Архитектура

Пакеты:
- `controller` - REST API
- `service` - бизнес-правила
- `repository` - доступ к данным
- `entity` - JPA-сущности
- `dto` - DTO для API
- `exception` - обработка ошибок
- `config` - конфигурация Spring

Основной поток в коде:
1. `POST /api/orders` - создать заказ
2. `POST /api/orders/{id}/accept` - принять заказ
3. `POST /api/orders/{id}/pack` - упаковать заказ
4. `POST /api/orders/{id}/handoff` - передать в доставку
5. `POST /api/orders/{id}/deliver` - завершить доставку

Дополнительно:
- `GET /api/orders` - список заказов
- `GET /api/orders/{id}` - заказ по id
- `POST /api/orders/{id}/cancel` - отмена заказа

## Безопасность

REST API защищён через Spring Security и HTTP Basic. Доступ к операциям разграничен по привилегиям, которые выдаются ролям:

- `ROLE_MANAGER` - `ORDER_CREATE`, `ORDER_READ`, `ORDER_ACCEPT`, `ORDER_CANCEL`
- `ROLE_WAREHOUSE` - `ORDER_READ`, `ORDER_PACK`
- `ROLE_DELIVERY` - `ORDER_READ`, `ORDER_HANDOFF`, `ORDER_DELIVER`
- `ROLE_ADMIN` - все привилегии

Для локальной ручной проверки можно включить bootstrap учёток, ролей и привилегий через:

- `APP_SECURITY_SEED_ENABLED=true`

После этого приложение создаст недостающие тестовые пользователи:

- `manager` / `manager123`
- `warehouse` / `warehouse123`
- `delivery` / `delivery123`
- `admin` / `admin123`

Пароли можно переопределить переменными окружения:

- `APP_SECURITY_SEED_ENABLED`
- `APP_SECURITY_MANAGER_PASSWORD`
- `APP_SECURITY_WAREHOUSE_PASSWORD`
- `APP_SECURITY_DELIVERY_PASSWORD`
- `APP_SECURITY_ADMIN_PASSWORD`

## Стек
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

## Конфигурация БД

Приложение по умолчанию ожидает PostgreSQL через локальный SSH-туннель до учебного сервера.

См.:
- [`application.properties`](/Users/meow4-hi/IdeaProjects/BLPS-Itmo/src/main/resources/application.properties)
- [`TESTING.md`](/Users/meow4-hi/IdeaProjects/BLPS-Itmo/TESTING.md)

Ключевые моменты:
- таблицы создаются Hibernate автоматически: `spring.jpa.hibernate.ddl-auto=update`
- `schema.sql` не используется
- тесты используют H2 в PostgreSQL-совместимом режиме

## Сборка

```bash
mvn clean package
```

Готовый jar:
- [`target/ozon-seller-backend-0.0.1-SNAPSHOT.jar`](/Users/meow4-hi/IdeaProjects/BLPS-Itmo/target/ozon-seller-backend-0.0.1-SNAPSHOT.jar)

Запуск:

```bash
java -jar target/ozon-seller-backend-0.0.1-SNAPSHOT.jar
```

## Тестирование

Автотесты:

```bash
mvn test
```

Ручные проверки:
- curl-скрипты лежат в `scripts/curl`
- основной сценарий: `scripts/curl/run_happy_path.sh` c разными ролями для manager/warehouse/delivery
- для авторизации скриптов используйте `API_USERNAME` и `API_PASSWORD`

Полная инструкция:
- [`TESTING.md`](/Users/meow4-hi/IdeaProjects/BLPS-Itmo/TESTING.md)

## Что важно помнить при доработках

- Не ориентироваться вслепую на старую формулировку про 10 технических статусов в коде
- Перед изменениями смотреть текущие endpoint'ы и тесты
- При изменении fetch-логики репозитория перепроверять маппинг DTO в сервисе
- Проект должен оставаться простым и подходящим для университетской лабораторной работы
