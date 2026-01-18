# План внедрения ELK стека для сбора и анализа логов

## Обзор

ELK стек (Elasticsearch, Logstash, Kibana) - это комплексное решение для централизованного сбора, обработки, хранения и визуализации логов приложения.

**Архитектура потока логов:**
```
main-app          → Logback (JSON) → 
                                    ↓
analytics-service → Logback (JSON) → Logstash → Elasticsearch → Kibana (визуализация)
```

**Оба сервиса отправляют логи в один Logstash, но с полем `service` для различения.**

**Преимущества:**
- Централизованное хранение всех логов
- Мощный поиск и фильтрация
- Визуализация логов через Kibana
- Анализ производительности и ошибок
- Корреляция событий между сервисами

---

## ❓ Часто задаваемые вопросы

### Запустится ли приложение без ELK стека?

**Ответ: ДА, приложение запустится и будет работать!**

**При правильной настройке:**
- ✅ Приложение запускается без контейнеров ELK стека
- ✅ Логи идут в консоль (как сейчас, по умолчанию)
- ✅ При запуске ELK стека логи автоматически начинают отправляться в Logstash
- ✅ При недоступности Logstash приложение продолжает работать, логи идут в консоль

**Что для этого нужно:**
1. В `logback-spring.xml` использовать `ignoreExceptions="true"` для Logstash appender
2. В `docker-compose.yml` НЕ добавлять `depends_on: logstash` для backend
3. Настроить fallback на ConsoleAppender

**Пример запуска:**
```bash
# Запуск БЕЗ ELK стека - оба приложения работают нормально
docker-compose up postgres kafka backend analytics-service

# Запуск С ELK стеком - логи из обоих сервисов отправляются в Logstash
docker-compose up postgres kafka backend analytics-service elasticsearch logstash kibana
```

---

## Шаг 1: Подготовка и анализ текущего состояния

### 1.1 Проверка текущего логирования

**Что проверить:**
- Какие контроллеры уже используют логирование (`@Slf4j` или `LoggerFactory`)
- Какие уровни логирования настроены в `application.properties`
- Есть ли файл `logback-spring.xml` или используется дефолтная конфигурация

**Действия:**
1. Откройте `main-app/src/main/resources/application.properties`
2. Откройте `analytics-service/src/main/resources/application.properties`
3. Проверьте настройки `logging.level.*` в обоих файлах
4. Найдите все классы с логированием:
   - `grep -r "@Slf4j\|Logger" main-app/src/main/java/`
   - `grep -r "@Slf4j\|Logger" analytics-service/src/main/java/`

**Текущее состояние (из анализа кодовой базы):**

**main-app:**
- ✅ `BookController` уже использует `@Slf4j`
- ✅ `StripeService` использует `LoggerFactory.getLogger()`
- ✅ В `application.properties` настроены уровни логирования для разных пакетов
- ❌ Нет `logback-spring.xml` (используется дефолтная конфигурация)

**analytics-service:**
- ✅ Множество классов используют `@Slf4j` (AnalyticsService, Scheduler, Consumers)
- ✅ В `application.properties` настроены уровни логирования
- ❌ Нет `logback-spring.xml` (используется дефолтная конфигурация)

### 1.2 Определение требований

**Что логировать:**
- HTTP запросы (метод, URL, статус, время выполнения)
- Ошибки и исключения
- Бизнес-события (покупки, регистрации, и т.д.)
- SQL запросы (опционально, для разработки)
- Kafka события (уже частично логируется)

**Формат логов:**
- JSON формат для удобного парсинга в Logstash
- Структурированные поля: timestamp, level, logger, message, thread, и т.д.

---

## Шаг 2: Настройка Logback для JSON формата

### 2.1 Создание файлов logback-spring.xml

**⚠️ ВАЖНО: Нужно создать конфигурацию для ОБОИХ сервисов!**

#### 2.1.1 Для main-app

**Расположение:** `main-app/src/main/resources/logback-spring.xml`

