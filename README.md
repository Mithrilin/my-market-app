# My Market App

Мультимодульное реактивное приложение интернет-магазина: витрина с каталогом товаров, корзиной и оформлением заказов + отдельный RESTful-сервис платежей.

---

## Структура проекта

````
my-market-app/                     (parent pom, packaging=pom)
  ├── openapi/
  │    └── payment-api.yaml        (общая OpenAPI-спецификация payment-service)
  ├── keycloak/
  │    └── realm-export.json       (realm market, клиенты market-app и payment-service)
  ├── pom.xml
  ├── docker-compose.yaml
  ├── market-app/                  (основное веб-приложение)
  │    ├── pom.xml
  │    ├── Dockerfile
  │    └── src/...
  └── payment-service/             (RESTful-сервис платежей)
       ├── pom.xml
       ├── Dockerfile
       └── src/...
````

---

## Технологический стек

### Общее
- **Java 21** — основной язык
- **Spring Boot 3.4.13** — фреймворк
- **Spring WebFlux** — реактивный веб-стек (Project Reactor, Netty)
- **Spring Validation** — валидация входных данных
- **Lombok** — сокращение шаблонного кода
- **Maven** — система сборки (мультимодульная)
- **Docker / Docker Compose** — контейнеризация и локальный запуск
- **Keycloak** — OAuth2-провайдер (realm `market`)

### market-app
- **Spring Data R2DBC** — реактивный доступ к PostgreSQL
- **R2DBC PostgreSQL Driver** — драйвер
- **PostgreSQL** — БД каталога и заказов
- **Thymeleaf (reactive)** — шаблонизатор HTML
- **Spring Cache + Spring Data Redis (reactive)** — кеш товаров в Redis
- **Redis** — кеш карточек, списков и корзины
- **Spring Security + OAuth2 Client** — форма логина для пользователей и `client_credentials` для вызова payment-service
- **MapStruct** — маппинг объектов
- **openapi-generator-maven-plugin** — генерация реактивного клиента payment-service из `openapi/payment-api.yaml`

### payment-service
- **Spring WebFlux** — реактивные REST-эндпоинты
- **Spring Validation** — валидация `PaymentRequest`
- **Spring Data R2DBC + PostgreSQL** — хранение балансов пользователей (`accounts`)
- **Spring Security OAuth2 Resource Server (JWT)** — проверка JWT от Keycloak, доступ по роли `SERVICE` в `resource_access.market-app.roles`
- **openapi-generator-maven-plugin** — генерация REST-интерфейса и DTO из `openapi/payment-api.yaml`
- **springdoc-openapi (Swagger UI)** — просмотр спецификации: `http://localhost:8082/swagger-ui.html`
- JSON-обмен

### Тесты
- **Reactor Test** — `StepVerifier` для реактивных потоков
- **Testcontainers** — интеграционные тесты с PostgreSQL (в обоих модулях, через `r2dbc:tc:` URL и `application-test.yaml`); 
в `payment-service` также `PaymentSecurityIntegrationTest` с **реальным Keycloak** (`dasniko/testcontainers-keycloak`, `KeycloakContainer` + импорт realm `market`)
- **Mockito** — unit-тесты сервисов
- **`@WebFluxTest` / `WebTestClient`** — тесты контроллеров
- **`@DataR2dbcTest`** — тесты репозиториев
- **`@SpringBootTest`** — тесты контроллеров payment-service с полным контекстом (нужен из-за маппингов на сгенерированном интерфейсе `PaymentsApi`)
- **Конкурентные тесты** — `PaymentServiceIntegrationTest#pay_shouldHandleConcurrentPaymentsWithRowLock` проверяет `SELECT ... FOR UPDATE` при параллельных списаниях

---

## Функциональные возможности

### Витрина (`market-app`)

#### Управление товарами
- Просмотр списка товаров с пагинацией (по умолчанию 5 на страницу)
- Фильтрация по названию и описанию
- Сортировка: по алфавиту (A-Z), по цене (возр.), без сортировки
- Просмотр детальной карточки товара
- Добавление/удаление товаров в корзину

#### Корзина
- Просмотр товаров в корзине
- Увеличение/уменьшение количества (`PLUS`/`MINUS`)
- Удаление из корзины (`DELETE` — обнуление `count`)
- Подсчёт суммы
- **Проверка возможности оплаты**: кнопка «Купить» активна, только если баланс в payment-service ≥ суммы корзины и сервис доступен. Иначе — сообщение и заблокированная кнопка.

