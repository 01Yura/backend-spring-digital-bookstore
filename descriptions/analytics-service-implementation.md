# Реализация Сервиса Аналитики и Метрик через Kafka

## Обзор

Сервис аналитики и метрик - это отдельный микросервис, который собирает события из основного приложения spring-digital-bookstore через Apache Kafka, агрегирует их в оперативной памяти и предоставляет API для получения статистики.

**Архитектура:**

- Основное приложение отправляет события в Kafka при различных действиях пользователей:
  - **BookViewEvent** - при просмотре книги через BookController (GET /api/v1/books/{bookId})
  - **BookDownloadEvent** - при скачивании книги через BookFileController (GET /api/v1/books/{bookId}/download)
  - **BookPurchaseEvent** - при покупке книги через StripeService (после успешной оплаты через Stripe)
  - **BookReviewEvent** - при создании/обновлении отзыва через ReviewController (POST/PUT /api/v1/books/{bookId}/reviews)
  - **BookRatingEvent** - при создании/обновлении рейтинга через RatingController (POST/PUT /api/v1/books/{bookId}/ratings)
- Микросервис аналитики подписывается на топики Kafka и обрабатывает события
- Данные статистики (BookStatistics, UserActivity, ReviewStatistics) хранятся в оперативной памяти микросервиса аналитики (HashMap, ConcurrentHashMap)
- Kafka UI предоставляет графический интерфейс для просмотра топиков, сообщений и партиций

---

## Основные понятия Kafka (для начинающих)

Перед тем как разбирать архитектуру, давайте разберем основные понятия Apache Kafka, которые используются в этом проекте.

### Что такое Kafka?

**Apache Kafka** - это распределенная платформа для обмена сообщениями (message broker). Представьте её как почтовую систему:

- **Producer (Отправитель)** - отправляет сообщения (события)
- **Topic (Тема/Топик)** - "почтовый ящик", куда отправляются сообщения определенного типа
- **Consumer (Получатель)** - читает сообщения из топика (может быть несколько разных consumers, читающих из одного топика, но в нашем проекте используется один - микросервис аналитики)
- **Broker** - сервер Kafka, который хранит и передает сообщения

### Основные концепции

#### 1. Topic (Топик)

**Топик** - это категория или канал, куда отправляются сообщения. В нашем проекте есть несколько топиков:

- `book.views` - события просмотра книг
- `book.downloads` - события скачивания книг
- `book.purchases` - события покупки книг
- `book.reviews` - события создания/обновления отзывов
- `book.ratings` - события создания/обновления рейтингов
- `analytics.aggregated-stats` - агрегированная статистика

**Аналогия:** Представьте, что топик - это отдельная газета. Есть газета "Просмотры книг", газета "Покупки" и т.д.

#### 2. Partition (Партиция)

**Партиция** - это способ разделить топик на несколько частей для параллельной обработки.

**Зачем нужны партиции?**

- **Масштабируемость:** Можно обрабатывать больше сообщений одновременно
- **Параллелизм:** Несколько consumers могут читать из разных партиций одновременно
- **Порядок:** Сообщения с одинаковым ключом всегда попадают в одну партицию

**Пример:**

```
Топик book.views (3 партиции):
┌─────────────┬─────────────┬─────────────┐
│ Partition 0 │ Partition 1 │ Partition 2 │
│             │             │             │
│ Event 1     │ Event 2     │ Event 3     │
│ Event 4     │ Event 5     │ Event 6     │
└─────────────┴─────────────┴─────────────┘
```

**Важно:** Сообщения с одинаковым ключом (например, `bookId=1`) всегда попадают в одну и ту же партицию. Это гарантирует порядок обработки событий для одной книги.

#### 3. Producer (Производитель)

**Producer** - это приложение, которое отправляет сообщения в топики Kafka.

В нашем проекте:

- **Основное приложение** (spring-digital-bookstore) является Producer
- Использует `KafkaProducerService` для отправки событий
- Отправляет события при действиях пользователей (просмотр, скачивание, покупка и т.д.)

**Как это работает:**

```java
// Когда пользователь просматривает книгу
BookViewEvent event = BookViewEvent.builder()
    .bookId(1)
    .userId(5)
    .bookTitle("Spring Boot Guide")
    .build();

// Отправляем в Kafka
kafkaProducerService.sendBookViewEvent(event);
// Сообщение попадает в топик book.views
```

#### 4. Consumer (Потребитель)

**Consumer** - это приложение, которое читает сообщения из топиков Kafka.

**Важно:** В одном проекте может быть несколько разных consumers, которые читают из одних и тех же топиков, но обрабатывают события по-разному. Например:

- **Consumer для аналитики** - собирает статистику (как в нашем проекте)
- **Consumer для уведомлений** - отправляет email/SMS при определенных событиях
- **Consumer для архивирования** - сохраняет все события в долгосрочное хранилище
- **Consumer для мониторинга** - отслеживает аномалии и ошибки
- **Consumer для рекомендаций** - анализирует поведение пользователей для рекомендательной системы

Каждый consumer может быть в своей Consumer Group, и все они будут получать одни и те же сообщения независимо друг от друга.

**В нашем проекте:**

- **Микросервис аналитики** является Consumer (единственный consumer в проекте)
- Подписывается на топики и обрабатывает события
- Обновляет статистику в памяти

**Как это работает:**

```java
@KafkaListener(topics = "book.views", groupId = "analytics-service-group")
public void consumeBookView(BookViewEvent event) {
    // Получили событие из Kafka
    // Обрабатываем его
    analyticsService.processBookView(event);
    // Обновляем статистику
}
```

#### 5. Consumer Group (Группа потребителей)

**Consumer Group** - это группа consumers, которые работают вместе для обработки сообщений из топика.

**Как это работает:**

- Все consumers в одной группе **делят между собой** партиции топика
- Каждое сообщение обрабатывается **только одним** consumer из группы
- Если в группе 3 consumers, а в топике 3 партиции, то каждый consumer обрабатывает свою партицию

**Пример:**

```
Топик book.views (3 партиции):
┌─────────────┬─────────────┬─────────────┐
│ Partition 0 │ Partition 1 │ Partition 2 │
└──────┬──────┴──────┬──────┴──────┬──────┘
       │             │             │
       ▼             ▼             ▼
   Consumer 1  Consumer 2  Consumer 3
   (в одной группе)
```

**Важно:** Если в группе больше consumers, чем партиций, лишние consumers будут простаивать.

#### 6. Offset (Смещение)

**Offset** - это номер сообщения в партиции. Kafka запоминает, какое сообщение уже прочитано каждым consumer.

**Зачем это нужно?**

- Если consumer упал и перезапустился, он продолжит с того места, где остановился
- Не потеряются сообщения

**Пример:**

```
Partition 0:
Offset 0: Event 1 ✅ (прочитано)
Offset 1: Event 2 ✅ (прочитано)
Offset 2: Event 3 ⏳ (следующее для чтения)
Offset 3: Event 4
Offset 4: Event 5
```

---

## Подробная механика работы (пошагово)

Теперь разберем, как все это работает вместе в нашем проекте.

### Шаг 1: Пользователь выполняет действие

**Пример:** Пользователь открывает страницу книги с ID=1

```
Пользователь → GET /api/v1/books/1
```

### Шаг 2: Основное приложение обрабатывает запрос

```java
// BookController.java
@GetMapping("/{bookId}")
public ResponseEntity<BookResponse> getBookById(@PathVariable Long bookId) {
    // 1. Получаем информацию о книге из БД
    BookResponse book = bookService.getBookById(bookId);

    // 2. Создаем событие
    BookViewEvent event = BookViewEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType("BOOK_VIEW")
        .timestamp(LocalDateTime.now())
        .bookId(1)  // ← Это будет ключом для партиционирования!
        .userId(5)
        .bookTitle("Spring Boot Guide")
        .bookGenre("TECHNOLOGY")
        .build();

    // 3. Отправляем в Kafka
    kafkaProducerService.sendBookViewEvent(event);

    // 4. Возвращаем ответ пользователю (не ждем обработки в Kafka!)
    return ResponseEntity.ok(book);
}
```

**Важно:** Отправка в Kafka происходит **асинхронно**. Пользователь получает ответ сразу, не дожидаясь обработки события.

### Шаг 3: Kafka получает и сохраняет событие

```
KafkaProducerService.sendBookViewEvent(event)
    ↓
Kafka определяет партицию на основе ключа (bookId=1)
    ↓
Сообщение сохраняется в топик book.views, Partition X
    ↓
Kafka возвращает подтверждение Producer'у
```

**Как определяется партиция?**

Kafka использует формулу: `partition = hash(key) % numberOfPartitions`

- Если `bookId = 1`, а партиций 3, то: `hash(1) % 3 = X`
- Все события с `bookId = 1` всегда попадут в одну и ту же партицию
- Это гарантирует, что события одной книги обрабатываются в правильном порядке

**Визуализация:**

```
Топик: book.views (3 партиции)

Partition 0:  [bookId=3] [bookId=6] [bookId=9]
Partition 1:  [bookId=1] [bookId=4] [bookId=7] ← Событие попало сюда
Partition 2:  [bookId=2] [bookId=5] [bookId=8]
```

### Шаг 4: Микросервис аналитики читает событие

```java
// BookViewConsumer.java
@KafkaListener(
    topics = "book.views",
    groupId = "analytics-service-group"  // ← Группа потребителей
)
public void consumeBookView(BookViewEvent event) {
    log.info("Received book view event: {}", event);

    // Обрабатываем событие
    analyticsService.processBookView(event);
}
```

**Что происходит:**

1. Kafka видит, что есть consumer в группе `analytics-service-group`
2. Kafka назначает этому consumer партиции для чтения
3. Consumer читает сообщения из назначенных партиций
4. Kafka обновляет offset (запоминает, что сообщение прочитано)

**Визуализация:**

```
Consumer Group: analytics-service-group

Partition 0 → Consumer 1 читает
Partition 1 → Consumer 2 читает ← Наше событие обрабатывается здесь
Partition 2 → Consumer 3 читает
```

### Шаг 5: Обработка события в микросервисе

```java
// AnalyticsService.java
public void processBookView(BookViewEvent event) {
    // 1. Получаем или создаем статистику для книги
    BookStatistics stats = bookStats.computeIfAbsent(
        event.getBookId(),
        id -> new BookStatistics(id, event.getBookTitle(), event.getBookGenre())
    );

    // 2. Обновляем счетчики
    stats.incrementViewCount();  // viewCount++
    stats.addUniqueViewer(event.getUserId());  // Добавляем пользователя в Set

    // 3. Обновляем временные метки
    stats.updateLastViewAt(event.getTimestamp());

    // 4. Обновляем активность пользователя
    if (event.getUserId() != null) {
        UserActivity activity = userActivity.computeIfAbsent(
            event.getUserId(),
            UserActivity::new
        );
        activity.incrementBooksViewed();
        activity.addViewedBook(event.getBookId());
    }
}
```

**Где хранятся данные?**

- В оперативной памяти (ConcurrentHashMap)
- Очень быстро, но теряются при перезапуске сервиса
- Для pet проекта это нормально

### Шаг 6: Агрегация и обратная отправка (каждую минуту)

Каждую минуту микросервис аналитики агрегирует накопленные данные и отправляет их обратно в основное приложение:

