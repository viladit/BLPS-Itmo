# Проверка приложения

## 1. Запустить WildFly + RabbitMQ + две PostgreSQL БД + сервис уведомлений через Docker

Основной демонстрационный запуск для лабораторной:

```bash
docker compose up --build
```

Compose поднимает:
- `orders-db` на локальном порту `15432`
- `notifications-db` на локальном порту `15433`
- `rabbitmq` на портах `5672` и `15672`
- `app` на `http://localhost:8080`
- `notifications-service` на `http://localhost:8081`

Приложение деплоится в WildFly как `ROOT.war`. WildFly настраивает две XA datasource:
- `java:/PostgresDS` для основной БД заказов
- `java:/NotificationDS` для БД уведомлений

Обе PostgreSQL БД запускаются с `max_prepared_transactions=100`, иначе PostgreSQL не позволит XA/two-phase commit.

Основной фронт для ручной проверки и toast-уведомлений доступен по адресу:

```bash
http://localhost:8080/
```

Фронт сервиса уведомлений доступен отдельно как вспомогательная страница:

```bash
http://localhost:8081/
```

## 2. Проверить, что API отвечает

```bash
curl -u manager:manager123 http://localhost:8080/api/orders
```

Ожидается JSON-массив, например `[]`, если заказов пока нет.

Если не передать учётные данные, API должен вернуть `401`.

Тестовые пользователи при Docker-запуске создаются автоматически:
- `manager` / `manager123`
- `warehouse` / `warehouse123`
- `delivery` / `delivery123`
- `admin` / `admin123`

## 3. Проверить создание заказа и асинхронное уведомление через RabbitMQ/JMS

```bash
export API_USERNAME=manager
export API_PASSWORD=manager123
./scripts/curl/create_order.sh
./scripts/curl/list_orders.sh
```

Ожидаемое поведение:
- в `list_orders` появляется новый заказ
- приложение OZON публикует сообщение в RabbitMQ queue `order.status.queue`
- `notifications-service` получает сообщение через JMS listener, пишет запись в свою БД и отправляет WebSocket-событие
- на основном фронте `http://localhost:8080/` появляется всплывающее toast-уведомление справа снизу

Для прямой проверки двух БД:

```bash
docker compose exec -T orders-db psql -U orders -d orders -c "select count(*) from orders;"
docker compose exec -T notifications-db psql -U notifications -d notifications -c "select count(*) from notification_history;"
```

RabbitMQ management UI: `http://localhost:15672/`, логин и пароль: `guest` / `guest`.

Чтобы доказать, что сообщение реально проходит через очередь:

```bash
docker compose stop notifications-service
./scripts/curl/create_order.sh
docker compose exec -T rabbitmq rabbitmqctl list_queues name messages_ready messages_unacknowledged messages consumers
docker compose start notifications-service
sleep 5
docker compose exec -T notifications-db psql -U notifications -d notifications -c "select id, order_id, status, message, sent_at from notification_history order by id desc limit 5;"
```

При остановленном consumer у очереди `order.status.queue` должно быть `messages_ready=1`. После запуска `notifications-service` очередь должна опустеть, а в `notification_history` должна появиться запись.

## 4. Проверить rollback распределённой транзакции

### Управляемый сбой после записи уведомления

Скрипт вызывает `POST /api/orders?failNotification=true`: сервис уведомлений успевает выполнить insert, затем бросает исключение. JTA должна откатить и заказ, и уведомление.

```bash
export API_USERNAME=manager
export API_PASSWORD=manager123
./scripts/curl/create_order_notification_failure.sh
./scripts/curl/list_orders.sh
./scripts/curl/list_notifications.sh
```

Ожидается `503`, а количество заказов и уведомлений не должно увеличиться.

### Реальное падение БД уведомлений

Скрипт останавливает контейнер `notifications-db`, пытается создать заказ, затем поднимает БД обратно и показывает состояние.

```bash
./scripts/curl/run_notification_db_down_rollback.sh
```

Ожидается `503` на создании заказа. После восстановления БД количество заказов остаётся прежним.

## 5. Проверить текущий happy path

Из корня проекта:

```bash
chmod +x scripts/curl/*.sh
./scripts/curl/run_happy_path.sh
```

`run_happy_path.sh` сам переключает роли:
- `manager` для `create` и `accept`
- `warehouse` для `pack`
- `delivery` для `handoff` и `deliver`
- `manager` для итогового `get`

При необходимости можно переопределить учётки и пароли переменными:

```bash
export API_MANAGER_USERNAME=manager
export API_MANAGER_PASSWORD=manager123
export API_WAREHOUSE_USERNAME=warehouse
export API_WAREHOUSE_PASSWORD=warehouse123
export API_DELIVERY_USERNAME=delivery
export API_DELIVERY_PASSWORD=delivery123
./scripts/curl/run_happy_path.sh
```

Текущий happy path в этой ветке:
- создать заказ
- принять заказ
- упаковать заказ
- передать в доставку
- завершить доставку
- получить итоговое состояние заказа
- вывести список уведомлений

Важно: старые шаги вроде `check-stock`, `reserve`, `confirm`, `picked` и `picking-task` больше не используются в curl-сценарии, потому что текущая модель процесса упрощена.

