# Проверка приложения

## 1. Поднять SSH-туннель до учебного сервера

По умолчанию приложение подключается к PostgreSQL через локальный порт `5434`, который нужно пробросить на `helios`.

Рекомендуемый вариант:

```bash
ssh -p 2222 -L 5434:localhost:5432 s368925@helios.cs.ifmo.ru
```

Если хотите использовать другой локальный порт, например `15432`:

```bash
ssh -p 2222 -L 15432:localhost:5432 s368925@helios.cs.ifmo.ru
export DB_PORT=15432
```

## 2. Запустить приложение

Вариант через Maven:

```bash
export APP_SECURITY_SEED_ENABLED=true
mvn spring-boot:run
```

Вариант через jar:

```bash
export APP_SECURITY_SEED_ENABLED=true
mvn clean package
java -jar target/ozon-seller-backend-0.0.1-SNAPSHOT.jar
```

Что важно:
- приложение слушает `http://localhost:8080`
- таблицы создаются Hibernate автоматически через `spring.jpa.hibernate.ddl-auto=update`
- `schema.sql` не используется

## 3. Проверить, что API отвечает

```bash
curl -u manager:manager123 http://localhost:8080/api/orders
```

Ожидается JSON-массив, например `[]`, если заказов пока нет.

Если не передать учётные данные, API должен вернуть `401`.

Тестовые пользователи по умолчанию:
- `manager` / `manager123`
- `warehouse` / `warehouse123`
- `delivery` / `delivery123`
- `admin` / `admin123`

Bootstrap тестовых учёток включается только если задано:
- `APP_SECURITY_SEED_ENABLED=true`

Пароли можно переопределить через:
- `APP_SECURITY_SEED_ENABLED`
- `APP_SECURITY_MANAGER_PASSWORD`
- `APP_SECURITY_WAREHOUSE_PASSWORD`
- `APP_SECURITY_DELIVERY_PASSWORD`
- `APP_SECURITY_ADMIN_PASSWORD`

## 4. Проверить текущий happy path

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

Важно: старые шаги вроде `check-stock`, `reserve`, `confirm`, `picked` и `picking-task` больше не используются в curl-сценарии, потому что текущая модель процесса упрощена.

## 5. Проверить сценарий отмены

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

## 6. Проверить отдельные эндпоинты вручную

Доступные curl-скрипты:

```bash
export API_USERNAME=manager
export API_PASSWORD=manager123
./scripts/curl/list_orders.sh
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

## 7. Проверить автотесты

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

## 8. Что проверять на защите

- туннель до `helios` поднят
- приложение стартует без ошибок подключения к БД
- таблицы создаются автоматически
- happy path проходит по текущим статусам: `CREATED -> ACCEPTED -> PACKED -> IN_DELIVERY -> DELIVERED`
- отмена после передачи в доставку запрещена
- невалидные переходы возвращают `400`
- несуществующий заказ возвращает `404`