```java
// AnalyticsAggregationScheduler.java
@Scheduled(fixedRate = 60000) // Каждую 1 минуту
public void aggregateAndSendStatistics() {
    // 1. Агрегируем статистику по всем книгам
    Map<Long, BookStatistics> allBookStats = analyticsService.getAllBookStatistics();

    // 2. Отправляем каждую книгу в Kafka
    for (BookStatistics stats : allBookStats.values()) {
        BookStatisticsAggregated aggregated = convertToAggregated(stats);
        kafkaTemplate.send(
            "analytics.aggregated-stats",  // ← Топик для агрегированных данных
            "BOOK_STATS",                  // ← Ключ (тип агрегации)
            aggregated                     // ← Данные
        );
    }

    // 3. Отправляем общую статистику системы
    SystemOverviewAggregated systemOverview = analyticsService.getSystemOverview();
    kafkaTemplate.send("analytics.aggregated-stats", "SYSTEM_OVERVIEW", systemOverview);
}
```

**Визуализация:**

```
Микросервис аналитики (Producer)
    ↓
Отправляет агрегированные данные
    ↓
Топик: analytics.aggregated-stats
    ├─ Ключ: "BOOK_STATS" → BookStatisticsAggregated
    ├─ Ключ: "SYSTEM_OVERVIEW" → SystemOverviewAggregated
    └─ Ключ: "POPULAR_BOOKS" → PopularBooksAggregated
    ↓
Основное приложение (Consumer)
    ↓
Сохраняет в БД
```

### Шаг 7: Основное приложение получает агрегированные данные

```java
// AnalyticsStatsConsumer.java
@KafkaListener(
    topics = "analytics.aggregated-stats",
    groupId = "main-app-analytics-group"
)
public void consumeAggregatedStats(
        @Payload Map<String, Object> payload,
        @Header(KafkaHeaders.RECEIVED_KEY) String key) {

    switch (key) {
        case "BOOK_STATS":
            // Сохраняем статистику по книге в БД
            BookStatisticsAggregated bookStats = convert(payload);
            saveBookAnalytics(bookStats);
            break;

        case "SYSTEM_OVERVIEW":
            // Сохраняем общую статистику в БД
            SystemOverviewAggregated systemOverview = convert(payload);
            saveSystemAnalytics(systemOverview);
            break;
    }
}
```

**Результат:** Данные сохраняются в PostgreSQL и доступны через Admin API.

---

## Почему используется партиционирование?

### Пример без партиционирования (плохо):

```
Топик book.views (1 партиция):
┌─────────────────────────────────┐
│ Partition 0                     │
│ [bookId=1] [bookId=2] [bookId=1]│
│ [bookId=3] [bookId=1] [bookId=2]│
└─────────────────────────────────┘
         ↓
    Consumer 1
    (обрабатывает последовательно)
```

**Проблемы:**

- Медленно (все события обрабатываются одним consumer)
- Нет параллелизма
- При большом количестве событий - узкое место

### Пример с партиционированием (хорошо):

```
Топик book.views (3 партиции):
┌─────────────┬─────────────┬─────────────┐
│ Partition 0 │ Partition 1 │ Partition 2 │
│ [bookId=3]  │ [bookId=1]  │ [bookId=2]  │
│ [bookId=6]  │ [bookId=4]  │ [bookId=5]  │
│ [bookId=9]  │ [bookId=7]  │ [bookId=8]  │
└──────┬──────┴─────┬───────┴─────┬───────┘
       │            │             │
       ▼            ▼             ▼
   Consumer 1  Consumer 2  Consumer 3
   (обрабатывают параллельно!)
```

**Преимущества:**

- ✅ Параллельная обработка (3 consumers работают одновременно)
- ✅ Масштабируемость (можно добавить больше партиций и consumers)
- ✅ Порядок для одной книги (все события bookId=1 в одной партиции)

### Как выбирается ключ партиционирования?

В нашем проекте используются разные ключи для разных топиков:

| Топик            | Ключ     | Почему?                                                       |
| ---------------- | -------- | ------------------------------------------------------------- |
| `book.views`     | `bookId` | Все события одной книги в одной партиции → правильный порядок |
| `book.downloads` | `userId` | Все скачивания одного пользователя в одной партиции           |
| `book.purchases` | `userId` | Все покупки одного пользователя в одной партиции              |
| `book.reviews`   | `bookId` | Все отзывы одной книги в одной партиции                       |
| `book.ratings`   | `bookId` | Все рейтинги одной книги в одной партиции                     |

**Важно:** Ключ должен быть выбран так, чтобы связанные события попадали в одну партицию.

---

## Полный цикл данных (пример)

Давайте проследим полный путь одного события от начала до конца:

### Сценарий: Пользователь просматривает книгу

**1. Пользователь открывает страницу книги:**

```
GET /api/v1/books/1
```

**2. BookController обрабатывает запрос:**

```java
BookResponse book = bookService.getBookById(1);
// Создаем событие
BookViewEvent event = BookViewEvent.builder()
    .bookId(1)
    .userId(5)
    .bookTitle("Spring Boot Guide")
    .build();
// Отправляем в Kafka
kafkaProducerService.sendBookViewEvent(event);
// Возвращаем ответ пользователю
return ResponseEntity.ok(book);
```

**3. Kafka сохраняет событие:**

```
Топик: book.views
Ключ: "1" (bookId)
Партиция: hash("1") % 3 = Partition 1
Offset: 42 (номер сообщения в партиции)
```

**4. Микросервис аналитики читает событие:**

```
Consumer Group: analytics-service-group
Consumer читает из Partition 1
Offset обновляется: 42 → 43
```

**5. Обработка события:**

```java
// Обновляем статистику книги ID=1
bookStats.get(1).incrementViewCount();  // было 100, стало 101
bookStats.get(1).addUniqueViewer(5);   // добавили пользователя 5

// Обновляем активность пользователя 5
userActivity.get(5).incrementBooksViewed();  // было 10, стало 11
```

**6. Через 1 минуту - агрегация:**

```java
// Микросервис агрегирует данные
BookStatisticsAggregated aggregated = {
    bookId: 1,
    viewCount: 101,
    uniqueViewers: 50,
    ...
}

// Отправляет в Kafka
kafkaTemplate.send("analytics.aggregated-stats", "BOOK_STATS", aggregated);
```

**7. Основное приложение получает агрегированные данные:**

```java
// Сохраняет в БД
bookAnalyticsRepository.save(BookAnalytics {
    bookId: 1,
    viewCount: 101,
    aggregatedAt: "2025-12-17T13:20:00"
});
```

**8. Админ может посмотреть статистику:**

```
GET /api/v1/admin/analytics/books/1
→ Возвращает данные из БД
```

---

## Архитектура решения

```
┌─────────────────────────────────────────────────────────────┐
│                 spring-digital-bookstore                    │
│                    (Основное приложение)                    │
│                                                             │
│  ┌────────────────┐  ┌──────────────────┐  ┌────────────────┐
│  │ BookController │  │BookFileController│  │ReviewController│
│  │ BookViewEvent  │  │BookDownloadEvent │  │BookReviewEvent │
│  └───────┬────────┘  └────────┬─────────┘  └───────┬────────┘
│          │                    │                    │        │
│  ┌───────▼────────┐  ┌────────▼────────┐  ┌────────▼───────┐│
│  │RatingController│  │  StripeService  │  │                ││
│  │BookRatingEvent │  │BookPurchaseEvent│  │                ││
│  └───────┬────────┘  └────────┬────────┘  └────────┬───────┘│
│          │                    │                    │        │
│          └────────────────────┼────────────────────┘        │
│                               │                             │
│                        ┌──────▼──────┐                      │
│                        │KafkaProducer│                      │
│                        │  Service    │                      │
│                        └──────┬──────┘                      │
└───────────────────────────────┼─────────────────────────────┘
                                │
                                │ Kafka Events
                                │
┌───────────────────────────────▼──────────────────────────────┐
│                         Apache Kafka                         │
│                                                              │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐  │
│  │  book.views    │  │ book.downloads │  │book.purchases  │  │
│  │(3 partitions)  │  │(3 partitions)  │  │(2 partitions)  │  │
│  └────────────────┘  └────────────────┘  └────────────────┘  │
│                                                              │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐  │
│  │ book.reviews   │  │ book.ratings   │  │                │  │
│  │(2 partitions)  │  │(2 partitions)  │  │                │  │
│  └────────────────┘  └────────────────┘  └────────────────┘  │
└────────────────────────────┬─────────────────────────────────┘
                             │
                             │ Consume Events
                             │
┌────────────────────────────▼────────────────────────────────┐
│              Analytics Service (Microservice)               │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │          Kafka Consumers (Spring Kafka)             │    │
│  │  - BookViewConsumer                                 │    │
│  │  - BookDownloadConsumer                             │    │
│  │  - BookPurchaseConsumer                             │    │
│  │  - BookReviewConsumer                               │    │
│  │  - BookRatingConsumer                               │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │         In-Memory Storage (ConcurrentHashMap)       │    │
│  │  - BookStatistics (views, downloads, purchases)     │    │
│  │  - UserActivity (user actions)                      │    │
│  │  - ReviewStatistics (review counts, avg ratings)    │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              REST API (AnalyticsController)         │    │
│  │  - GET /api/analytics/books/{id}/stats              │    │
│  │  - GET /api/analytics/books/popular                 │    │
│  │  - GET /api/analytics/users/{id}/activity           │    │
│  │  - GET /api/analytics/overview                      │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                      Kafka UI (Web)                         │
│                  http://localhost:8089                      │
│  - Просмотр топиков и партиций                              │
│  - Просмотр сообщений                                       │
│  - Мониторинг consumer groups                               │
└─────────────────────────────────────────────────────────────┘

                             │
                             │ Aggregated Statistics
                             │ (every 1 minute)
                             │
┌────────────────────────────▼─────────────────────────────────┐
│                         Apache Kafka                         │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐    │
│  │ analytics.aggregated-stats (2 partitions)            │    │
│  │ - BookStatisticsAggregated                           │    │
│  │ - SystemOverviewAggregated                           │    │
│  │ - PopularBooksAggregated                             │    │
│  └──────────────────────────────────────────────────────┘    │
└────────────────────────────┬─────────────────────────────────┘
                             │
                             │ Consume Aggregated Data
                             │
┌────────────────────────────▼────────────────────────────────┐
│                 spring-digital-bookstore                    │
│                    (Основное приложение)                    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │     Kafka Consumer (AnalyticsStatsConsumer)         │    │
│  │  - Получает агрегированную статистику               │    │
│  │  - Сохраняет в БД (AnalyticsStats entity)           │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │         Database (PostgreSQL)                       │    │
│  │  - analytics_stats table                            │    │
│  │  - book_analytics table                             │    │
│  │  - system_analytics table                           │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │     Admin API (AnalyticsAdminController)            │    │
│  │  - GET /api/v1/admin/analytics/stats                │    │
│  │  - GET /api/v1/admin/analytics/books/{id}           │    │
│  │  - GET /api/v1/admin/analytics/overview             │    │
│  │  - GET /api/v1/admin/analytics/popular              │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## Часто задаваемые вопросы (FAQ)

### 1. Что произойдет, если Kafka недоступна?

**Ответ:** Producer (основное приложение) попытается отправить событие несколько раз (retries=3). Если не получится:

- Событие **не будет отправлено**
- Основное приложение **продолжит работать** (не упадет)
- Пользователь получит ответ, но событие потеряется

**Рекомендация:** В production нужно настроить Dead Letter Queue (DLQ) для потерянных сообщений.

### 2. Что произойдет, если микросервис аналитики упадет?

**Ответ:**

- Kafka **сохранит все события** (они не потеряются)
- Когда микросервис перезапустится, он **продолжит с того места, где остановился** (благодаря offset)
- Все события будут обработаны (может быть задержка)

**Пример:**

```
Микросервис обработал события до Offset 100
↓
Микросервис упал
↓
В Kafka накопилось еще 50 событий (Offset 101-150)
↓
Микросервис перезапустился
↓
Продолжил с Offset 101 и обработал все 50 событий
```

### 3. Можно ли запустить несколько экземпляров микросервиса аналитики?

**Ответ:** Да! Это называется **горизонтальное масштабирование**.

**Как это работает:**

```
Consumer Group: analytics-service-group

