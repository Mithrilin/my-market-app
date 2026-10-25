# My Market App

Мультимодульное реактивное приложение интернет-магазина: витрина с каталогом товаров, корзиной и оформлением заказов + отдельный RESTful-сервис платежей.

---

## Структура проекта

````
my-market-app/                (parent pom, packaging=pom)  
  ├── openapi/
  │     └── payment-api.yaml  (общая OpenAPI-спецификация payment-service)
  ├── pom.xml  
  ├── docker-compose.yaml  
  ├── market-app/             (основное веб-приложение)  
  │     ├── pom.xml  
  │     ├── Dockerfile  
  │     └── src/...  
  └── payment-service/        (RESTful-сервис платежей)  
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

### market-app
- **Spring Data R2DBC** — реактивный доступ к PostgreSQL
- **R2DBC PostgreSQL Driver** — драйвер
- **PostgreSQL** — БД каталога и заказов
- **Thymeleaf (reactive)** — шаблонизатор HTML
- **Spring Cache + Spring Data Redis (reactive)** — кеш товаров в Redis
- **Redis** — кеш карточек, списков и корзины
- **MapStruct** — маппинг объектов
- **openapi-generator-maven-plugin** — генерация реактивного клиента payment-service из `openapi/payment-api.yaml`

### payment-service
- **Spring WebFlux** — реактивные REST-эндпоинты
- **Spring Validation** — валидация `PaymentRequest`
- **openapi-generator-maven-plugin** — генерация REST-интерфейса и DTO из `openapi/payment-api.yaml`
- **springdoc-openapi (Swagger UI)** — просмотр спецификации: `http://localhost:8081/swagger-ui.html`
- **In-memory баланс** (`AtomicLong`, инициализируется из конфига) — без БД
- JSON-обмен

### Тесты
- **Reactor Test** — `StepVerifier` для реактивных потоков
- **Testcontainers** — интеграционные тесты с PostgreSQL (в `market-app`)
- **Mockito** — unit-тесты сервисов
- **`@WebFluxTest` / `WebTestClient`** — тесты контроллеров
- **`@DataR2dbcTest`** — тесты репозиториев

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
- При отказе/недоступности платежа — заказ **не создаётся**, показывается ошибка (`400`/`500` через `ErrorHandler`)
- Просмотр списка заказов и детальной страницы заказа

#### Кеширование в Redis (`market-app`)
- **Карточка товара** (`item`): кешируется `ItemDto` по ключу `id`
- **Список товаров** (`itemList`): кешируется `ItemListResponse` по ключу `sort:search:pageNumber:pageSize`
- **TTL** — 3 минуты (`CacheConfig.CACHE_TTL`)
- **Инвалидация** (`@CacheEvict(allEntries = true)`) при:
  - изменении количества товара со страницы товара / списка (`changeItemCountFromItemPage`)
  - изменении количества в корзине (`changeItemCountFromCartPage`)
  - сохранении нового заказа (`OrderPersistenceService.saveNewOrder`)
- Сериализация — `GenericJackson2JsonRedisSerializer`
- `null`-значения не кешируются (`disableCachingNullValues`)
- **Корзина не кешируется**: `paymentAvailable`/`paymentMessage` зависят от текущего баланса и доступности payment-service, 
поэтому запрашиваются на каждый `GET /cart/items`. `CartDto` (товары + сумма) и `PaymentStatusDto` (статус оплаты) разделены — данные корзины при необходимости можно кешировать отдельно от UI-состояния оплаты.

### Сервис платежей (`payment-service`)
- `GET /api/payments/balance` — текущий баланс
- `POST /api/payments` — списание суммы по заказу
- Баланс изменяемый: `AtomicLong` с CAS-обновлением (безопасно при конкурентных запросах)
- Начальный баланс задаётся через `payment.initial-balance` (env `PAYMENT_INITIAL_BALANCE`)
- При недостатке средств — `400 Bad Request` с JSON-ошибкой
- Валидация `amount > 0`

### Обработка ошибок
- **`market-app`**: `@ControllerAdvice` (`ErrorHandler`), рендерит `error.html`
  - `404` — `NotFoundException`
  - `400` — `BadRequestException`, `ConstraintViolationException`, `WebExchangeBindException`, `MissingRequestValueException`, `ServerWebInputException`
  - `500` — прочие исключения
- **`payment-service`**: `@RestControllerAdvice` (`ErrorHandler`), JSON-ошибки
  - `400` — `InsufficientFundsException`, `WebExchangeBindException`
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

### payment-service