**Что должно быть в файле:**
1. **JSON Encoder** - для форматирования логов в JSON
2. **Console Appender** - для вывода в консоль (для разработки)
3. **File Appender** - для сохранения логов в файл (опционально)
4. **Logstash TCP Appender** - для отправки логов в Logstash
5. **Профили для разных окружений** (dev, prod)

**Структура JSON лога для main-app:**
```json
{
  "timestamp": "2024-01-15T10:30:00.123Z",
  "level": "INFO",
  "logger": "online.ityura.springdigitallibrary.controller.BookController",
  "message": "Book retrieved successfully",
  "thread": "http-nio-8080-exec-1",
  "application": "spring-digital-library",
  "service": "main-app",
  "traceId": "abc123",
  "spanId": "def456"
}
```

#### 2.1.2 Для analytics-service

**Расположение:** `analytics-service/src/main/resources/logback-spring.xml`

**Структура JSON лога для analytics-service:**
```json
{
  "timestamp": "2024-01-15T10:30:00.123Z",
  "level": "INFO",
  "logger": "online.ityura.analytics.service.AnalyticsService",
  "message": "Statistics aggregated successfully",
  "thread": "scheduler-1",
  "application": "analytics-service",
  "service": "analytics-service"
}
```

**Важно:** Оба сервиса должны отправлять логи в один Logstash, но с полем `service` для различения. В Logstash можно будет фильтровать и создавать отдельные индексы для каждого сервиса.

### 2.2 Добавление зависимостей (если нужны)

**Проверьте pom.xml для ОБОИХ сервисов:**
- Spring Boot уже включает `spring-boot-starter-logging` (Logback + SLF4J)
- Для JSON формата может понадобиться `net.logstash.logback:logstash-logback-encoder`

**Если зависимости нет, добавить:**

**В `main-app/pom.xml`:**
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

**В `analytics-service/pom.xml`:**
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

---

## Шаг 3: Настройка Docker контейнеров для ELK стека

### 3.1 Добавление сервисов в docker-compose.yml

**Файл для редактирования:** `for_local_run_only/docker-compose.yml` (или другой, в зависимости от окружения)

**Нужно добавить 3 сервиса:**

#### 3.1.1 Elasticsearch
- **Порт:** 9200 (HTTP API), 9300 (транспорт)
- **Volumes:** для хранения данных
- **Environment:** настройки памяти, кластера
- **Healthcheck:** проверка готовности

#### 3.1.2 Logstash
- **Порт:** 5044 (Beats), 5000 (TCP), 9600 (API)
- **Volumes:** конфигурационные файлы pipeline
- **Depends_on:** Elasticsearch
- **Конфигурация:** pipeline для приема и обработки логов

#### 3.1.3 Kibana
- **Порт:** 5601 (веб-интерфейс)
- **Depends_on:** Elasticsearch
- **Environment:** URL Elasticsearch

### 3.2 Создание конфигурации Logstash

**Файл:** `infra/logstash/pipeline/logstash.conf` (или в отдельной директории)

**Что должно быть:**
1. **Input** - прием логов от Spring Boot (TCP или HTTP)
2. **Filter** - парсинг JSON, добавление полей, фильтрация
3. **Output** - отправка в Elasticsearch

**Структура pipeline:**
```
input {
  tcp {
    port => 5000
    codec => json_lines
  }
}

filter {
  # Парсинг JSON (если приходит как строка)
  json {
    source => "message"
  }
  
  # Добавление метаданных
  mutate {
    add_field => { "[@metadata][index]" => "%{service}-%{+YYYY.MM.dd}" }
  }
  
  # Фильтрация чувствительных данных (пароли, токены)
  # mutate {
  #   gsub => ["message", "(password|token|secret)=[^&\\s]+", "\\1=***"]
  # }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    # Динамический индекс на основе поля service
    index => "%{[@metadata][index]}"
    # Или можно использовать фиксированные индексы:
    # index => "%{service}-logs-%{+YYYY.MM.dd}"
  }
  
  # Fallback на stdout для отладки (опционально)
  stdout {
    codec => rubydebug
  }
}
```