Экземпляр 1 → обрабатывает Partition 0 и 1
Экземпляр 2 → обрабатывает Partition 2

Или:

Экземпляр 1 → обрабатывает Partition 0
Экземпляр 2 → обрабатывает Partition 1
Экземпляр 3 → обрабатывает Partition 2
```

**Важно:** Kafka автоматически распределит партиции между экземплярами в одной группе.

### 4. Что такое offset и зачем он нужен?

**Offset** - это номер сообщения в партиции. Kafka запоминает, какое сообщение уже прочитано.

**Пример:**

```
Partition 0:
Offset 0: Event A ✅ (прочитано)
Offset 1: Event B ✅ (прочитано)
Offset 2: Event C ⏳ (следующее для чтения)
Offset 3: Event D
Offset 4: Event E
```

**Зачем:**

- Если consumer упал, он продолжит с последнего прочитанного offset
- Можно перечитать сообщения (если нужно)
- Гарантирует, что каждое сообщение обработано

### 5. Почему данные хранятся в памяти, а не в БД?

**Ответ:** Для pet проекта это упрощает реализацию и демонстрирует работу Kafka.

**Плюсы хранения в памяти:**

- ✅ Очень быстро (нет запросов к БД)
- ✅ Просто реализовать
- ✅ Достаточно для демонстрации

**Минусы:**

- ❌ Данные теряются при перезапуске
- ❌ Ограничение по памяти
- ❌ Не подходит для production

**В production:** Нужно сохранять в БД (PostgreSQL, MongoDB и т.д.)

### 6. Как работает партиционирование по ключу?

**Механизм:**

1. Producer отправляет сообщение с ключом (например, `bookId=1`)
2. Kafka вычисляет: `partition = hash(key) % numberOfPartitions`
3. Сообщение попадает в вычисленную партицию

**Пример:**

```
Топик book.views (3 партиции)
Ключ: bookId=1

hash("1") = 12345
12345 % 3 = 0

→ Сообщение попадает в Partition 0
```

**Важно:** Сообщения с одинаковым ключом всегда попадают в одну партицию.

### 7. Что такое Consumer Group и зачем он нужен?

**Consumer Group** - это группа consumers, которые работают вместе.

**Как работает:**

- Все consumers в группе **делят** партиции между собой
- Каждое сообщение обрабатывается **только одним** consumer из группы
- Если consumer упал, его партиции перераспределятся между другими

**Пример:**

```
Топик: book.views (3 партиции)
Consumer Group: analytics-service-group

Consumer 1 → Partition 0
Consumer 2 → Partition 1
Consumer 3 → Partition 2

Если Consumer 2 упал:
Consumer 1 → Partition 0
Consumer 3 → Partition 1 и 2 (взял на себя)
```

### 8. Можно ли читать одно сообщение несколько раз?

**Ответ:** Да, но нужно использовать разные Consumer Groups.

**Пример:**

```
Топик: book.views
Сообщение: Event A

Consumer Group 1 (analytics-service-group):
  → Consumer 1 читает Event A ✅

Consumer Group 2 (backup-service-group):
  → Consumer 2 читает Event A ✅ (то же сообщение!)

Consumer Group 3 (archive-service-group):
  → Consumer 3 читает Event A ✅ (и еще раз!)
```

**Зачем это нужно:**

- Разные сервисы могут обрабатывать одни и те же события
- Можно создать резервную копию данных
- Можно архивировать события

### 9. Что произойдет, если отправить событие без ключа?

**Ответ:** Kafka использует Round-Robin (по кругу) для распределения по партициям.

**Пример:**

```
Топик: book.views (3 партиции)
События без ключа:

Event 1 → Partition 0
Event 2 → Partition 1
Event 3 → Partition 2
Event 4 → Partition 0 (снова по кругу)
Event 5 → Partition 1
```

**Проблема:** События одной книги могут попасть в разные партиции → порядок не гарантирован.

**Рекомендация:** Всегда используйте ключ для связанных событий.

### 10. Как проверить, что все работает?

**Способы проверки:**

1. **Kafka UI** (http://localhost:8089):

   - Просмотр топиков и партиций
   - Просмотр сообщений
   - Мониторинг consumer groups и offsets

2. **Логи приложений:**

   - Основное приложение: "Sent book view event..."
   - Микросервис аналитики: "Received book view event..."

3. **API микросервиса аналитики:**

   ```bash
   curl http://localhost:8090/api/analytics/books/1/stats
   ```

4. **База данных:**
   ```sql
   SELECT * FROM book_analytics ORDER BY aggregated_at DESC;
   ```

---

## Обратный поток данных (Analytics → Main App)

### Механика работы

1. **Микросервис аналитики** периодически (каждую 1 минуту) агрегирует накопленные данные
2. **Отправляет** агрегированную статистику в топик `analytics.aggregated-stats`
3. **Основное приложение** подписывается на этот топик через Kafka Consumer
4. **Сохраняет** полученные данные в БД (PostgreSQL)
5. **Админ панель** получает данные через REST API из БД

### Преимущества подхода

- ✅ Асинхронная обработка - не блокирует основное приложение
- ✅ Надежность - данные сохраняются в БД, не теряются при перезапуске
- ✅ Масштабируемость - можно добавить несколько consumers
- ✅ История - можно хранить историю статистики по времени
- ✅ Тестирование Kafka - видно полный цикл: producer → kafka → consumer

---

## События и эндпоинты основного приложения

### 1. Просмотр книги (Book View)

**Контроллер:** `BookController`

**Эндпоинт:** `GET /api/v1/books/{bookId}`

**Событие:** `BookViewEvent`

**Когда отправлять:**

- После успешного получения детальной информации о книге в методе `getBookById()`
- Для авторизованных и неавторизованных пользователей (userId может быть null)

**Структура события:**

```json
{
  "eventId": "uuid",
  "eventType": "BOOK_VIEW",
  "timestamp": "2025-12-17T13:20:00Z",
  "bookId": 1,
  "userId": 5, // null если не авторизован
  "bookTitle": "Spring Boot Guide",
  "bookGenre": "TECHNOLOGY"
}
```

**Топик:** `book.views`
**Партиций:** 3
**Ключ партиционирования:** `bookId` (чтобы события одной книги попадали в одну партицию)

---

### 2. Скачивание книги (Book Download)

**Контроллер:** `BookFileController`

**Эндпоинт:** `GET /api/v1/books/{bookId}/download`

**Событие:** `BookDownloadEvent`

**Когда отправлять:**

- После успешного скачивания PDF файла в методе `downloadBook()`
- Только для авторизованных пользователей

**Структура события:**

```json
{
  "eventId": "uuid",
  "eventType": "BOOK_DOWNLOAD",
  "timestamp": "2025-12-17T13:20:00Z",
  "bookId": 1,
  "userId": 5,
  "bookTitle": "Spring Boot Guide",
  "bookPrice": 9.99,
  "isFree": false
}
```

**Топик:** `book.downloads`
**Партиций:** 3
**Ключ партиционирования:** `userId` (чтобы события одного пользователя попадали в одну партицию)

---

### 3. Покупка книги (Book Purchase)

**Сервис:** `StripeService` (не контроллер, но сервис обработки платежей)

**Эндпоинт:** Webhook от Stripe после успешной оплаты

**Событие:** `BookPurchaseEvent`

**Когда отправлять:**

- После успешной обработки webhook от Stripe (событие `checkout.session.completed`)
- В методе `StripeService.handlePaymentSuccess()` после сохранения покупки в БД

**Структура события:**

```json
{
  "eventId": "uuid",
  "eventType": "BOOK_PURCHASE",
  "timestamp": "2025-12-17T13:20:00Z",
  "bookId": 1,
  "userId": 5,
  "bookTitle": "Spring Boot Guide",
  "amountPaid": 8.99,
  "originalPrice": 9.99,
  "discountPercent": 10.0,
  "stripeSessionId": "cs_test_..."
}
```

**Топик:** `book.purchases`
**Партиций:** 2
**Ключ партиционирования:** `userId`

---

### 4. Создание отзыва (Book Review)

**Контроллер:** `ReviewController`

**Эндпоинты:**

- `POST /api/v1/books/{bookId}/reviews` - создание отзыва
- `PUT /api/v1/books/{bookId}/reviews/my` - обновление отзыва

**Событие:** `BookReviewEvent`

**Когда отправлять:**

- После успешного создания отзыва в методе `createReview()`
- После успешного обновления отзыва в методе `updateMyReview()`

**Структура события:**

```json
{
  "eventId": "uuid",
  "eventType": "BOOK_REVIEW",
  "timestamp": "2025-12-17T13:20:00Z",
  "bookId": 1,
  "userId": 5,
  "reviewId": 10,
  "action": "CREATED", // или "UPDATED"
  "reviewLength": 150
}
```

**Топик:** `book.reviews`
**Партиций:** 2
**Ключ партиционирования:** `bookId`

---

### 5. Создание рейтинга (Book Rating)

**Контроллер:** `RatingController`

**Эндпоинты:**

- `POST /api/v1/books/{bookId}/ratings` - создание рейтинга
- `PUT /api/v1/books/{bookId}/ratings/my` - обновление рейтинга

**Событие:** `BookRatingEvent`

**Когда отправлять:**

- После успешного создания рейтинга в методе `createRating()`
- После успешного обновления рейтинга в методе `updateRating()`

**Структура события:**

```json
{
  "eventId": "uuid",
  "eventType": "BOOK_RATING",
  "timestamp": "2025-12-17T13:20:00Z",
  "bookId": 1,
  "userId": 5,
  "ratingId": 20,
  "ratingValue": 8,
  "oldRatingValue": 7, // только для UPDATED, null для CREATED
  "action": "CREATED" // или "UPDATED"
}
```

**Топик:** `book.ratings`
**Партиций:** 2
**Ключ партиционирования:** `bookId`

---

## Структура проекта микросервиса

```
analytics-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── online/ityura/analytics/
│   │   │       ├── AnalyticsServiceApplication.java
│   │   │       ├── config/
│   │   │       │   ├── KafkaConfig.java
│   │   │       │   └── AnalyticsConfig.java
│   │   │       ├── controller/
│   │   │       │   └── AnalyticsController.java
│   │   │       ├── consumer/
│   │   │       │   ├── BookViewConsumer.java
│   │   │       │   ├── BookDownloadConsumer.java
│   │   │       │   ├── BookPurchaseConsumer.java
│   │   │       │   ├── BookReviewConsumer.java
│   │   │       │   └── BookRatingConsumer.java
│   │   │       ├── dto/
│   │   │       │   ├── BookViewEvent.java
│   │   │       │   ├── BookDownloadEvent.java
│   │   │       │   ├── BookPurchaseEvent.java
│   │   │       │   ├── BookReviewEvent.java
│   │   │       │   └── BookRatingEvent.java
│   │   │       ├── service/
│   │   │       │   └── AnalyticsService.java
│   │   │       └── model/
│   │   │           ├── BookStatistics.java
│   │   │           ├── UserActivity.java
│   │   │           └── ReviewStatistics.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application.yml
│   └── test/
├── Dockerfile
├── pom.xml
└── README.md
```

---

## Модели данных (In-Memory Storage)

### BookStatistics

Хранит статистику по каждой книге:

```java
public class BookStatistics {
    private Long bookId;
    private String bookTitle;
    private String bookGenre;

