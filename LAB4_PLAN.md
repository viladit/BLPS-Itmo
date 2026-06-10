# План выполнения ЛР-4: миграция на Camunda BPM (embedded)

## ТЗ (вариант 52)

Переработать программу из ЛР-3:
1. Для управления бизнес-процессом использовать BPM-движок **Camunda**.
2. Заменить всю «статическую» бизнес-логику на «динамическую» на базе BPMS. Весь бизнес-процесс из ЛР 1–3 (разграничение доступа по ролям, управление транзакциями, асинхронная обработка, периодические задачи) должен быть **сохранён**.
3. BPM-движок встроен в веб-приложение (**embedded mode**).
4. Бизнес-процесс описывается в **Camunda Modeler** (BPMN 2.0).
5. UI приложения генерируется **генератором форм Camunda** (form fields в BPMN → Camunda Tasklist).
6. Итоговая сборка разворачивается на сервере **helios** под **Apache Tomcat** (WAR).

Правила: интегрировать в процесс всё, что возможно; для неподдерживаемого (JMS) — использовать API/адаптеры. Распределённые транзакции на движок переносить **не требуется** (оставить как есть в коде делегатов).

## Текущее состояние проекта (ЛР 1–3)

- **Стек**: Java 21, Spring Boot 3.3.1, Maven, packaging `war`, PostgreSQL ×2, RabbitMQ.
- **Процесс**: order fulfillment — `CREATED → ACCEPTED → PACKED → IN_DELIVERY → DELIVERED`, отмена возможна до передачи в доставку. Логика в `src/main/java/ru/itmo/blps/ozon/service/OrderService.java` (переходы статусов с валидацией).
- **Роли**: Spring Security, Basic Auth, `@PreAuthorize` по привилегиям в `OrderAccessFacade.java`. Роли: `ROLE_MANAGER`, `ROLE_WAREHOUSE`, `ROLE_DELIVERY`, `ROLE_ADMIN`; привилегии `ORDER_CREATE/READ/ACCEPT/PACK/HANDOFF/DELIVER/CANCEL`. Сидер: `SecurityDataSeeder.java`.
- **Транзакции (ЛР-2)**: JTA Narayana, 2PC между orders-db и notifications-db (`config/NarayanaJtaConfig.java`, XADataSource + TransactionalDriver).
- **Асинхронность (ЛР-3)**: JMS → RabbitMQ, очередь `order.status.queue` (`OrderNotificationService`, `JmsOrderNotificationSender`, `JmsConfig.java`); отдельный notifications-service (порт 8081) пишет историю и пушит в WebSocket STOMP.
- **Периодика**: `PendingOrderReminderService` — `@Scheduled`, ищет «зависшие» CREATED-заказы, создаёт задачу в Bitrix EIS, шлёт JMS-уведомление.
- **Frontend**: статический SPA (`src/main/resources/static/`) — подлежит замене на Camunda Tasklist.
- **Деплой**: сейчас WildFly через docker-compose; нужно перейти на Tomcat.

## Выбор версии Camunda

**Camunda 7** (Camunda Platform 7.21+ / community), а не Camunda 8: только C7 поддерживает embedded mode внутри Spring Boot приложения и генерацию форм (Generated Task Forms в Tasklist). Зависимости:

```xml
<dependency>
  <groupId>org.camunda.bpm.springboot</groupId>
  <artifactId>camunda-bpm-spring-boot-starter-webapp</artifactId>
  <version>7.22.0</version> <!-- совместима со Spring Boot 3.3 -->
</dependency>
<dependency>
  <groupId>org.camunda.bpm.springboot</groupId>
  <artifactId>camunda-bpm-spring-boot-starter-rest</artifactId>
  <version>7.22.0</version>
</dependency>
```

Проверить матрицу совместимости Camunda 7.x ↔ Spring Boot 3.3 (7.21/7.22 поддерживают Spring Boot 3.2/3.3). Таблицы движка Camunda — в orders-db (auto schema update `camunda.bpm.database.schema-update: true`).

## Шаг 1. Модель процесса в Camunda Modeler (BPMN 2.0)

Файл `src/main/resources/order-fulfillment.bpmn`, process id `orderFulfillment`. Структура:

1. **Start Event** «Заказ создан» + **Generated Form** (form fields: `customerName`, `deliveryAddress`, `items` — можно упростить до строковых полей/JSON; `failNotification`, `notificationPauseSeconds` — boolean/long для демо JTA).
2. **Service Task** «Сохранить заказ» → JavaDelegate `CreateOrderDelegate` (вызывает текущую логику `createOrder`: JTA-транзакция на две БД сохраняется здесь, внутри делегата — переносить 2PC на движок не требуется). Кладёт `orderId` в process variables.
3. **User Task** «Подтвердить заказ» — candidate group `MANAGER`, generated form (поле решения `approved` boolean, `cancellationReason` string). 
4. **Exclusive Gateway** «Подтверждён?» → ветка отмены (Service Task `CancelOrderDelegate` → End «Отменён») или дальше.
5. **Service Task** «Сменить статус → ACCEPTED» (`AcceptOrderDelegate`) + отправка JMS-уведомления.
6. **User Task** «Собрать заказ» — candidate group `WAREHOUSE` → **Service Task** `PackOrderDelegate` (статус PACKED + JMS).
7. **User Task** «Передать в доставку» — candidate group `DELIVERY`, форма: `carrierName`, `trackingNumber` → **Service Task** `HandToDeliveryDelegate` (создаёт `Delivery`, статус IN_DELIVERY + JMS).
8. **User Task** «Подтвердить доставку» — candidate group `DELIVERY` → **Service Task** `MarkDeliveredDelegate` (DELIVERED + JMS) → **End Event** «Заказ выполнен».
9. **Отмена до доставки**: на этапы 3–6 повесить **Event Subprocess** или **Boundary Message/Conditional Event** для отмены; проще — добавить в формы user task'ов поле/исход «отменить» и gateway после каждого. Выбрать один вариант и аргументировать в отчёте.
10. **Периодическое напоминание (замена @Scheduled)**: **Event Subprocess (non-interrupting Timer Start Event, cycle `R/PT15S`** или boundary non-interrupting timer на user task «Подтвердить заказ» с duration = pendingAge): Service Task `PendingReminderDelegate` — создание задачи в Bitrix EIS + JMS-уведомление (переиспользовать логику `PendingOrderReminderService`, сам `@Scheduled` удалить/отключить). Это ключевой пункт «периодические задачи перенесены в BPMS».

Все Service Task'и с флагом `camunda:asyncBefore="true"` там, где нужны границы транзакций движка и ретраи (job executor).

## Шаг 2. Делегаты (JavaDelegate) — «динамическая» логика

Пакет `ru.itmo.blps.ozon.bpm.delegate`. Каждый — Spring bean (`@Component("createOrderDelegate")`), в BPMN — `camunda:delegateExpression="${createOrderDelegate}"`. Делегаты тонкие: достают переменные процесса, вызывают существующие методы `OrderService`/`OrderNotificationService`. **Из `OrderService` убрать валидацию допустимых переходов как «статическую» оркестрацию** — порядок шагов теперь гарантирует BPMN (можно оставить как defensive-проверку, но в отчёте указать, что управление потоком перешло к движку).

JMS-отправка уведомлений: Camunda не поддерживает JMS из коробки → по правилам ТЗ интегрируем через адаптер — делегаты вызывают существующий `JmsOrderNotificationSender` (это нужно явно отметить в отчёте, п. «правила выполнения», §2).

## Шаг 3. Роли и доступ

1. Включить identity service Camunda: создать группы `MANAGER`, `WAREHOUSE`, `DELIVERY`, `ADMIN` и пользователей (manager/manager123 и т.д.) при старте (`@EventListener(PostDeployEvent)` или сидер через `IdentityService`), либо адаптировать `SecurityDataSeeder`.
2. На user task'ах — `candidateGroups`: задача видна в Tasklist только нужной роли → это и есть «разграничение доступа по ролям», перенесённое в BPMS.
3. Camunda authorization (`camunda.bpm.authorization.enabled=true`) — выдать группам права на процесс/задачи; admin-пользователь для Cockpit.
4. Старый Spring Security/`OrderAccessFacade`: REST-контроллеры либо удалить (UI теперь Tasklist), либо оставить read-only `GET /api/orders` для отчёта — решить по ходу; не должен конфликтовать с security webapp Camunda (настроить SecurityFilterChain, чтобы не перекрывал `/camunda/**`).

## Шаг 4. UI — генератор форм Camunda

Старый SPA (`static/index.html`, `app.js`, `app.css`) удалить/отключить. Все взаимодействия — через **Camunda Tasklist** (`/camunda/app/tasklist`):
- запуск процесса — Start Form (generated, form fields в Modeler: type string/long/boolean/enum, validation `required`);
- шаги — Generated Task Forms у user task'ов.
WebSocket-уведомления во встроенный Tasklist не встроить — notifications-service оставить как есть (история в БД), в отчёте указать, что real-time UI заменён Tasklist'ом.

## Шаг 5. Транзакции

- Движок Camunda использует datasource приложения (orders-db). Указать `camunda.bpm.datasource`/primary DS.
- JTA/Narayana остаётся внутри `CreateOrderDelegate` (orders-db + notifications-db, 2PC) — по правилу §3 распределённые транзакции на движок не переносим. Проверить сосуществование Narayana transaction manager и Camunda (Camunda Spring Boot starter использует `PlatformTransactionManager` — убедиться, что `JtaTransactionManager` корректно подхватывается, иначе изолировать: движок на обычном DS/`DataSourceTransactionManager`, делегат открывает JTA-транзакцию программно через `UserTransaction`). Это самый рискованный пункт — заложить время.
- Демо-сценарий отката (failNotification/pause) сохранить как переменные процесса.