#### Оформление заказов
- Платёж через payment-service (до сохранения позиций заказа)
- При успехе — создание заказа, обнуление корзины, переход на страницу заказа
- При отказе/недоступности платежа — заказ **не создаётся**, показывается ошибка (`400`/`503` через `ErrorHandler`)
- Просмотр списка заказов и детальной страницы заказа

#### Кеширование в Redis (`market-app`)

- **Карточка товара** (`item`): кешируется `ItemDto` по ключу `id:userId`
- **Список товаров** (`itemList`): кешируется `ItemListResponse` по ключу `sort:search:pageNumber:pageSize:userId`
- **Корзина** (`cart`): кешируется `CartDto` (товары + сумма) по ключу `userId` — `CartCacheService`
- **TTL** — 3 минуты (`CacheConfig.CACHE_TTL`)
- **Инвалидация** (`@CacheEvict(allEntries = true)` для кэшей `item`, `itemList`, `cart`) при:
  - изменении количества товара со страницы товара / списка (`changeItemCountFromItemPage`)
  - изменении количества в корзине (`changeItemCountFromCartPage`)
  - сохранении нового заказа (`OrderPersistenceService.saveNewOrder`)
- Сериализация — `GenericJackson2JsonRedisSerializer`
- `null`-значения не кешируются (`disableCachingNullValues`)
- **Статус оплаты не кешируется**: `paymentAvailable`/`paymentMessage` зависят от текущего баланса и доступности 
payment-service и запрашиваются на каждый `GET /cart/items`. `CartDto` (товары + сумма) кешируется отдельно, `PaymentStatusDto` (статус оплаты) — нет.

### Сервис платежей (`payment-service`)
- `GET /api/payments/balance` — текущий баланс
- `POST /api/payments` — списание суммы по заказу
- Баланс хранится в PostgreSQL (таблица `accounts`), списание — в транзакции с `SELECT ... FOR UPDATE`
- Начальные балансы задаются через `data.sql` (для пользователей 1 и 2 — по 200000)
- При недостатке средств — `400 Bad Request` с JSON-ошибкой
- Валидация `amount > 0`, `userId > 0`
- Доступ только с JWT, содержащим роль `SERVICE` в claim `resource_access.market-app.roles`

### Обработка ошибок
- **`market-app`**: `@ControllerAdvice` (`ErrorHandler`), рендерит `error.html`
  - `404` — `NotFoundException`
  - `400` — `BadRequestException`, `ConstraintViolationException`, `WebExchangeBindException`, `MissingRequestValueException`, `ServerWebInputException`
  - `403` — `ForbiddenException`
  - `503` — `PaymentServiceException` (недоступность payment-service)
  - `500` — прочие исключения
- **`payment-service`**: `@RestControllerAdvice` (`ErrorHandler`), JSON-ошибки
  - `400` — `InsufficientFundsException`, `WebExchangeBindException`
  - `403` — `AccessDeniedException`
  - `500` — прочие исключения

---

## API Endpoints

### market-app

| Метод | Endpoint | Описание | Параметры |
|-------|----------|----------|-----------|
| GET | `/` или `/items` | Список товаров | `search`, `sort` (NO/ALPHA/PRICE), `pageNumber`, `pageSize` |
| GET | `/items/{id}` | Карточка товара | `id` |
| POST | `/items/{id}` | Изменить количество со страницы товара | `id`, `action` (PLUS/MINUS/DELETE) |
| POST | `/items` | Изменить количество со списка | `id`, `action`, `search`, `sort`, `pageNumber`, `pageSize` |
| GET | `/cart/items` | Корзина | — |
| POST | `/cart/items` | Изменить количество в корзине | `id`, `action` |
| GET | `/orders` | Список заказов | — |
| GET | `/orders/{id}` | Заказ по ID | `id`, `newOrder` (boolean, default false) |
| POST | `/buy` | Оформить заказ | — |

Доступ: `GET /`, `/items`, `/items/{id}` — публично; остальное — после логина (`/login`).

### payment-service