    // Счетчики
    private AtomicLong viewCount = new AtomicLong(0);
    private AtomicLong downloadCount = new AtomicLong(0);
    private AtomicLong purchaseCount = new AtomicLong(0);
    private AtomicLong reviewCount = new AtomicLong(0);
    private AtomicLong ratingCount = new AtomicLong(0);

    // Агрегированные данные
    private AtomicLong totalRevenue = new AtomicLong(0); // в центах
    private AtomicLong totalRatingsSum = new AtomicLong(0);
    private double averageRating = 0.0;

    // Временные метки
    private LocalDateTime firstViewAt;
    private LocalDateTime lastViewAt;
    private LocalDateTime lastPurchaseAt;

    // Уникальные пользователи (Set для дедупликации)
    private Set<Long> uniqueViewers = ConcurrentHashMap.newKeySet();
    private Set<Long> uniqueDownloaders = ConcurrentHashMap.newKeySet();
    private Set<Long> uniquePurchasers = ConcurrentHashMap.newKeySet();
}
```

**Хранение:** `ConcurrentHashMap<Long, BookStatistics>` где ключ = bookId

---

### UserActivity

Хранит активность каждого пользователя:

```java
public class UserActivity {
    private Long userId;

    // Счетчики действий
    private AtomicLong booksViewed = new AtomicLong(0);
    private AtomicLong booksDownloaded = new AtomicLong(0);
    private AtomicLong booksPurchased = new AtomicLong(0);
    private AtomicLong reviewsCreated = new AtomicLong(0);
    private AtomicLong ratingsCreated = new AtomicLong(0);

    // Список просмотренных книг
    private Set<Long> viewedBooks = ConcurrentHashMap.newKeySet();

    // Список скачанных книг
    private Set<Long> downloadedBooks = ConcurrentHashMap.newKeySet();

    // Список купленных книг
    private Set<Long> purchasedBooks = ConcurrentHashMap.newKeySet();

    // Временные метки
    private LocalDateTime firstActivityAt;
    private LocalDateTime lastActivityAt;

    // Общая сумма покупок (в центах)
    private AtomicLong totalSpent = new AtomicLong(0);
}
```

**Хранение:** `ConcurrentHashMap<Long, UserActivity>` где ключ = userId

---

### ReviewStatistics

Агрегированная статистика по отзывам:

```java
public class ReviewStatistics {
    private AtomicLong totalReviews = new AtomicLong(0);
    private AtomicLong totalReviewLength = new AtomicLong(0);
    private double averageReviewLength = 0.0;

    // Статистика по действиям
    private AtomicLong reviewsCreated = new AtomicLong(0);
    private AtomicLong reviewsUpdated = new AtomicLong(0);
}
```

**Хранение:** Один экземпляр (singleton)

---

## API микросервиса аналитики

### Base URL: `http://localhost:8090/api/analytics`

### 1. GET /api/analytics/books/{bookId}/stats

**Описание:** Получить статистику по конкретной книге

**Response (200 OK):**

```json
{
  "bookId": 1,
  "bookTitle": "Spring Boot Guide",
  "bookGenre": "TECHNOLOGY",
  "viewCount": 1250,
  "downloadCount": 340,
  "purchaseCount": 280,
  "reviewCount": 45,
  "ratingCount": 120,
  "averageRating": 8.5,
  "totalRevenue": 2519.2,
  "uniqueViewers": 850,
  "uniqueDownloaders": 340,
  "uniquePurchasers": 280,
  "firstViewAt": "2025-01-15T10:30:00",
  "lastViewAt": "2025-12-17T14:20:00",
  "lastPurchaseAt": "2025-12-17T13:45:00"
}
```

**Ошибки:**

- `404` - Книга не найдена в статистике

---

### 2. GET /api/analytics/books/popular

**Описание:** Получить список самых популярных книг

**Query Parameters:**

| Параметр | Тип    | Обязательный | По умолчанию | Описание                                                 |
| -------- | ------ | ------------ | ------------ | -------------------------------------------------------- |
| limit    | int    | Нет          | 10           | Количество книг                                          |
| sortBy   | string | Нет          | "views"      | Сортировка: "views", "downloads", "purchases", "revenue" |

**Response (200 OK):**

```json
{
  "books": [
    {
      "bookId": 1,
      "bookTitle": "Spring Boot Guide",
      "viewCount": 1250,
      "downloadCount": 340,
      "purchaseCount": 280,
      "totalRevenue": 2519.2,
      "rank": 1
    },
    {
      "bookId": 5,
      "bookTitle": "Java Advanced",
      "viewCount": 980,
      "downloadCount": 250,
      "purchaseCount": 200,
      "totalRevenue": 1799.0,
      "rank": 2
    }
  ],
  "total": 2
}
```

---

### 3. GET /api/analytics/users/{userId}/activity

**Описание:** Получить активность конкретного пользователя

**Response (200 OK):**

```json
{
  "userId": 5,
  "booksViewed": 25,
  "booksDownloaded": 12,
  "booksPurchased": 8,
  "reviewsCreated": 5,
  "ratingsCreated": 10,
  "totalSpent": 71.92,
  "viewedBooks": [1, 2, 3, 5, 7],
  "downloadedBooks": [1, 2, 3],
  "purchasedBooks": [1, 5, 7],
  "firstActivityAt": "2025-01-10T09:15:00",
  "lastActivityAt": "2025-12-17T14:20:00"
}
```

**Ошибки:**

- `404` - Пользователь не найден в статистике

---

### 4. GET /api/analytics/overview

**Описание:** Получить общую статистику по всей системе

**Response (200 OK):**

```json
{
  "totalBooks": 150,
  "totalUsers": 1250,
  "totalViews": 45000,
  "totalDownloads": 12000,
  "totalPurchases": 8500,
  "totalRevenue": 76500.5,
  "totalReviews": 1200,
  "totalRatings": 3500,
  "averageRating": 7.8,
  "averageReviewLength": 245.5,
  "mostPopularBook": {
    "bookId": 1,
    "bookTitle": "Spring Boot Guide",
    "viewCount": 1250
  },
  "topGenre": {
    "genre": "TECHNOLOGY",
    "bookCount": 45,
    "totalViews": 18000
  }
}
```

---

### 5. GET /api/analytics/health

**Описание:** Проверка состояния сервиса

**Response (200 OK):**

```json
{
  "status": "UP",
  "eventsProcessed": 125000,
  "booksTracked": 150,
  "usersTracked": 1250,
  "uptime": 3600000
}
```

---

## Обратный поток данных: Analytics → Main App

### Архитектура обратного потока

После обработки событий микросервис аналитики периодически агрегирует данные и отправляет их обратно в основное приложение через Kafka. Основное приложение сохраняет эти данные в БД, и админ может просматривать их через админ панель.

### Топик для агрегированных данных

#### analytics.aggregated-stats

```properties
Topic: analytics.aggregated-stats
Partitions: 2
Replication Factor: 1
Key: aggregationType (String) - "BOOK_STATS", "SYSTEM_OVERVIEW", "POPULAR_BOOKS"
Value: AggregatedStatisticsEvent (JSON)
Retention: 7 days
```

**Назначение:** Агрегированная статистика от микросервиса аналитики

**Типы сообщений:**

1. `BOOK_STATS` - статистика по конкретной книге
2. `SYSTEM_OVERVIEW` - общая статистика системы
3. `POPULAR_BOOKS` - список популярных книг

---

### DTO для агрегированных данных

#### BookStatisticsAggregated

```json
{
  "aggregationType": "BOOK_STATS",
  "timestamp": "2025-12-17T13:20:00Z",
  "bookId": 1,
  "bookTitle": "Spring Boot Guide",
  "bookGenre": "TECHNOLOGY",
  "viewCount": 1250,
  "downloadCount": 340,
  "purchaseCount": 280,
  "reviewCount": 45,
  "ratingCount": 120,
  "averageRating": 8.5,
  "totalRevenue": 2519.2,
  "uniqueViewers": 850,
  "uniqueDownloaders": 340,
  "uniquePurchasers": 280,
  "firstViewAt": "2025-01-15T10:30:00",
  "lastViewAt": "2025-12-17T14:20:00",
  "lastPurchaseAt": "2025-12-17T13:45:00"
}
```

#### SystemOverviewAggregated

```json
{
  "aggregationType": "SYSTEM_OVERVIEW",
  "timestamp": "2025-12-17T13:20:00Z",
  "totalBooks": 150,
  "totalUsers": 1250,
  "totalViews": 45000,
  "totalDownloads": 12000,
  "totalPurchases": 8500,
  "totalRevenue": 76500.5,
  "totalReviews": 1200,
  "totalRatings": 3500,
  "averageRating": 7.8,
  "averageReviewLength": 245.5,
  "mostPopularBookId": 1,
  "mostPopularBookTitle": "Spring Boot Guide",
  "topGenre": "TECHNOLOGY",
  "topGenreBookCount": 45,
  "topGenreTotalViews": 18000
}
```

#### PopularBooksAggregated

```json
{
  "aggregationType": "POPULAR_BOOKS",
  "timestamp": "2025-12-17T13:20:00Z",
  "books": [
    {
      "bookId": 1,
      "bookTitle": "Spring Boot Guide",
      "viewCount": 1250,
      "downloadCount": 340,
      "purchaseCount": 280,
      "totalRevenue": 2519.2,
      "rank": 1
    },
    {
      "bookId": 5,
      "bookTitle": "Java Advanced",
      "viewCount": 980,
      "downloadCount": 250,
      "purchaseCount": 200,
      "totalRevenue": 1799.0,
      "rank": 2
    }
  ],
  "limit": 10,
  "sortBy": "views"
}
```

---

### Реализация в микросервисе аналитики