## Шаг 6. Сборка WAR и деплой на helios (Tomcat)

1. Перейти с WildFly на Tomcat: packaging `war`, `spring-boot-starter-tomcat` → scope `provided`, `SpringBootServletInitializer` в main-классе. Narayana — версии «embedded in app» (убрать профиль wildfly/provided-scope для Narayana, вернуть compile).
2. Удалить/не использовать WildFly CLI-конфиги; datasource-параметры — через `application.yml` + переменные окружения.
3. На helios нет Docker: проверить доступность PostgreSQL и RabbitMQ на helios. Запасной вариант: H2 для Camunda/orders (file-based) и отключаемый JMS-профиль (`@Profile`), если RabbitMQ на helios недоступен — но сначала уточнить, что есть на сервере (обычно у студентов своя PostgreSQL-схема на helios). Подготовить Spring-профиль `helios`.
4. Деплой: `mvn package` → `~/<dir>/apache-tomcat/webapps/`, context path учитывать в URL Tasklist (`/<app>/camunda/app/tasklist`). Tomcat запускать на непривилегированном порту (например, 8080+смещение по варианту).
5. Локальная проверка перед helios: тот же WAR в локальном Tomcat 10.1 (Jakarta EE 10, совместим со Spring Boot 3.3).

## Шаг 7. Очистка «статической» логики

- Удалить `@Scheduled` из `PendingOrderReminderService` (логику оставить как сервис, вызываемый делегатом из timer event subprocess).
- Удалить/упростить REST endpoints переходов (`/accept`, `/pack`, ...) — переходы только через Tasklist.
- Удалить SPA-статику.
- `TransactionSynchronization.afterCommit` для JMS — пересмотреть: внутри делегата с `asyncBefore` коммит управляется движком; либо оставить, либо вынести JMS в отдельный async service task после смены статуса.

## Шаг 8. Проверка (сквозной сценарий)

1. Логин manager в Tasklist → Start process через start form → заказ в БД (CREATED), процесс виден в Cockpit.
2. Задача «Подтвердить заказ» видна только группе MANAGER; не подтверждать ~10с → срабатывает timer, создаётся EIS-задача + JMS-уведомление (проверить notifications-db).
3. Подтвердить → задача у WAREHOUSE → собрать → задача у DELIVERY → передать (форма с трек-номером) → доставить → процесс завершён, статусы и JMS-уведомления на каждом шаге.
4. Отмена: на этапе MANAGER выбрать «отменить» → процесс завершается, статус CANCELLED.
5. Демо JTA-отката: запуск с failNotification=true → откат обеих БД, инцидент в Cockpit (ретраи job executor).
6. Всё то же — на helios под Tomcat.

## Шаг 9. Материалы для отчёта (собирать по ходу)

1. Текст задания.
2. BPMN-диаграмма из Modeler (модель потока управления со всеми изменениями).
3. Блок-схема архитектуры: точки интеграции движка — delegates→OrderService/JTA, delegates→JMS-адаптер (RabbitMQ), identity service↔роли, timer↔Bitrix EIS, Tasklist↔пользователи.
4. Изменения в процессе относительно ЛР-3 с аргументацией: отмена как gateway/event subprocess (почему), периодика как timer (почему), удаление REST-переходов и SPA (почему), defensive-валидация.
5. Ссылка на репозиторий.
6. Выводы.

## Порядок работ (итерации)

1. Зависимости Camunda + запуск пустого движка локально (webapp доступен, admin создан).
2. BPMN happy path (без отмены и таймера) + делегаты + generated forms → сквозной прогон в Tasklist.
3. Роли: группы, candidate groups, authorization.
4. Таймер-подпроцесс напоминаний, удаление @Scheduled.
5. Отмена заказа в модели.
6. JTA-совместимость + демо отката.
7. Чистка статической логики и SPA.
8. WAR под Tomcat локально → деплой на helios.
9. Скриншоты/диаграммы для отчёта.

## Риски

- **Narayana + Camunda transaction manager** — возможны конфликты; запасной план в Шаге 5.
- **Версия Camunda ↔ Spring Boot 3.3** — сверить compatibility matrix перед стартом.
- **helios**: нет Docker → RabbitMQ может быть недоступен; нужен профиль с заглушкой/in-memory брокером (или ActiveMQ Artemis embedded как JMS-провайдер) — решить заранее.
- **Tomcat + Spring Boot WAR**: Tomcat должен быть 10.1+ (Jakarta), на helios проверить версию.