**Результат:**
- Оба сервиса (`main-app` и `analytics-service`) отправляют логи на один порт Logstash (5000)
- Logstash различает сервисы по полю `service` (добавляется в logback-spring.xml каждого сервиса)
- Логи из `main-app` будут в индексе: `main-app-logs-2024.01.15` (или `spring-digital-library-2024.01.15`)
- Логи из `analytics-service` будут в индексе: `analytics-service-logs-2024.01.15`
- В Kibana можно фильтровать по полю `service` или создать отдельные index patterns для каждого сервиса

### 3.3 Настройка сети Docker

**Проверьте:**
- Все сервисы должны быть в одной сети (`spring-library-network`)
- Backend и analytics-service должны иметь доступ к Logstash по имени хоста `logstash`
- ⚠️ **ВАЖНО:** 
  - Backend НЕ должен иметь `depends_on: logstash` - он должен запускаться независимо!
  - Analytics-service НЕ должен иметь `depends_on: logstash` - он должен запускаться независимо!
  - Оба сервиса должны работать без ELK стека (логи будут идти в консоль)

---

## Шаг 4: Настройка Spring Boot приложений

### 4.1 Обновление application.properties

**⚠️ ВАЖНО: Нужно обновить для ОБОИХ сервисов!**

#### 4.1.1 Для main-app

**Файл:** `main-app/src/main/resources/application.properties`

**Добавить настройки:**
```properties
# ELK Stack настройки
logging.logstash.enabled=true
logging.logstash.host=${LOGSTASH_HOST:logstash}
logging.logstash.port=${LOGSTASH_PORT:5000}
```

#### 4.1.2 Для analytics-service

**Файл:** `analytics-service/src/main/resources/application.properties`

**Добавить настройки:**
```properties
# ELK Stack настройки
logging.logstash.enabled=true
logging.logstash.host=${LOGSTASH_HOST:logstash}
logging.logstash.port=${LOGSTASH_PORT:5000}
```

### 4.2 Обновление переменных окружения в docker-compose.yml

**⚠️ ВАЖНО: Нужно добавить для ОБОИХ сервисов!**

**Для сервиса `backend` добавить:**
```yaml
environment:
  # ... существующие переменные ...
  LOGSTASH_HOST: logstash
  LOGSTASH_PORT: 5000
```

**Для сервиса `analytics-service` добавить:**
```yaml
analytics-service:
  # ... существующие настройки ...
  environment:
    # ... существующие переменные ...
    LOGSTASH_HOST: logstash
    LOGSTASH_PORT: 5000
  # ⚠️ ВАЖНО: НЕ добавлять depends_on: logstash - сервис должен запускаться независимо!
```

### 4.3 Настройка профилей Spring

**В logback-spring.xml для ОБОИХ сервисов использовать профили:**
- `dev` - логи в консоль + файл (без Logstash, для локальной разработки)
- `prod` - логи в Logstash + файл (для продакшена)

**Важно:** Оба сервиса должны использовать одинаковую структуру профилей для консистентности.

### 4.4 ⚠️ ВАЖНО: Обеспечение работы приложения БЕЗ ELK стека

**Критически важно:** Приложение должно запускаться и работать, даже если контейнеры ELK стека не запущены!

**Что нужно сделать:**

1. **Logstash Appender должен быть опциональным:**
   - Использовать `ignoreExceptions="true"` в Logstash appender
   - Или использовать AsyncAppender с fallback на ConsoleAppender
   - При недоступности Logstash логи должны автоматически идти в консоль/файл

2. **Backend НЕ должен зависеть от Logstash:**
   - В `docker-compose.yml` НЕ добавлять `depends_on: logstash` для сервиса `backend`
   - Backend должен запускаться независимо от ELK стека

3. **Настройка fallback в logback-spring.xml:**
   ```xml
   <!-- Logstash appender с игнорированием ошибок -->
   <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
     <destination>${LOGSTASH_HOST:-logstash}:${LOGSTASH_PORT:-5000}</destination>
     <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
     <keepAliveDuration>5 minutes</keepAliveDuration>
     <reconnectionDelay>10 seconds</reconnectionDelay>
     <connectionTimeout>5 seconds</connectionTimeout>
     <!-- Игнорировать ошибки подключения - приложение не должно падать -->
     <ignoreExceptions>true</ignoreExceptions>
   </appender>
   ```