| Метод | Endpoint | Описание | Тело / ответ                                                                                                                                |
|-------|----------|----------|---------------------------------------------------------------------------------------------------------------------------------------------|
| GET | `/api/payments/balance` | Баланс | → `{"balance": 200000}`                                                                                                                     |
| POST | `/api/payments` | Списание | `{"amount": 850}` → `{"amount":850,"newBalance":9150,"status":"SUCCESS"}`; при недостатке — `400` с `{"errorMessage":"...","balance":null}` |
Swagger UI: `http://localhost:8081/swagger-ui.html`  
Спецификация: `http://localhost:8081/openapi/payment-api.yaml`

---

## Установка и запуск

### Требования
- Java 21+
- Maven 3.8+
- Docker и Docker Compose
- (опционально, для локального запуска без Docker) PostgreSQL 16+, Redis 7+

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
- витрина: http://localhost:8080
- payment-service: http://localhost:8081
- PostgreSQL: localhost:5432 (market-bd)
- Redis: localhost:6379
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
- PAYMENT_SERVICE_URL=http://payment-service:8081
- PAYMENT_SERVICE_TIMEOUT_MS=2000
```
payment-service:
```bash
- PAYMENT_INITIAL_BALANCE=200000
```

### Локальный запуск

1. **PostgreSQL:**  
Создайте базу данных:
```bash
CREATE DATABASE market-bd;
```
2. **Redis**  
   запустить на
```bash
localhost:6379
```
3. **payment-service:**
```bash
mvn -pl payment-service spring-boot:run
```
(слушает 8081, начальный баланс payment.initial-balance, по умолчанию 200000)
4. **market-app:**
```bash
mvn -pl market-app spring-boot:run
```
(слушает 8080, ждёт PostgreSQL на localhost:5432, Redis на localhost:6379, payment-service на localhost:8081)

## Особенности реализации

### Оплата
- `OrderServiceImpl.createOrder()` (без `@Transactional`):
  1. читает корзину, считает сумму;
  2. **вызывает `payment-service`** (`POST /api/payments` с `{amount}`) — вне транзакции;
  3. при успехе — передаёт управление `OrderPersistenceService.saveNewOrder(...)`, где в **короткой** `@Transactional` сохраняются `Order`, `order_items` и обнуляется корзина;
  4. при отказе (`4xx` от payment-service) — `BadRequestException` (HTTP `400`), при недоступности (`5xx`/сеть) — `PaymentServiceException` (HTTP `503`). В обоих случаях в БД ничего не пишется.

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
- Сгенерированный `ApiClient` (`library=webclient`) строит URI от **своего** поля `basePath`, а не от `WebClient.baseUrl`. Поэтому в `PaymentClientImpl` явно вызывается `apiClient.setBasePath(properties.baseUrl())`, а `WebClient` конфигурируется только транспортом (таймауты, коннектор).
- `baseUrl` проверяется на `null`/blank на старте — если переменная `PAYMENT_SERVICE_URL` не задана, приложение падает с понятным сообщением.
- Ошибки клиента разделены: `4xx` → `BadRequestException` (`400`), `5xx`/сеть → `PaymentServiceException` (`503`).

## Структура базы данных

### Таблица `items` (Товары)
Хранит информацию о товарах в каталоге магазина.

| Колонка | Тип | Ограничения | Описание |
|---------|-----|-------------|----------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Уникальный идентификатор товара |
| `title` | VARCHAR | NOT NULL | Название товара |
| `description` | VARCHAR | - | Описание товара |
| `img_path` | VARCHAR | - | Путь к изображению товара |
| `price` | BIGINT | NOT NULL | Цена товара |
| `count` | INTEGER | DEFAULT 0 | Количество товара в корзине |

### Таблица `orders` (Заказы)
Хранит информацию о заказах.

| Колонка | Тип | Ограничения | Описание |
|---------|-----|-------------|----------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Уникальный идентификатор заказа |
| `total_sum` | BIGINT | NOT NULL | Общая сумма заказа |

### Таблица `order_items` (Товары в заказе)
Хранит информацию о товарах в момент заказа.

| Колонка | Тип | Ограничения | Описание              |
|---------|-----|-------------|-----------------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Уникальный идентификатор |
| `order_id` | BIGINT | FOREIGN KEY → orders(id) | Идентификатор заказа  |
| `title` | VARCHAR | NOT NULL | Название товара       |
| `price` | BIGINT | NOT NULL | Цена товара   |
| `count` | INTEGER | DEFAULT 0 | Количество товара в заказе |