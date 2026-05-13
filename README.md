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

## Архитектура лабораторной 3

Пакеты:
- `controller` - REST API
- `service` - бизнес-правила
- `repository` - доступ к данным
- `entity` - JPA-сущности
- `dto` - DTO для API
- `exception` - обработка ошибок
- `config` - конфигурация Spring
- `notification` - сервис уведомлений и доступ ко второй БД
- `eis` - JCA-адаптер интеграции с внешней EIS Bitrix24

Целевая схема для лабораторной 3:
1. `app` - OZON seller backend: пишет заказы в `orders-db`, отправляет события статусов в RabbitMQ через JMS API
2. `orders-db` - PostgreSQL с заказами
3. `rabbitmq` - очередь сообщений RabbitMQ, JMS provider через RabbitMQ JMS client
4. `notifications-service` - отдельный сервис из `services/blps_notifications` (исходник коллеги из `https://github.com/3epa/blps_notifications`), получает сообщения через JMS API, пишет историю уведомлений и отправляет WebSocket-события на свой фронт
5. `notifications-db` - PostgreSQL сервиса уведомлений

В упрощённой модели статусов состояние `CREATED` трактуется как “заказ ожидает подтверждения менеджером”.
Для таких заказов добавлен scheduled-сценарий: `@Scheduled` периодически ищет старые `CREATED` заказы, отправляет напоминание в RabbitMQ и создаёт задачу во внешней EIS. В Docker-режиме порог ожидания — 10 секунд, повтор уведомлений — каждые 15 секунд.

Основной фронт OZON (`http://localhost:8080/`) подключается к WebSocket endpoint сервиса уведомлений и показывает входящие события как всплывающие toast-уведомления. Отдельная страница сервиса уведомлений на `http://localhost:8081/` остаётся вспомогательной, но для демонстрации уже не обязательна.

EIS выбрана как Bitrix24 CRM: для seller-side процесса это естественный вариант, потому что зависший заказ превращается в CRM-задачу менеджеру. Интеграция сделана через небольшой JCA-адаптер (`BitrixManagedConnectionFactory`, `BitrixConnectionFactory`, `BitrixConnection`). Если `APP_EIS_BITRIX_WEBHOOK_URL` не задан, адаптер работает в демонстрационном no-op режиме и не ломает локальный запуск.

Основной поток в коде:
1. `POST /api/orders` - создать заказ
2. `POST /api/orders/{id}/accept` - принять заказ
3. `POST /api/orders/{id}/pack` - упаковать заказ
4. `POST /api/orders/{id}/handoff` - передать в доставку
5. `POST /api/orders/{id}/deliver` - завершить доставку

Дополнительно:
- `GET /api/orders` - список заказов
- `GET /api/orders?status=CREATED` - список заказов в ожидании подтверждения
- `GET /api/orders/{id}` - заказ по id
- `POST /api/orders/{id}/cancel` - отмена заказа
- `POST /api/orders/pending-reminders/run` - вручную запустить scheduled-сценарий напоминаний
- `GET /api/notifications` - уведомления, записанные во второй БД

## Асинхронные уведомления через RabbitMQ / JMS

При создании заказа и изменении статуса приложение публикует DTO `OrderStatusNotification` в очередь `order.status.queue`.
Формат совместим с сервисом уведомлений коллеги:

- `orderId`
- `customerName`
- `newStatus`
- `message`

Публикация выполняется после успешного коммита основной транзакции, поэтому откатившийся заказ не порождает внешнее уведомление.
`notifications-service` пересылает сообщения в WebSocket-топики `/topic/orders/{orderId}` и общий `/topic/orders`; основной фронт слушает общий топик.
Локально отправка включается переменной:

```bash
APP_NOTIFICATIONS_JMS_ENABLED=true
```

## Распределённая транзакция

Создание заказа теперь объединяет две взаимозависимые операции в одной декларативной транзакции:

1. запись заказа и товаров через JPA в основную БД `orders`
2. запись события `ORDER_CREATED` через `NotificationService` в отдельную БД `notifications`

Метод `OrderService.createOrder(...)` помечен `@Transactional(rollbackFor = Exception.class)`, поэтому обе XA datasource участвуют в одной JTA-транзакции. В WildFly профиль `wildfly` использует Jakarta EE transaction manager сервера приложений и JNDI datasource:

- `java:/PostgresDS` - основная БД заказов
- `java:/NotificationDS` - БД уведомлений

Для локальных тестов вне WildFly есть standalone-конфигурация Narayana, но основной демонстрационный запуск для лабораторной — Docker Compose с WildFly.

Этот сценарий оставлен как отдельная демонстрация распределённой транзакции из предыдущей ветки. Основной сценарий лабораторной 3 для уведомлений теперь асинхронный: OZON не пишет напрямую в БД сервиса уведомлений, а публикует сообщение в RabbitMQ.

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
- WildFly
- Jakarta EE JTA / Spring JTA
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
- Docker Compose поднимает две PostgreSQL БД с `max_prepared_transactions=100`, потому что XA/two-phase commit в PostgreSQL требует prepared transactions

## Docker / WildFly

Самый простой локальный запуск:

```bash
docker compose up --build
```

После старта:

- основной фронт и toast-уведомления: `http://localhost:8080/`
- вспомогательный фронт сервиса уведомлений: `http://localhost:8081/`
- RabbitMQ management: `http://localhost:15672/` (`guest` / `guest`)
- API: `http://localhost:8080/api/orders`
- логин для создания заказа: `manager` / `manager123`

Dockerfile собирает WAR профилем `wildfly`, добавляет PostgreSQL JDBC driver в WildFly module, настраивает две XA datasource через `docker/wildfly/configure-datasources.cli` и деплоит приложение как `ROOT.war`.

## Сборка WAR

```bash
mvn -P wildfly clean package
```

Готовый WAR:
- [`target/ozon-seller-backend-0.0.1-SNAPSHOT.war`](/Users/meow4-hi/IdeaProjects/BLPS-Itmo/target/ozon-seller-backend-0.0.1-SNAPSHOT.war)

## Тестирование

Автотесты:

```bash
mvn test
```

Ручные проверки:
- curl-скрипты лежат в `scripts/curl`
- основной сценарий: `scripts/curl/run_happy_path.sh` c разными ролями для manager/warehouse/delivery
- проверка rollback при ошибке уведомлений: `scripts/curl/create_order_notification_failure.sh`
- проверка rollback при остановленной БД уведомлений: `scripts/curl/run_notification_db_down_rollback.sh`
- для авторизации скриптов используйте `API_USERNAME` и `API_PASSWORD`

Полная инструкция:
- [`TESTING.md`](/Users/meow4-hi/IdeaProjects/BLPS-Itmo/TESTING.md)

## Что важно помнить при доработках

- Не ориентироваться вслепую на старую формулировку про 10 технических статусов в коде
- Перед изменениями смотреть текущие endpoint'ы и тесты
- При изменении fetch-логики репозитория перепроверять маппинг DTO в сервисе
- Проект должен оставаться простым и подходящим для университетской лабораторной работы