4. **Альтернатива - условная активация через профили:**
   - Использовать Spring профили для активации Logstash appender только когда нужно
   - По умолчанию (без профиля) - только консоль и файл

**Результат:**
- ✅ Приложение запускается без ELK стека
- ✅ Логи идут в консоль (как сейчас)
- ✅ При запуске ELK стека логи автоматически начинают отправляться в Logstash
- ✅ При недоступности Logstash приложение продолжает работать, логи идут в консоль

---

## Шаг 5: Улучшение логирования в контроллерах

### 5.1 Добавление логирования в контроллеры

**Что логировать:**
- Входящие HTTP запросы (метод, URL, параметры)
- Исходящие ответы (статус код, время выполнения)
- Ошибки и исключения
- Бизнес-события

**Подход:**
- Использовать `@Slf4j` (Lombok) - не нужно создавать Logger вручную
- Добавить логирование в методы контроллеров
- Использовать структурированное логирование (MDC для traceId)

### 5.2 Создание Interceptor для автоматического логирования запросов

**Опционально, но рекомендуется:**
- Создать `LoggingInterceptor` для автоматического логирования всех HTTP запросов
- Добавить traceId для корреляции логов одного запроса
- Логировать время выполнения запроса

**Файл:** `main-app/src/main/java/online/ityura/springdigitallibrary/config/LoggingInterceptor.java`

---

## Шаг 6: Тестирование и проверка

### 6.1 Запуск ELK стека

**Команды:**
```bash
# Запуск только ELK стека (для проверки)
docker-compose up elasticsearch logstash kibana

# Или запуск всего стека (включая оба приложения)
docker-compose up -d

# Или запуск с указанием конкретных сервисов
docker-compose up postgres kafka backend analytics-service elasticsearch logstash kibana
```

**Проверка:**
1. Elasticsearch: `http://localhost:9200` - должен вернуть JSON с информацией о кластере
2. Logstash: проверить логи контейнера - должен подключиться к Elasticsearch
3. Kibana: `http://localhost:5601` - должен открыться веб-интерфейс

### 6.2 Проверка отправки логов

**Действия:**
1. Запустить оба Spring Boot приложения (main-app и analytics-service)
2. Выполнить несколько HTTP запросов к API main-app
3. Дождаться событий в analytics-service (через Kafka)
4. Проверить в Kibana:
   - Создать index patterns:
     - `spring-digital-library-*` или `main-app-*` (для main-app)
     - `analytics-service-*` (для analytics-service)
   - Или создать один общий pattern: `*-logs-*` (если используете единый формат)
   - Открыть Discover - должны появиться логи из обоих сервисов
   - Фильтровать по полю `service` для просмотра логов конкретного сервиса

### 6.3 Проверка формата логов

**В Kibana проверить:**
- Поля логов отображаются корректно
- Timestamp парсится правильно
- Уровни логирования видны
- Поиск работает
- **Поле `service` присутствует и различает сервисы:**
  - `main-app` для основного приложения
  - `analytics-service` для сервиса аналитики
- Можно фильтровать логи по полю `service` в Discover

### 6.4 Проверка логов обоих сервисов

**Действия:**
1. Выполнить запросы к main-app (например, GET /api/v1/books)
2. Дождаться обработки событий в analytics-service (через Kafka)
3. В Kibana проверить:
   - Логи из main-app появляются с `service: "main-app"`
   - Логи из analytics-service появляются с `service: "analytics-service"`
   - Оба типа логов видны в одном индексе или в разных (в зависимости от настройки Logstash)

---

## Шаг 7: Настройка Kibana Dashboard

### 7.1 Создание Index Patterns

**В Kibana:**
1. Management → Stack Management → Index Patterns
2. Создать index patterns для каждого сервиса:
   - **Pattern 1:** `spring-digital-library-*` или `main-app-*` (для main-app)
     - Time field: `@timestamp`
   - **Pattern 2:** `analytics-service-*` (для analytics-service)
     - Time field: `@timestamp`
3. Или создать один общий pattern: `*-logs-*` (если используете единый формат именования)
4. Create patterns