| Метод | Endpoint | Описание | Тело / ответ |
|-------|----------|----------|--------------|
| GET | `/api/payments/balance?userId=1` | Баланс | → `{"balance": 200000}` |
| POST | `/api/payments` | Списание | `{"userId":1,"amount":850}` → `{"userId":1,"amount":850,"newBalance":199150,"status":"SUCCESS"}`; при недостатке — `400` с `{"errorMessage":"...","balance":null}` |

Оба эндпоинта требуют JWT с ролью `SERVICE` (OAuth2 Resource Server, Keycloak realm `market`).  
Swagger UI: `http://localhost:8082/swagger-ui.html`  
Спецификация: `http://localhost:8082/openapi/payment-api.yaml`

---

## Аутентификация и авторизация

- **Keycloak** (realm `market`) импортируется из `keycloak/realm-export.json` при старте контейнера.
- **`market-app`**:
  - пользователи логинятся через форму (`/login`), данные — в таблице `users` (`user`/`admin`, пароль `password`, BCrypt);
  - для вызова `payment-service` используется OAuth2-клиент `market-app` (confidential, `client_credentials`, secret `market-secret`), у service account есть роль `SERVICE`.
- **`payment-service`**:
  - настроен как OAuth2 Resource Server (`issuer-uri` Keycloak);
  - кастомный `NestedResourceAccessRolesConverter` (обёрнут в `ReactiveJwtAuthenticationConverterAdapter`) читает роль из 
  вложенного claim `resource_access.market-app.roles`, поэтому в токене от `market-app` приходит authority `SERVICE`;
  - на методах `PaymentController` дополнительно стоит `@PreAuthorize("hasAuthority('SERVICE')")` (включён `@EnableReactiveMethodSecurity`);
  - `@PreAuthorize("hasAuthority('SERVICE')")` на методах `PaymentController`;
  - В тестовом `payment-service/src/test/resources/keycloak/realm-export.json` дополнительно заведён клиент `no-service-client` 
  (без роли `SERVICE`) — для проверки ответа `403` в `PaymentSecurityIntegrationTest`.

---

## Установка и запуск

### Требования
- Java 21+
- Maven 3.8+
- Docker и Docker Compose

### Запуск через Docker Compose (рекомендуется)

1. **Соберите jar-файлы** (Dockerfile ожидает готовые `target/*.jar`):
```bash
mvn clean package -DskipTests
```
2. **Запустите приложение с помощью Docker Compose:**
```bash
docker compose up --build
```
3. **Сервисы будут доступны:**
```bash
- витрина (market-app): http://localhost:8081
- payment-service:       http://localhost:8082
- Keycloak:              http://localhost:8080 (admin/admin)
- PostgreSQL market-bd:  localhost:5432
- PostgreSQL payment-bd: localhost:5433
- Redis:                 localhost:6379
```
4. **Для остановки приложения:**
```bash
docker compose down
```

### Переменные окружения (Docker Compose)
market-app:
```bash
- SPRING_R2DBC_URL=r2dbc:postgresql://market-bd:5432/market-bd
- SPRING_R2DBC_USERNAME=postgres
- SPRING_R2DBC_PASSWORD=postgres
- SPRING_DATA_REDIS_HOST=redis
- SPRING_DATA_REDIS_PORT=6379
- PAYMENT_SERVICE_URL=http://payment-service:8082
- PAYMENT_SERVICE_TIMEOUT_MS=2000
- SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MARKET_APP_CLIENT_ID=market-app
- SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MARKET_APP_CLIENT_SECRET=market-secret
- SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_MARKET_KEYCLOAK_TOKEN_URI=http://keycloak:8080/realms/market/protocol/openid-connect/token
```
payment-service:
```bash
- SPRING_R2DBC_URL=r2dbc:postgresql://payment-bd:5432/payment-bd
- SPRING_R2DBC_USERNAME=postgres
- SPRING_R2DBC_PASSWORD=postgres
- KEYCLOAK_ISSUER_URI=http://keycloak:8080/realms/market
```

### Локальный запуск

1. **PostgreSQL:**  
   Создайте базы данных:
```bash
CREATE DATABASE "market-bd";
CREATE DATABASE "payment-bd";
```
2. **Redis**  
   запустить на
```bash
localhost:6379
```
3. **Keycloak:**  
   проще всего поднять из compose (порт 8080) — он импортирует realm `market`:
```bash
docker compose up keycloak
```
4. **payment-service** (по умолчанию слушает 8082, ждёт PostgreSQL на `localhost:5432/payment-bd` и Keycloak на `localhost:8080`):
```bash
mvn -pl payment-service spring-boot:run
```
> Если `payment-bd` поднят из compose, снаружи он на `localhost:5433` — переопределите `SPRING_R2DBC_URL=r2dbc:postgresql://localhost:5433/payment-bd`.
5. **market-app** (по умолчанию слушает 8081, ждёт PostgreSQL на `localhost:5432/market-bd`, Redis на `localhost:6379`, payment-service на `localhost:8082`, Keycloak на `localhost:8080`):
```bash
mvn -pl market-app spring-boot:run
```

## Особенности реализации

### Оплата
- `OrderServiceImpl.createOrder()` (без `@Transactional`):
  1. читает корзину, считает сумму;
  2. **вызывает `payment-service`** (`POST /api/payments` с `{userId, amount}`) — вне транзакции;
  3. при успехе — передаёт управление `OrderPersistenceService.saveNewOrder(...)`, где в **короткой** `@Transactional` сохраняются `Order`, `order_items` и обнуляется корзина;
  4. при отказе (`4xx` от payment-service) — `BadRequestException` (HTTP `400`), при недоступности (`5xx`/сеть) — `PaymentServiceException` (HTTP `503`). В обоих случаях в БД ничего не пишется.
- Списание в `payment-service` — отдельная транзакция (`SELECT ... FOR UPDATE` + `UPDATE balance`), компенсации при сбое сохранения заказа в `market-app` нет.

### Кеш и `count`
- **Карточка товара** (`item`): кешируется `ItemDto` по ключу `id`
- **Список товаров** (`itemList`): кешируется `ItemListResponse` по ключу `sort:search:pageNumber:pageSize`
- **TTL** — 3 минуты (`CacheConfig.CACHE_TTL`)
- **Инвалидация** (`@CacheEvict(allEntries = true)`) при:
  - изменении количества товара со страницы товара / списка (`changeItemCountFromItemPage`)
  - изменении количества в корзине (`changeItemCountFromCartPage`)
  - сохранении нового заказа (`OrderPersistenceService.saveNewOrder`)
- Сериализация — `GenericJackson2JsonRedisSerializer`
- `null`-значения не кешируются (`disableCachingNullValues`)
- **Корзина не кешируется**: `paymentAvailable`/`paymentMessage` зависят от текущего баланса и доступности payment-service, поэтому запрашиваются на каждый `GET /cart/items`. `CartDto` (товары + сумма) и `PaymentStatusDto` (статус оплаты) разделены — данные корзины при необходимости можно кешировать отдельно от UI-состояния оплаты.

### Таймауты вызова payment-service
- `WebClient` настроен на `connect-timeout` и `response-timeout` из `payment.service.timeout-ms` (по умолчанию 2000 мс).
- При недоступности payment-service кнопка «Купить» на странице корзины блокируется, показывается сообщение.

### Особенности OpenAPI-клиента
- Сгенерированный `ApiClient` (`library=webclient`) строит URI от **своего** поля `basePath`, а не от `WebClient.baseUrl`. Поэтому в `PaymentClientImpl` явно вызывается `apiClient.setBasePath(properties.baseUrl())`, а `WebClient` конфигурируется только транспортом (таймауты, коннектор, OAuth2-фильтр).
- `baseUrl` проверяется на `null`/blank на старте — если переменная `PAYMENT_SERVICE_URL` не задана, приложение падает с понятным сообщением.
- Ошибки клиента разделены: `4xx` → `BadRequestException` (`400`), `401/403` и `5xx`/сеть → `PaymentServiceException` (`503`).

## Структура базы данных

### `market-app` (БД `market-bd`)
- `users` — пользователи формы логина (`id`, `username`, `password`, `role`)
- `items` — товары каталога (`id`, `title`, `description`, `img_path`, `price`)
- `cart_items` — корзина (`user_id`, `item_id`, `count`, PK `(user_id, item_id)`)
- `orders` — заказы (`id`, `user_id`, `total_sum`)
- `order_items` — позиции заказа (`id`, `order_id`, `title`, `price`, `count`)

### `payment-service` (БД `payment-bd`)
- `accounts` — балансы пользователей (`user_id BIGINT PRIMARY KEY`, `balance BIGINT NOT NULL`)
