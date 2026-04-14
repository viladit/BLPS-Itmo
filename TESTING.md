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
mvn spring-boot:run
```

Вариант через jar:

```bash
mvn clean package
java -jar target/ozon-seller-backend-0.0.1-SNAPSHOT.jar
```

Что важно:
- приложение слушает `http://localhost:8080`
- таблицы создаются Hibernate автоматически через `spring.jpa.hibernate.ddl-auto=update`
- `schema.sql` не используется

## 3. Проверить, что API отвечает

```bash
curl http://localhost:8080/api/orders
```

Ожидается JSON-массив, например `[]`, если заказов пока нет.

## 4. Проверить текущий happy path

Из корня проекта:

```bash
chmod +x scripts/curl/*.sh
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
./scripts/curl/create_order.sh
```

Потом отмените заказ:

```bash
./scripts/curl/cancel_order.sh 1
```

Вместо `1` подставьте реальный `orderId`.

Ожидаемое поведение:
- отмена работает для заказа до передачи в доставку
- после `IN_DELIVERY` и `DELIVERED` сервис должен вернуть ошибку `400`

## 6. Проверить отдельные эндпоинты вручную

Доступные curl-скрипты:

```bash
./scripts/curl/list_orders.sh
./scripts/curl/get_order.sh 1
./scripts/curl/accept_order.sh 1
./scripts/curl/pack_order.sh 1
./scripts/curl/handoff_to_delivery.sh 1
./scripts/curl/mark_delivered.sh 1
./scripts/curl/cancel_order.sh 1
```

## 7. Проверить автотесты

```bash
mvn test
```

Сейчас в проекте есть:
- сервисные тесты на переходы статусов
- controller tests с `MockMvc`

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