### 7.2 Создание визуализаций

**Рекомендуемые визуализации:**

**Общие (для всех сервисов):**
- **Logs Timeline** - временная шкала всех логов
- **Logs by Service** - распределение логов по сервисам (main-app, analytics-service)
- **Logs by Level** - распределение по уровням (INFO, WARN, ERROR) с группировкой по сервисам
- **Error Rate by Service** - количество ошибок по времени для каждого сервиса

**Для main-app:**
- **Request Duration** - время выполнения запросов
- **Top Errors** - топ ошибок
- **HTTP Status Codes** - распределение HTTP статусов

**Для analytics-service:**
- **Kafka Consumer Lag** - задержка обработки событий
- **Aggregation Performance** - производительность агрегации статистики
- **Event Processing Rate** - скорость обработки событий

### 7.3 Создание Dashboard

**Создать Dashboard с:**

**Общий Dashboard:**
- Обзор всех логов (main-app + analytics-service)
- Распределение логов по сервисам
- Общие метрики производительности
- Мониторинг ошибок по сервисам

**Dashboard для main-app:**
- Статистика по эндпоинтам
- Время выполнения запросов
- HTTP статусы
- Ошибки и исключения

**Dashboard для analytics-service:**
- Производительность Kafka consumers
- Скорость обработки событий
- Метрики агрегации
- Health check статус

---

## Шаг 8: Оптимизация и настройка производительности

### 8.1 Настройка ротации индексов в Elasticsearch

**Проблема:** Без ротации индексы будут расти бесконечно

**Решение:** Настроить Index Lifecycle Management (ILM) в Elasticsearch
- Удалять старые индексы (например, старше 30 дней)
- Оптимизировать размер индексов

### 8.2 Настройка буферизации в Logstash

**В logstash.conf:**
- Настроить batch size для эффективной отправки в Elasticsearch
- Настроить retry при ошибках

### 8.3 Настройка памяти для Elasticsearch

**В docker-compose.yml:**
- Установить `ES_JAVA_OPTS=-Xms1g -Xmx1g` (в зависимости от ресурсов)
- Для продакшена рекомендуется минимум 2GB

---

## Шаг 9: Безопасность (опционально, для продакшена)

### 9.1 Аутентификация в Kibana

**Для продакшена:**
- Настроить базовую аутентификацию или OAuth
- Ограничить доступ к Kibana

### 9.2 Шифрование трафика

**Опционально:**
- TLS для связи между Logstash и Elasticsearch
- HTTPS для Kibana

### 9.3 Фильтрация чувствительных данных

**В Logstash filter:**
- Удалять пароли, токены, секретные ключи из логов
- Маскировать email адреса (опционально)

---

## Шаг 10: Мониторинг и алертинг (опционально)

### 10.1 Настройка алертов в Kibana

**Создать правила:**
- Алерт при большом количестве ошибок
- Алерт при медленных запросах
- Алерт при недоступности сервиса

### 10.2 Интеграция с внешними системами

**Опционально:**
- Отправка алертов в Slack/Telegram
- Интеграция с PagerDuty
- Экспорт метрик в Prometheus

---

## Чеклист внедрения

### Подготовка
- [ ] Проанализировать текущее логирование
- [ ] Определить требования к логированию
- [ ] Проверить зависимости в pom.xml

### Настройка Logback
- [ ] Создать `logback-spring.xml` для main-app
- [ ] Создать `logback-spring.xml` для analytics-service
- [ ] Настроить JSON encoder в обоих файлах
- [ ] Настроить Logstash appender в обоих файлах
- [ ] Добавить поле `service` для различения сервисов
- [ ] Добавить профили для разных окружений
- [ ] Протестировать формат логов обоих сервисов

### Docker контейнеры
- [ ] Добавить Elasticsearch в docker-compose.yml
- [ ] Добавить Logstash в docker-compose.yml
- [ ] Добавить Kibana в docker-compose.yml
- [ ] Создать конфигурацию Logstash pipeline
- [ ] Настроить volumes для данных
- [ ] Настроить сеть Docker