#### 1. Scheduled Task для агрегации

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsAggregationScheduler {

    private final AnalyticsService analyticsService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Агрегация каждую 1 минуту (по умолчанию)
    // Для тестирования можно уменьшить: 10000 (10 сек), 30000 (30 сек)
    @Scheduled(fixedRate = 60000) // 1 минута в миллисекундах
    public void aggregateAndSendStatistics() {
        log.info("Starting statistics aggregation...");

        try {
            // 1. Агрегировать статистику по всем книгам
            Map<Long, BookStatistics> allBookStats = analyticsService.getAllBookStatistics();
            for (BookStatistics stats : allBookStats.values()) {
                BookStatisticsAggregated aggregated = convertToAggregated(stats);
                kafkaTemplate.send(
                    "analytics.aggregated-stats",
                    "BOOK_STATS",
                    aggregated
                );
                log.debug("Sent aggregated stats for book: {}", stats.getBookId());
            }

            // 2. Отправить общую статистику системы
            SystemOverviewAggregated systemOverview = analyticsService.getSystemOverview();
            kafkaTemplate.send(
                "analytics.aggregated-stats",
                "SYSTEM_OVERVIEW",
                systemOverview
            );
            log.debug("Sent system overview");

            // 3. Отправить популярные книги
            PopularBooksAggregated popularBooks = analyticsService.getPopularBooks(10, "views");
            kafkaTemplate.send(
                "analytics.aggregated-stats",
                "POPULAR_BOOKS",
                popularBooks
            );
            log.debug("Sent popular books");

            log.info("Statistics aggregation completed successfully");
        } catch (Exception e) {
            log.error("Error during statistics aggregation", e);
        }
    }

    private BookStatisticsAggregated convertToAggregated(BookStatistics stats) {
        return BookStatisticsAggregated.builder()
            .aggregationType("BOOK_STATS")
            .timestamp(LocalDateTime.now())
            .bookId(stats.getBookId())
            .bookTitle(stats.getBookTitle())
            .bookGenre(stats.getBookGenre())
            .viewCount(stats.getViewCount().get())
            .downloadCount(stats.getDownloadCount().get())
            .purchaseCount(stats.getPurchaseCount().get())
            .reviewCount(stats.getReviewCount().get())
            .ratingCount(stats.getRatingCount().get())
            .averageRating(stats.getAverageRating())
            .totalRevenue(stats.getTotalRevenue().get() / 100.0) // из центов в доллары
            .uniqueViewers(stats.getUniqueViewers().size())
            .uniqueDownloaders(stats.getUniqueDownloaders().size())
            .uniquePurchasers(stats.getUniquePurchasers().size())
            .firstViewAt(stats.getFirstViewAt())
            .lastViewAt(stats.getLastViewAt())
            .lastPurchaseAt(stats.getLastPurchaseAt())
            .build();
    }
}
```

#### 2. Конфигурация Scheduled Tasks

```java
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Конфигурация для @Scheduled
}
```

#### 3. Kafka Producer Configuration

```properties
# Kafka Producer для отправки агрегированных данных
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.producer.acks=all
spring.kafka.producer.retries=3
```

---

### Реализация в основном приложении

#### 1. Entity для сохранения статистики

```java
@Entity
@Table(name = "book_analytics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "book_title")
    private String bookTitle;

    @Column(name = "book_genre")
    private String bookGenre;

    @Column(name = "view_count")
    private Long viewCount;

    @Column(name = "download_count")
    private Long downloadCount;

    @Column(name = "purchase_count")
    private Long purchaseCount;

    @Column(name = "review_count")
    private Long reviewCount;

    @Column(name = "rating_count")
    private Long ratingCount;

    @Column(name = "average_rating", precision = 4, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "total_revenue", precision = 10, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "unique_viewers")
    private Integer uniqueViewers;

    @Column(name = "unique_downloaders")
    private Integer uniqueDownloaders;

    @Column(name = "unique_purchasers")
    private Integer uniquePurchasers;

    @Column(name = "aggregated_at", nullable = false)
    private LocalDateTime aggregatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

```java
@Entity
@Table(name = "system_analytics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_books")
    private Integer totalBooks;

    @Column(name = "total_users")
    private Integer totalUsers;

    @Column(name = "total_views")
    private Long totalViews;

    @Column(name = "total_downloads")
    private Long totalDownloads;

    @Column(name = "total_purchases")
    private Long totalPurchases;

    @Column(name = "total_revenue", precision = 12, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "total_reviews")
    private Long totalReviews;

    @Column(name = "total_ratings")
    private Long totalRatings;

    @Column(name = "average_rating", precision = 4, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "average_review_length", precision = 6, scale = 2)
    private BigDecimal averageReviewLength;

    @Column(name = "most_popular_book_id")
    private Long mostPopularBookId;

    @Column(name = "most_popular_book_title")
    private String mostPopularBookTitle;

    @Column(name = "top_genre")
    private String topGenre;

    @Column(name = "top_genre_book_count")
    private Integer topGenreBookCount;

    @Column(name = "top_genre_total_views")
    private Long topGenreTotalViews;

    @Column(name = "aggregated_at", nullable = false)
    private LocalDateTime aggregatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

#### 2. Kafka Consumer в основном приложении

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsStatsConsumer {

    private final BookAnalyticsRepository bookAnalyticsRepository;
    private final SystemAnalyticsRepository systemAnalyticsRepository;
    private final ObjectMapper objectMapper = new ObjectMapper() {{
        registerModule(new JavaTimeModule());
    }};

    @KafkaListener(
        topics = "analytics.aggregated-stats",
        groupId = "main-app-analytics-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAggregatedStats(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {

        log.info("Received aggregated stats with key: {}", key);

        try {

            switch (key) {
                case "BOOK_STATS":
                    BookStatisticsAggregated bookStats = objectMapper.convertValue(
                        payload,
                        BookStatisticsAggregated.class
                    );
                    saveBookAnalytics(bookStats);
                    break;

                case "SYSTEM_OVERVIEW":
                    SystemOverviewAggregated systemOverview = objectMapper.convertValue(
                        payload,
                        SystemOverviewAggregated.class
                    );
                    saveSystemAnalytics(systemOverview);
                    break;

                case "POPULAR_BOOKS":
                    // Можно сохранить в отдельную таблицу или пропустить
                    // (данные уже есть в book_analytics)
                    log.debug("Received popular books, skipping save");
                    break;

                default:
                    log.warn("Unknown aggregation type: {}", key);
            }
        } catch (Exception e) {
            log.error("Error processing aggregated stats", e);
            // Можно отправить в DLQ (Dead Letter Queue)
        }
    }

    private void saveBookAnalytics(BookStatisticsAggregated stats) {
        BookAnalytics analytics = BookAnalytics.builder()
            .bookId(stats.getBookId())
            .bookTitle(stats.getBookTitle())
            .bookGenre(stats.getBookGenre())
            .viewCount(stats.getViewCount())
            .downloadCount(stats.getDownloadCount())
            .purchaseCount(stats.getPurchaseCount())
            .reviewCount(stats.getReviewCount())
            .ratingCount(stats.getRatingCount())
            .averageRating(BigDecimal.valueOf(stats.getAverageRating()))
            .totalRevenue(BigDecimal.valueOf(stats.getTotalRevenue()))
            .uniqueViewers(stats.getUniqueViewers())
            .uniqueDownloaders(stats.getUniqueDownloaders())
            .uniquePurchasers(stats.getUniquePurchasers())
            .aggregatedAt(stats.getTimestamp())
            .build();

        // Сохраняем новую запись (можно добавить логику обновления последней)
        bookAnalyticsRepository.save(analytics);
        log.info("Saved book analytics for bookId: {}", stats.getBookId());
    }

    private void saveSystemAnalytics(SystemOverviewAggregated stats) {
        SystemAnalytics analytics = SystemAnalytics.builder()
            .totalBooks(stats.getTotalBooks())
            .totalUsers(stats.getTotalUsers())
            .totalViews(stats.getTotalViews())
            .totalDownloads(stats.getTotalDownloads())
            .totalPurchases(stats.getTotalPurchases())
            .totalRevenue(BigDecimal.valueOf(stats.getTotalRevenue()))
            .totalReviews(stats.getTotalReviews())
            .totalRatings(stats.getTotalRatings())
            .averageRating(BigDecimal.valueOf(stats.getAverageRating()))
            .averageReviewLength(BigDecimal.valueOf(stats.getAverageReviewLength()))
            .mostPopularBookId(stats.getMostPopularBookId())
            .mostPopularBookTitle(stats.getMostPopularBookTitle())
            .topGenre(stats.getTopGenre())
            .topGenreBookCount(stats.getTopGenreBookCount())
            .topGenreTotalViews(stats.getTopGenreTotalViews())
            .aggregatedAt(stats.getTimestamp())
            .build();

        systemAnalyticsRepository.save(analytics);
        log.info("Saved system analytics");
    }
}
```

#### 3. Конфигурация Kafka Consumer в основном приложении

```properties
# Kafka Consumer для получения агрегированных данных
spring.kafka.consumer.group-id=main-app-analytics-group
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.enable-auto-commit=true
```

#### 4. Repository для работы с БД

```java
@Repository
public interface BookAnalyticsRepository extends JpaRepository<BookAnalytics, Long> {

    // Получить последнюю статистику по книге
    Optional<BookAnalytics> findFirstByBookIdOrderByAggregatedAtDesc(Long bookId);

    // Получить статистику за период
    List<BookAnalytics> findByBookIdAndAggregatedAtBetween(
        Long bookId,
        LocalDateTime start,
        LocalDateTime end
    );

    // Получить все книги с последней статистикой
    @Query("SELECT ba FROM BookAnalytics ba " +
           "WHERE ba.aggregatedAt = (SELECT MAX(ba2.aggregatedAt) " +
           "FROM BookAnalytics ba2 WHERE ba2.bookId = ba.bookId)")
    List<BookAnalytics> findLatestForAllBooks();
}
```

```java
@Repository
public interface SystemAnalyticsRepository extends JpaRepository<SystemAnalytics, Long> {

    // Получить последнюю статистику системы
    Optional<SystemAnalytics> findFirstByOrderByAggregatedAtDesc();

    // Получить статистику за период
    List<SystemAnalytics> findByAggregatedAtBetween(
        LocalDateTime start,
        LocalDateTime end
    );
}
```

---

### API для админ панели

#### 1. GET /api/v1/admin/analytics/books/{bookId}

**Описание:** Получить последнюю статистику по книге

**Авторизация:** Требуется (Bearer Token, роль ADMIN)

**Response (200 OK):**

```json
{
  "bookId": 1,
  "bookTitle": "Spring Boot Guide",
  "bookGenre": "TECHNOLOGY",
  "viewCount": 1250,
  "downloadCount": 340,
  "purchaseCount": 280,
  "reviewCount": 45,
  "ratingCount": 120,
  "averageRating": 8.5,
  "totalRevenue": 2519.2,
  "uniqueViewers": 850,
  "uniqueDownloaders": 340,
  "uniquePurchasers": 280,
  "aggregatedAt": "2025-12-17T13:20:00",
  "createdAt": "2025-12-17T13:20:00"
}
```

**Ошибки:**

- `404` - Статистика не найдена

---

#### 2. GET /api/v1/admin/analytics/books/{bookId}/history

**Описание:** Получить историю статистики по книге за период

**Авторизация:** Требуется (Bearer Token, роль ADMIN)

**Query Parameters:**

| Параметр  | Тип    | Обязательный | Описание                  |
| --------- | ------ | ------------ | ------------------------- |
| startDate | string | Нет          | Начальная дата (ISO 8601) |
| endDate   | string | Нет          | Конечная дата (ISO 8601)  |

**Response (200 OK):**

```json
{
  "bookId": 1,
  "bookTitle": "Spring Boot Guide",
  "history": [
    {
      "viewCount": 1200,
      "downloadCount": 330,
      "purchaseCount": 275,
      "totalRevenue": 2475.0,
      "aggregatedAt": "2025-12-17T13:15:00"
    },
    {
      "viewCount": 1250,
      "downloadCount": 340,
      "purchaseCount": 280,
      "totalRevenue": 2519.2,
      "aggregatedAt": "2025-12-17T13:20:00"
    }
  ]
}
```

---

#### 3. GET /api/v1/admin/analytics/overview

**Описание:** Получить последнюю общую статистику системы

**Авторизация:** Требуется (Bearer Token, роль ADMIN)

**Response (200 OK):**

```json
{
  "totalBooks": 150,
  "totalUsers": 1250,
  "totalViews": 45000,
  "totalDownloads": 12000,
  "totalPurchases": 8500,
  "totalRevenue": 76500.5,
  "totalReviews": 1200,
  "totalRatings": 3500,
  "averageRating": 7.8,
  "averageReviewLength": 245.5,
  "mostPopularBookId": 1,
  "mostPopularBookTitle": "Spring Boot Guide",
  "topGenre": "TECHNOLOGY",
  "topGenreBookCount": 45,
  "topGenreTotalViews": 18000,
  "aggregatedAt": "2025-12-17T13:20:00"
}
```

**Ошибки:**

- `404` - Статистика не найдена

---

#### 4. GET /api/v1/admin/analytics/popular

**Описание:** Получить список популярных книг (на основе последней статистики)

**Авторизация:** Требуется (Bearer Token, роль ADMIN)

**Query Parameters:**

| Параметр | Тип    | Обязательный | По умолчанию | Описание                                                 |
| -------- | ------ | ------------ | ------------ | -------------------------------------------------------- |
| limit    | int    | Нет          | 10           | Количество книг                                          |
| sortBy   | string | Нет          | "views"      | Сортировка: "views", "downloads", "purchases", "revenue" |

**Response (200 OK):**

```json
{
  "books": [
    {
      "bookId": 1,
      "bookTitle": "Spring Boot Guide",
      "viewCount": 1250,
      "downloadCount": 340,
      "purchaseCount": 280,
      "totalRevenue": 2519.2,
      "rank": 1
    },
    {
      "bookId": 5,
      "bookTitle": "Java Advanced",
      "viewCount": 980,
      "downloadCount": 250,
      "purchaseCount": 200,
      "totalRevenue": 1799.0,
      "rank": 2
    }
  ],
  "total": 2,
  "sortBy": "views"
}
```

---

#### 5. GET /api/v1/admin/analytics/overview/history

**Описание:** Получить историю общей статистики за период

**Авторизация:** Требуется (Bearer Token, роль ADMIN)

**Query Parameters:**

| Параметр  | Тип    | Обязательный | Описание                  |
| --------- | ------ | ------------ | ------------------------- |
| startDate | string | Нет          | Начальная дата (ISO 8601) |
| endDate   | string | Нет          | Конечная дата (ISO 8601)  |

**Response (200 OK):**

```json
{
  "history": [
    {
      "totalBooks": 150,
      "totalUsers": 1245,
      "totalViews": 44800,
      "totalRevenue": 76200.0,
      "aggregatedAt": "2025-12-17T13:15:00"
    },
    {
      "totalBooks": 150,
      "totalUsers": 1250,
      "totalViews": 45000,
      "totalRevenue": 76500.5,
      "aggregatedAt": "2025-12-17T13:20:00"
    }
  ]
}
```

---

### Реализация контроллера для админ панели

```java
@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "Админ - Аналитика", description = "API для просмотра аналитики в админ панели")
@SecurityRequirement(name = "Bearer Authentication")
public class AnalyticsAdminController {

    private final BookAnalyticsRepository bookAnalyticsRepository;
    private final SystemAnalyticsRepository systemAnalyticsRepository;

    @Operation(summary = "Получить статистику по книге")
    @GetMapping("/books/{bookId}")
    public ResponseEntity<BookAnalyticsResponse> getBookAnalytics(@PathVariable Long bookId) {
        BookAnalytics analytics = bookAnalyticsRepository
            .findFirstByBookIdOrderByAggregatedAtDesc(bookId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Analytics not found for book: " + bookId
            ));

        return ResponseEntity.ok(BookAnalyticsResponse.from(analytics));
    }

    @Operation(summary = "Получить историю статистики по книге")
    @GetMapping("/books/{bookId}/history")
    public ResponseEntity<BookAnalyticsHistoryResponse> getBookAnalyticsHistory(
            @PathVariable Long bookId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        List<BookAnalytics> history = bookAnalyticsRepository
            .findByBookIdAndAggregatedAtBetween(bookId, start, end);

        if (history.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No analytics history found for book: " + bookId
            );
        }

        String bookTitle = history.get(0).getBookTitle();
        return ResponseEntity.ok(BookAnalyticsHistoryResponse.from(bookId, bookTitle, history));
    }

    @Operation(summary = "Получить общую статистику системы")
    @GetMapping("/overview")
    public ResponseEntity<SystemAnalyticsResponse> getSystemOverview() {
        SystemAnalytics analytics = systemAnalyticsRepository
            .findFirstByOrderByAggregatedAtDesc()
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "System analytics not found"
            ));

        return ResponseEntity.ok(SystemAnalyticsResponse.from(analytics));
    }

    @Operation(summary = "Получить популярные книги")
    @GetMapping("/popular")
    public ResponseEntity<PopularBooksResponse> getPopularBooks(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "views") String sortBy) {

        List<BookAnalytics> allBooks = bookAnalyticsRepository.findLatestForAllBooks();

        // Сортировка и фильтрация
        List<BookAnalytics> sorted = allBooks.stream()
            .sorted(getComparator(sortBy))
            .limit(limit)
            .collect(Collectors.toList());

        return ResponseEntity.ok(PopularBooksResponse.from(sorted, sortBy));
    }

    @Operation(summary = "Получить историю общей статистики")
    @GetMapping("/overview/history")
    public ResponseEntity<SystemAnalyticsHistoryResponse> getSystemOverviewHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        List<SystemAnalytics> history = systemAnalyticsRepository
            .findByAggregatedAtBetween(start, end);

        return ResponseEntity.ok(SystemAnalyticsHistoryResponse.from(history));
    }

    private Comparator<BookAnalytics> getComparator(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "downloads" -> Comparator.comparing(BookAnalytics::getDownloadCount).reversed();
            case "purchases" -> Comparator.comparing(BookAnalytics::getPurchaseCount).reversed();
            case "revenue" -> Comparator.comparing(BookAnalytics::getTotalRevenue).reversed();
            default -> Comparator.comparing(BookAnalytics::getViewCount).reversed();
        };
    }
}
```

---

### SQL миграции для создания таблиц

```sql
-- Таблица для статистики по книгам
CREATE TABLE book_analytics (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    book_title VARCHAR(255),
    book_genre VARCHAR(50),
    view_count BIGINT DEFAULT 0,
    download_count BIGINT DEFAULT 0,
    purchase_count BIGINT DEFAULT 0,
    review_count BIGINT DEFAULT 0,
    rating_count BIGINT DEFAULT 0,
    average_rating DECIMAL(4, 2),
    total_revenue DECIMAL(10, 2),
    unique_viewers INTEGER DEFAULT 0,
    unique_downloaders INTEGER DEFAULT 0,
    unique_purchasers INTEGER DEFAULT 0,
    aggregated_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_book_analytics_book_id ON book_analytics(book_id);
CREATE INDEX idx_book_analytics_aggregated_at ON book_analytics(aggregated_at);

-- Таблица для общей статистики системы
CREATE TABLE system_analytics (
    id BIGSERIAL PRIMARY KEY,
    total_books INTEGER DEFAULT 0,
    total_users INTEGER DEFAULT 0,
    total_views BIGINT DEFAULT 0,
    total_downloads BIGINT DEFAULT 0,
    total_purchases BIGINT DEFAULT 0,
    total_revenue DECIMAL(12, 2),
    total_reviews BIGINT DEFAULT 0,
    total_ratings BIGINT DEFAULT 0,
    average_rating DECIMAL(4, 2),
    average_review_length DECIMAL(6, 2),
    most_popular_book_id BIGINT,
    most_popular_book_title VARCHAR(255),
    top_genre VARCHAR(50),
    top_genre_book_count INTEGER,
    top_genre_total_views BIGINT,
    aggregated_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_system_analytics_aggregated_at ON system_analytics(aggregated_at);
```

---

## Конфигурация Kafka

### Топики и их настройки

#### 1. book.views

```properties
Topic: book.views
Partitions: 3
Replication Factor: 1
Key: bookId (Long, сериализуется как String)
Value: BookViewEvent (JSON)
Retention: 7 days
```

**Назначение:** События просмотра книг

---

#### 2. book.downloads

```properties
Topic: book.downloads
Partitions: 3
Replication Factor: 1
Key: userId (Long, сериализуется как String)
Value: BookDownloadEvent (JSON)
Retention: 7 days
```

**Назначение:** События скачивания книг

---

#### 3. book.purchases

```properties
Topic: book.purchases
Partitions: 2
Replication Factor: 1
Key: userId (Long, сериализуется как String)
Value: BookPurchaseEvent (JSON)
Retention: 30 days
```

**Назначение:** События покупки книг

---

#### 4. book.reviews

```properties
Topic: book.reviews
Partitions: 2
Replication Factor: 1
Key: bookId (Long, сериализуется как String)
Value: BookReviewEvent (JSON)
Retention: 7 days
```

**Назначение:** События создания/обновления отзывов

---

#### 5. book.ratings

```properties
Topic: book.ratings
Partitions: 2
Replication Factor: 1
Key: bookId (Long, сериализуется как String)
Value: BookRatingEvent (JSON)
Retention: 7 days
```

**Назначение:** События создания/обновления рейтингов

---

#### 6. analytics.aggregated-stats

```properties
Topic: analytics.aggregated-stats
Partitions: 2
Replication Factor: 1
Key: aggregationType (String) - "BOOK_STATS", "SYSTEM_OVERVIEW", "POPULAR_BOOKS"
Value: AggregatedStatisticsEvent (JSON)
Retention: 7 days
```

**Назначение:** Агрегированная статистика от микросервиса аналитики обратно в основное приложение

**Типы сообщений:**

- `BOOK_STATS` - статистика по конкретной книге
- `SYSTEM_OVERVIEW` - общая статистика системы
- `POPULAR_BOOKS` - список популярных книг

**Частота отправки:** Каждую 1 минуту (настраивается)

---

## Docker Compose конфигурация

### docker-compose.analytics.yml

```yaml
version: "3.8"

services:
  # Zookeeper для Kafka
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"
    networks:
      - kafka-network

  # Kafka Broker
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
      - "9093:9093"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,PLAINTEXT_INTERNAL://kafka:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_INTERNAL:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT_INTERNAL
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    networks:
      - kafka-network
    healthcheck:
      test:
        [
          "CMD",
          "kafka-broker-api-versions",
          "--bootstrap-server",
          "localhost:9092",
        ]
      interval: 30s
      timeout: 10s
      retries: 5

  # Kafka UI - графический интерфейс
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    depends_on:
      - kafka
    ports:
      - "8089:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
      KAFKA_CLUSTERS_0_ZOOKEEPER: zookeeper:2181
    networks:
      - kafka-network

  # Analytics Service
  analytics-service:
    build:
      context: ../analytics-service
      dockerfile: ../analytics-service/Dockerfile
    container_name: analytics-service
    depends_on:
      kafka:
        condition: service_healthy
    ports:
      - "8090:8090"
    environment:
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      SPRING_KAFKA_CONSUMER_GROUP_ID: analytics-service-group
      SERVER_PORT: 8090
    networks:
      - kafka-network
    restart: unless-stopped

networks:
  kafka-network:
    driver: bridge
```

---

## Пошаговая инструкция реализации

### Шаг 1: Подготовка основного приложения

#### 1.1. Добавление зависимостей Kafka в pom.xml

```xml
<!-- Spring Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

#### 1.2. Создание DTO для событий

Создать пакет `dto.event` с классами:

- `BookViewEvent`
- `BookDownloadEvent`
- `BookPurchaseEvent`
- `BookReviewEvent`
- `BookRatingEvent`

#### 1.3. Создание KafkaProducerService

Создать сервис для отправки событий в Kafka:

```java
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendBookViewEvent(BookViewEvent event) {
        kafkaTemplate.send("book.views", String.valueOf(event.getBookId()), event);
    }

    public void sendBookDownloadEvent(BookDownloadEvent event) {
        kafkaTemplate.send("book.downloads", String.valueOf(event.getUserId()), event);
    }

    // ... аналогично для других событий
}
```

#### 1.4. Интеграция в контроллеры

**BookController:**

```java
@GetMapping("/{bookId}")
public ResponseEntity<BookResponse> getBookById(@PathVariable Long bookId,
                                               Authentication authentication) {
    BookResponse book = bookService.getBookById(bookId);

    // Отправка события просмотра
    BookViewEvent event = BookViewEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType("BOOK_VIEW")
        .timestamp(LocalDateTime.now())
        .bookId(bookId)
        .userId(getCurrentUserIdOrNull(authentication)) // может быть null
        .bookTitle(book.getTitle())
        .bookGenre(book.getGenre() != null ? book.getGenre().name() : null)
        .build();

    kafkaProducerService.sendBookViewEvent(event);

    return ResponseEntity.ok(book);
}
```

**BookFileController (download):**

```java
@GetMapping("/{bookId}/download")
public ResponseEntity<Resource> downloadBook(@PathVariable Long bookId,
                                             Authentication authentication) {
    // ... логика скачивания ...

    // Отправка события скачивания
    BookDownloadEvent event = BookDownloadEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType("BOOK_DOWNLOAD")
        .timestamp(LocalDateTime.now())
        .bookId(bookId)
        .userId(getCurrentUserId(authentication))
        .bookTitle(book.getTitle())
        .bookPrice(book.getPrice().doubleValue())
        .isFree(book.getPrice().compareTo(BigDecimal.ZERO) == 0)
        .build();

    kafkaProducerService.sendBookDownloadEvent(event);

    return ResponseEntity.ok(resource);
}
```

**StripeService (purchase):**

```java
public void handlePaymentSuccess(String sessionId) {
    // ... логика обработки оплаты ...

    // Отправка события покупки
    BookPurchaseEvent event = BookPurchaseEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType("BOOK_PURCHASE")
        .timestamp(LocalDateTime.now())
        .bookId(book.getId())
        .userId(user.getId())
        .bookTitle(book.getTitle())
        .amountPaid(purchase.getAmountPaid().doubleValue())
        .originalPrice(book.getPrice().doubleValue())
        .discountPercent(book.getDiscountPercent().doubleValue())
        .stripeSessionId(sessionId)
        .build();

    kafkaProducerService.sendBookPurchaseEvent(event);
}
```

**ReviewController:**

Создание отзыва:

```java
@PostMapping("/{bookId}/reviews")
public ResponseEntity<ReviewResponse> createReview(@PathVariable Long bookId,
                                                   @RequestBody CreateReviewRequest request,
                                                   Authentication authentication) {
    ReviewResponse review = reviewService.createReview(bookId, request, authentication);

    // Отправка события отзыва
    BookReviewEvent event = BookReviewEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType("BOOK_REVIEW")
        .timestamp(LocalDateTime.now())
        .bookId(bookId)
        .userId(getCurrentUserId(authentication))
        .reviewId(review.getId())
        .action("CREATED")
        .reviewLength(request.getText().length())
        .build();

    kafkaProducerService.sendBookReviewEvent(event);

    return ResponseEntity.status(HttpStatus.CREATED).body(review);
}
```

Обновление отзыва:

```java
@PutMapping("/{bookId}/reviews/my")
public ResponseEntity<ReviewResponse> updateMyReview(@PathVariable Long bookId,
                                                      @RequestBody UpdateReviewRequest request,
                                                      Authentication authentication) {
    ReviewResponse review = reviewService.updateReview(bookId, request, authentication);

    // Отправка события обновления отзыва
    BookReviewEvent event = BookReviewEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType("BOOK_REVIEW")
        .timestamp(LocalDateTime.now())
        .bookId(bookId)
        .userId(getCurrentUserId(authentication))
        .reviewId(review.getId())
        .action("UPDATED")
        .reviewLength(request.getText().length())
        .build();

    kafkaProducerService.sendBookReviewEvent(event);

    return ResponseEntity.ok(review);
}
```

**RatingController:**

Создание рейтинга:

```java
@PostMapping("/{bookId}/ratings")
public ResponseEntity<RatingResponse> createRating(@PathVariable Long bookId,
                                                    @RequestBody CreateRatingRequest request,
                                                    Authentication authentication) {
    RatingResponse rating = ratingService.createRating(bookId, request, authentication);

    // Отправка события рейтинга
    BookRatingEvent event = BookRatingEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType("BOOK_RATING")
        .timestamp(LocalDateTime.now())
        .bookId(bookId)
        .userId(getCurrentUserId(authentication))
        .ratingId(rating.getId())
        .ratingValue(rating.getValue())
        .action("CREATED")
        .build();

    kafkaProducerService.sendBookRatingEvent(event);

    return ResponseEntity.status(HttpStatus.CREATED).body(rating);
}
```

Обновление рейтинга:

```java
@PutMapping("/{bookId}/ratings/my")
public ResponseEntity<RatingResponse> updateRating(@PathVariable Long bookId,
                                                   @RequestBody UpdateRatingRequest request,
                                                   Authentication authentication) {
    Long userId = getCurrentUserId(authentication);

    // Получаем старое значение рейтинга перед обновлением
    // (ratingRepository должен быть инжектирован в контроллер)
    Short oldRatingValue = ratingRepository.findByBookIdAndUserId(bookId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Rating not found"))
            .getValue();

    RatingResponse rating = ratingService.updateRating(bookId, userId, request);

    // Отправка события обновления рейтинга
    BookRatingEvent event = BookRatingEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType("BOOK_RATING")
        .timestamp(LocalDateTime.now())
        .bookId(bookId)
        .userId(userId)
        .ratingId(rating.getId())
        .ratingValue(rating.getValue())
        .oldRatingValue(oldRatingValue)
        .action("UPDATED")
        .build();

    kafkaProducerService.sendBookRatingEvent(event);

    return ResponseEntity.ok(rating);
}
```

#### 1.5. Конфигурация Kafka в application.properties

```properties
# Kafka Configuration
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.producer.acks=all
spring.kafka.producer.retries=3
```

---

### Шаг 2: Создание микросервиса аналитики

#### 2.1. Создание Spring Boot проекта

Использовать Spring Initializr с зависимостями:

- Spring Web
- Spring Kafka
- Lombok

#### 2.2. Структура моделей данных

Создать классы:

- `BookStatistics`
- `UserActivity`
- `ReviewStatistics`

#### 2.3. Создание сервиса аналитики

```java
@Service
@Slf4j
public class AnalyticsService {

    // In-memory storage
    private final ConcurrentHashMap<Long, BookStatistics> bookStats = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, UserActivity> userActivity = new ConcurrentHashMap<>();
    private final ReviewStatistics reviewStats = new ReviewStatistics();

    public void processBookView(BookViewEvent event) {
        BookStatistics stats = bookStats.computeIfAbsent(
            event.getBookId(),
            id -> new BookStatistics(id, event.getBookTitle(), event.getBookGenre())
        );

        stats.incrementViewCount();
        stats.addUniqueViewer(event.getUserId());
        stats.updateLastViewAt(event.getTimestamp());

        // Обновление активности пользователя
        if (event.getUserId() != null) {
            UserActivity activity = userActivity.computeIfAbsent(
                event.getUserId(),
                UserActivity::new
            );
            activity.incrementBooksViewed();
            activity.addViewedBook(event.getBookId());
        }
    }

    // Аналогично для других событий...
}
```

#### 2.4. Создание Kafka Consumers

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class BookViewConsumer {

    private final AnalyticsService analyticsService;

    @KafkaListener(
        topics = "book.views",
        groupId = "analytics-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeBookView(BookViewEvent event) {
        log.info("Received book view event: {}", event);
        try {
            analyticsService.processBookView(event);
        } catch (Exception e) {
            log.error("Error processing book view event: {}", event, e);
        }
    }
}
```

#### 2.5. Создание REST API

```java
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/books/{bookId}/stats")
    public ResponseEntity<BookStatisticsResponse> getBookStats(@PathVariable Long bookId) {
        BookStatistics stats = analyticsService.getBookStatistics(bookId);
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(BookStatisticsResponse.from(stats));
    }

    // ... другие эндпоинты
}
```

#### 2.6. Конфигурация Kafka Consumer

```properties
# Kafka Consumer Configuration
spring.kafka.bootstrap-servers=${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.consumer.group-id=${SPRING_KAFKA_CONSUMER_GROUP_ID:analytics-service-group}
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.enable-auto-commit=true
```

#### 2.7. Реализация Scheduled Task для агрегации

Создать класс `AnalyticsAggregationScheduler` (см. раздел "Реализация в микросервисе аналитики" выше)

#### 2.8. Добавление зависимости для Scheduling

В `pom.xml` добавить (обычно уже есть в Spring Boot):

```xml
<!-- Spring Scheduling уже включен в spring-boot-starter -->
```

Включить Scheduling в главном классе:

```java
@SpringBootApplication
@EnableScheduling
public class AnalyticsServiceApplication {
    // ...
}
```

---

### Шаг 3: Реализация обратного потока в основном приложении

#### 3.1. Создание Entity для сохранения статистики

Создать классы:

- `BookAnalytics` (см. раздел "Entity для сохранения статистики" выше)
- `SystemAnalytics` (см. раздел "Entity для сохранения статистики" выше)

#### 3.2. Создание Repository

Создать интерфейсы:

- `BookAnalyticsRepository`
- `SystemAnalyticsRepository`

#### 3.3. Создание Kafka Consumer

Создать класс `AnalyticsStatsConsumer` (см. раздел "Kafka Consumer в основном приложении" выше)

#### 3.4. Создание DTO для агрегированных данных

Создать классы в пакете `dto.analytics`:

- `BookStatisticsAggregated`
- `SystemOverviewAggregated`
- `PopularBooksAggregated`

#### 3.5. Конфигурация Kafka Consumer

Добавить в `application.properties`:

```properties
# Kafka Consumer для получения агрегированных данных
spring.kafka.consumer.group-id=main-app-analytics-group
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.enable-auto-commit=true
```

#### 3.6. Создание SQL миграций

Создать файл миграции (Flyway/Liquibase) или выполнить SQL вручную (см. раздел "SQL миграции" выше)

#### 3.7. Создание Admin API

Создать контроллер `AnalyticsAdminController` (см. раздел "Реализация контроллера для админ панели" выше)

#### 3.8. Создание Response DTO

Создать классы для ответов:

- `BookAnalyticsResponse`
- `BookAnalyticsHistoryResponse`
- `SystemAnalyticsResponse`
- `SystemAnalyticsHistoryResponse`
- `PopularBooksResponse`

---

### Шаг 4: Настройка Docker

#### 4.1. Dockerfile для analytics-service

```dockerfile
FROM openjdk:21-jdk-slim

WORKDIR /app

COPY target/analytics-service-*.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 4.2. Обновление docker-compose.yml основного приложения

Добавить Kafka и Zookeeper в существующий docker-compose.yml или использовать отдельный файл.

---

### Шаг 4: Запуск и тестирование

#### 4.1. Запуск инфраструктуры

```bash
# Запуск Kafka, Zookeeper, Kafka UI
docker-compose -f docker-compose.analytics.yml up -d

# Проверка статуса
docker-compose -f docker-compose.analytics.yml ps
```

#### 4.2. Создание топиков (опционально, если auto-create отключен)

```bash
# Вход в контейнер Kafka
docker exec -it kafka bash

# Создание топиков
kafka-topics --create --topic book.views --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics --create --topic book.downloads --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics --create --topic book.purchases --bootstrap-server localhost:9092 --partitions 2 --replication-factor 1
kafka-topics --create --topic book.reviews --bootstrap-server localhost:9092 --partitions 2 --replication-factor 1
kafka-topics --create --topic book.ratings --bootstrap-server localhost:9092 --partitions 2 --replication-factor 1
kafka-topics --create --topic analytics.aggregated-stats --bootstrap-server localhost:9092 --partitions 2 --replication-factor 1

# Просмотр топиков
kafka-topics --list --bootstrap-server localhost:9092
```

#### 4.3. Запуск основного приложения

```bash
# Запуск spring-digital-bookstore
mvn spring-boot:run
# или через Docker
```

#### 4.4. Запуск микросервиса аналитики

```bash
# Сборка
cd analytics-service
mvn clean package

# Запуск
java -jar target/analytics-service-*.jar
# или через Docker
docker-compose -f docker-compose.analytics.yml up analytics-service
```

#### 4.5. Тестирование

1. **Открыть Kafka UI:** http://localhost:8089
2. **Выполнить действия в основном приложении:**

   - Просмотреть книгу
   - Скачать книгу
   - Купить книгу
   - Создать отзыв
   - Создать рейтинг

3. **Проверить в Kafka UI:**

   - Сообщения в топиках
   - Партиции
   - Consumer groups
   - Offsets

4. **Проверить API аналитики (микросервис):**

   ```bash
   # Статистика по книге
   curl http://localhost:8090/api/analytics/books/1/stats

   # Популярные книги
   curl http://localhost:8090/api/analytics/books/popular?limit=10

   # Активность пользователя
   curl http://localhost:8090/api/analytics/users/5/activity

   # Общая статистика
   curl http://localhost:8090/api/analytics/overview
   ```

5. **Подождать интервал агрегации** (по умолчанию 1 минута, см. раздел "Тестирование обратного потока данных" для инструкции по изменению) и проверить обратный поток:

   - В Kafka UI проверить топик `analytics.aggregated-stats`
   - Должны появиться сообщения с ключами: `BOOK_STATS`, `SYSTEM_OVERVIEW`, `POPULAR_BOOKS`

6. **Проверить, что данные сохранились в БД основного приложения:**

   ```sql
   -- Проверить статистику по книгам
   SELECT * FROM book_analytics ORDER BY aggregated_at DESC LIMIT 10;

   -- Проверить общую статистику
   SELECT * FROM system_analytics ORDER BY aggregated_at DESC LIMIT 5;
   ```

7. **Проверить Admin API (основное приложение):**

   ```bash
   # Получить статистику по книге (требуется авторизация ADMIN)
   curl -H "Authorization: Bearer <admin_token>" \
        http://localhost:8080/api/v1/admin/analytics/books/1

   # Получить общую статистику
   curl -H "Authorization: Bearer <admin_token>" \
        http://localhost:8080/api/v1/admin/analytics/overview

   # Получить популярные книги
   curl -H "Authorization: Bearer <admin_token>" \
        http://localhost:8080/api/v1/admin/analytics/popular?limit=10&sortBy=views

   # Получить историю статистики по книге
   curl -H "Authorization: Bearer <admin_token>" \
        "http://localhost:8080/api/v1/admin/analytics/books/1/history?startDate=2025-12-17T00:00:00&endDate=2025-12-17T23:59:59"
   ```

---

## Сценарии для тестирования

### 1. Базовое тестирование

**Цель:** Проверить, что события попадают в Kafka и обрабатываются

**Шаги:**

1. Выполнить действие в основном приложении (например, просмотреть книгу)
2. Проверить в Kafka UI, что сообщение появилось в топике `book.views`
3. Проверить, что статистика обновилась через API аналитики

---

### 2. Тестирование партиций

**Цель:** Понять, как работает партиционирование

**Шаги:**

1. Отправить несколько событий с разными ключами (bookId)
2. В Kafka UI посмотреть, в какие партиции попали сообщения
3. Проверить, что события с одинаковым ключом попадают в одну партицию

**Ожидаемый результат:**

- События с `bookId=1` всегда в одной партиции
- События с `bookId=2` могут быть в другой партиции

---

### 3. Тестирование Consumer Groups

**Цель:** Понять, как работают consumer groups

**Шаги:**

1. Запустить два экземпляра микросервиса аналитики (разные порты)
2. Отправить события
3. Проверить в Kafka UI, как распределяются сообщения между consumers

**Ожидаемый результат:**

- Сообщения распределяются между consumers в группе
- Каждое сообщение обрабатывается только одним consumer

---

### 4. Тестирование обработки ошибок

**Цель:** Проверить поведение при ошибках обработки

**Шаги:**

1. Отправить некорректное сообщение в топик
2. Проверить логи микросервиса
3. Проверить, что сервис продолжает работать

---

### 5. Тестирование производительности

**Цель:** Проверить обработку большого количества событий

**Шаги:**

1. Отправить 1000+ событий
2. Проверить, что все события обработаны
3. Проверить метрики в Kafka UI (lag, throughput)

---

### 6. Тестирование перезапуска сервиса

**Цель:** Проверить, что сервис продолжает с правильного offset

**Шаги:**

1. Отправить несколько событий
2. Остановить микросервис
3. Отправить еще события
4. Запустить микросервис
5. Проверить, что обработаны все события (и старые, и новые)

---

### 7. Тестирование обратного потока данных (Analytics → Main App)

**Цель:** Проверить, что агрегированные данные возвращаются в основное приложение

**Важно:** Агрегация происходит каждую 1 минуту по умолчанию. Для тестирования рекомендуется уменьшить интервал.

**Как изменить интервал агрегации для тестирования:**

В микросервисе аналитики в классе `AnalyticsAggregationScheduler` измените значение:

```java
// Для быстрого тестирования (каждые 10 секунд)
@Scheduled(fixedRate = 10000) // 10 секунд

// Или каждые 30 секунд
@Scheduled(fixedRate = 30000) // 30 секунд

// По умолчанию (для production)
@Scheduled(fixedRate = 60000) // 1 минута
```

**Шаги:**

1. Выполнить несколько действий в основном приложении (просмотры, скачивания, покупки)
2. **Подождать интервал агрегации** (по умолчанию 1 минута, или измененный интервал для тестирования)
3. Проверить в Kafka UI топик `analytics.aggregated-stats`:
   - Должны быть сообщения с ключами: `BOOK_STATS`, `SYSTEM_OVERVIEW`, `POPULAR_BOOKS`
   - Просмотреть содержимое сообщений
4. Проверить логи основного приложения:
   - Должны быть записи о получении агрегированных данных
   - Должны быть записи о сохранении в БД
5. Проверить БД:
   ```sql
   SELECT * FROM book_analytics ORDER BY aggregated_at DESC LIMIT 5;
   SELECT * FROM system_analytics ORDER BY aggregated_at DESC LIMIT 1;
   ```
6. Проверить Admin API:
   ```bash
   curl -H "Authorization: Bearer <admin_token>" \
        http://localhost:8080/api/v1/admin/analytics/overview
   ```

**Ожидаемый результат:**

- Данные успешно отправляются из микросервиса в Kafka
- Основное приложение получает и сохраняет данные в БД
- Admin API возвращает актуальную статистику

---

### 8. Тестирование истории статистики

**Цель:** Проверить, что сохраняется история статистики

**Шаги:**

1. Подождать несколько циклов агрегации:
   - **По умолчанию:** 3 × 1 минута = 3 минуты
   - **Для тестирования (если изменили интервал):** 3 × 10 секунд = 30 секунд
2. Проверить, что в БД есть несколько записей с разными `aggregated_at`
3. Запросить историю через API:
   ```bash
   curl -H "Authorization: Bearer <admin_token>" \
        "http://localhost:8080/api/v1/admin/analytics/books/1/history"
   ```
4. Проверить, что возвращается массив с историей

**Ожидаемый результат:**

- В БД накапливается история статистики
- API возвращает историю за период

---

## Преимущества хранения в оперативной памяти

1. **Производительность:** Очень быстрый доступ к данным
2. **Простота:** Не нужна дополнительная БД
3. **Достаточно для pet проекта:** Для демонстрации работы Kafka этого хватает
4. **Легко сбросить:** Перезапуск сервиса = чистый старт

## Недостатки (для понимания)

1. **Потеря данных при перезапуске:** Все данные теряются
2. **Ограничение памяти:** При большом объеме данных может закончиться память
3. **Нет персистентности:** Данные не сохраняются между перезапусками

**Для pet проекта это нормально!** Это позволяет сфокусироваться на изучении Kafka, а не на настройке БД.

---

## Дополнительные возможности (опционально)

### 1. Экспорт статистики

Добавить эндпоинт для экспорта статистики в JSON/CSV:

```
GET /api/analytics/export?format=json
GET /api/analytics/export?format=csv
```

### 2. WebSocket для real-time обновлений

Подключить WebSocket для отправки статистики в реальном времени на фронтенд.

### 3. Метрики Prometheus

Добавить метрики Prometheus для мониторинга:

- Количество обработанных событий
- Размер хранилища
- Время обработки событий

### 4. Health checks

Расширить health endpoint с информацией о:

- Количестве обработанных событий
- Размере хранилища
- Статусе подключения к Kafka

---

## Заключение

Этот план реализации позволяет:

1. ✅ Интегрировать Kafka в существующее приложение
2. ✅ Создать отдельный микросервис для аналитики
3. ✅ Использовать Kafka UI для визуализации
4. ✅ Тестировать различные аспекты Kafka:
   - Топики и партиции
   - Consumer groups
   - Обработку событий
   - Производительность
   - Двусторонний поток данных (Main App → Analytics → Main App)
5. ✅ Хранить данные в оперативной памяти в микросервисе (просто для pet проекта)
6. ✅ Сохранять агрегированные данные в БД основного приложения
7. ✅ Предоставлять Admin API для просмотра статистики в админ панели

### Полный цикл данных

1. **Основное приложение** → отправляет события в Kafka
2. **Микросервис аналитики** → получает события, обрабатывает, хранит в памяти
3. **Микросервис аналитики** → периодически (каждую 1 минуту) агрегирует и отправляет обратно в Kafka
4. **Основное приложение** → получает агрегированные данные, сохраняет в БД
5. **Админ панель** → получает данные через REST API из БД

Этот подход демонстрирует:

- ✅ Producer-Consumer паттерн
- ✅ Обработку событий в реальном времени
- ✅ Агрегацию данных
- ✅ Обратный поток данных через Kafka
- ✅ Сохранение данных в БД
- ✅ API для доступа к данным