## 6. Проверить scheduled-сценарий ожидающих заказов

В Docker-режиме значения уже настроены для быстрой демонстрации:
- заказ считается ожидающим через `10 секунд`
- scheduler запускается каждые `15 секунд`
- повторное уведомление для ожидающего заказа уходит каждые `15 секунд`
- Bitrix-задача создаётся только один раз на заказ

После создания заказа можно дождаться автоматического scheduler или запустить проверку вручную:

```bash
export API_USERNAME=manager
export API_PASSWORD=manager123
./scripts/curl/create_order.sh
sleep 11
./scripts/curl/list_pending_orders.sh
./scripts/curl/run_pending_reminders.sh
```

Ожидаемое поведение:
- `GET /api/orders?status=CREATED` показывает заказ в ожидании подтверждения
- `POST /api/orders/pending-reminders/run` возвращает количество обработанных заказов
- в RabbitMQ и сервис уведомлений уходит напоминание, а на основном фронте появляется toast
- JCA-адаптер Bitrix24 создаёт CRM-задачу, если задан `APP_EIS_BITRIX_WEBHOOK_URL`; без webhook он пишет в лог no-op сообщение

Для живого Bitrix24 webhook в этой версии можно использовать старый метод `task.item.add`, если в интерфейсе портала нет `tasks.task.add`:

```bash
APP_EIS_BITRIX_ENABLED=true \
APP_EIS_BITRIX_WEBHOOK_URL='https://example.bitrix24.ru/rest/USER_ID/WEBHOOK_TOKEN/task.item.add.json' \
APP_EIS_BITRIX_RESPONSIBLE_ID=1 \
docker compose up --build
```

Webhook-токен секретный; не коммитьте настоящий URL и после демонстрации лучше перегенерируйте webhook.

## 7. Проверить сценарий отмены

Сначала создайте заказ:

```bash
export API_USERNAME=manager
export API_PASSWORD=manager123
./scripts/curl/create_order.sh
```

Потом отмените заказ:

```bash
export API_USERNAME=manager
export API_PASSWORD=manager123
./scripts/curl/cancel_order.sh 1
```

Вместо `1` подставьте реальный `orderId`.

Ожидаемое поведение:
- отмена работает для заказа до передачи в доставку
- после `IN_DELIVERY` и `DELIVERED` сервис должен вернуть ошибку `400`

## 8. Проверить отдельные эндпоинты вручную

Доступные curl-скрипты:

```bash
export API_USERNAME=manager
export API_PASSWORD=manager123
./scripts/curl/list_orders.sh
./scripts/curl/list_pending_orders.sh
./scripts/curl/list_notifications.sh
./scripts/curl/run_pending_reminders.sh
./scripts/curl/get_order.sh 1
./scripts/curl/accept_order.sh 1
./scripts/curl/cancel_order.sh 1
```

Для операций склада:

```bash
export API_USERNAME=warehouse
export API_PASSWORD=warehouse123
./scripts/curl/pack_order.sh 1
```

Для операций доставки:

```bash
export API_USERNAME=delivery
export API_PASSWORD=delivery123
./scripts/curl/handoff_to_delivery.sh 1
./scripts/curl/mark_delivered.sh 1
```

## 9. Проверить автотесты

```bash
mvn test
```

Сейчас в проекте есть:
- сервисные тесты на переходы статусов
- controller tests с `MockMvc`
- integration tests на security, роли и HTTP Basic

Тестовая база:
- H2 в режиме совместимости с PostgreSQL
- конфиг: `src/test/resources/application-test.properties`

## 10. Что проверять на защите

- `docker compose up --build` стартует WildFly, RabbitMQ, две PostgreSQL БД и отдельный сервис уведомлений
- в логах WildFly видно `Using JTA platform [org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform]`
- в логах WildFly видно bind datasource `java:/PostgresDS` и `java:/NotificationDS`
- таблицы создаются автоматически
- создание заказа создаёт запись в основной БД и отправляет JMS-сообщение в RabbitMQ
- сервис уведомлений получает JMS-сообщение, пишет в свою БД и отправляет событие на основной фронт как toast
- `@Scheduled` обрабатывает заказы `CREATED`, которые ожидают менеджера дольше порога, и повторяет уведомления
- интеграция с Bitrix24 сделана через JCA-адаптер; для живого Bitrix24 нужен webhook URL
- сбой уведомления или остановка `notifications-db` откатывает создание заказа
- happy path проходит по текущим статусам: `CREATED -> ACCEPTED -> PACKED -> IN_DELIVERY -> DELIVERED`
- отмена после передачи в доставку запрещена
- невалидные переходы возвращают `400`
- несуществующий заказ возвращает `404`

## 11. Старый запуск через SSH-туннель

Если Docker не используется, можно подключаться к учебной БД через локальный порт `5434`:

```bash
ssh -p 2222 -L 5434:localhost:5432 s368925@helios.cs.ifmo.ru
```

Для полноценной демонстрации распределённой транзакции всё равно нужна вторая БД уведомлений, поэтому предпочтительный вариант проверки — Docker Compose.