### Настройка приложения
- [ ] Обновить application.properties для main-app
- [ ] Обновить application.properties для analytics-service
- [ ] Добавить переменные окружения LOGSTASH_HOST/PORT для backend в docker-compose
- [ ] Добавить переменные окружения LOGSTASH_HOST/PORT для analytics-service в docker-compose
- [ ] Настроить профили Spring для обоих сервисов

### Улучшение логирования
- [ ] Добавить `@Slf4j` в контроллеры (где отсутствует)
- [ ] Добавить логирование важных событий
- [ ] Создать LoggingInterceptor (опционально)
- [ ] Настроить MDC для traceId

### Тестирование
- [ ] Запустить ELK стек
- [ ] Проверить подключение между сервисами
- [ ] Проверить отправку логов из приложения
- [ ] Проверить отображение логов в Kibana
- [ ] Протестировать поиск и фильтрацию

### Настройка Kibana
- [ ] Создать index patterns для обоих сервисов (или один общий)
- [ ] Создать визуализации для main-app
- [ ] Создать визуализации для analytics-service
- [ ] Создать общий dashboard
- [ ] Создать отдельные dashboards для каждого сервиса
- [ ] Настроить сохраненные поиски с фильтрацией по полю `service`

### Оптимизация
- [ ] Настроить ротацию индексов
- [ ] Настроить буферизацию Logstash
- [ ] Настроить память для Elasticsearch
- [ ] Протестировать производительность

### Безопасность (для продакшена)
- [ ] Настроить аутентификацию Kibana
- [ ] Настроить фильтрацию чувствительных данных
- [ ] Настроить шифрование (опционально)

---

## Порядок выполнения шагов

**Рекомендуемая последовательность:**

1. **Шаг 1** - Подготовка (анализ текущего состояния)
2. **Шаг 2** - Настройка Logback (создание logback-spring.xml)
3. **Шаг 3** - Docker контейнеры (добавление ELK в docker-compose)
4. **Шаг 4** - Настройка приложения (application.properties)
5. **Шаг 5** - Улучшение логирования (добавление логов в контроллеры)
6. **Шаг 6** - Тестирование (проверка работы)
7. **Шаг 7** - Настройка Kibana (визуализации и dashboard)
8. **Шаг 8** - Оптимизация (производительность)
9. **Шаг 9** - Безопасность (если нужно для продакшена)
10. **Шаг 10** - Мониторинг (опционально)

---

## Важные замечания

### Что НЕ нужно делать:
- ❌ Создавать Logger вручную в каждом контроллере - используйте `@Slf4j`
- ❌ Писать логи напрямую в файлы для ELK - Logstash соберет их автоматически
- ❌ Настраивать прямое подключение к Elasticsearch из приложения - используйте Logstash
- ❌ Логировать пароли, токены, секретные ключи - фильтруйте их в Logstash
- ❌ Делать backend зависимым от logstash в docker-compose.yml (`depends_on: logstash`) - приложение должно работать без ELK стека!

### Рекомендации:
- ✅ Используйте структурированное логирование (JSON)
- ✅ Добавляйте traceId для корреляции логов одного запроса
- ✅ Логируйте важные бизнес-события
- ✅ Настройте ротацию индексов для управления размером хранилища
- ✅ Тестируйте на локальной среде перед деплоем в продакшен
- ✅ **ОБЯЗАТЕЛЬНО:** Настройте fallback - приложение должно работать без ELK стека
- ✅ Используйте `ignoreExceptions="true"` в Logstash appender для устойчивости

---

## Дополнительные ресурсы

- [Elasticsearch Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Logstash Documentation](https://www.elastic.co/guide/en/logstash/current/index.html)
- [Kibana Documentation](https://www.elastic.co/guide/en/kibana/current/index.html)
- [Logback Documentation](http://logback.qos.ch/documentation.html)
- [Logstash Logback Encoder](https://github.com/logfellow/logstash-logback-encoder)

---

## Поддержка

При возникновении проблем:
1. Проверьте логи контейнеров: `docker-compose logs elasticsearch logstash kibana`
2. Проверьте подключение между сервисами
3. Проверьте формат логов в Kibana
4. Проверьте конфигурацию Logstash pipeline
