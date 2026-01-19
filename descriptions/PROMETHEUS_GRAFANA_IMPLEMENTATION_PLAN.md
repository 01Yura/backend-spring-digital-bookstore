# План внедрения Prometheus и Grafana для мониторинга метрик и ошибок

## Общая информация

**Цель:** Настроить мониторинг метрик и ошибок сервера (400, 500 и т.д.) с использованием Prometheus и Grafana для локального запуска через docker-compose.yml.

**Сервисы для мониторинга:**
- `backend` (main-app) - порт 8088
- `analytics-service` - порт 8090

---

## ШАГ 1: Добавление зависимостей Spring Boot Actuator

### 1.1. Обновить `main-app/pom.xml`

**Файл:** `main-app/pom.xml`

**Действие:** Добавить зависимости в секцию `<dependencies>`:

```xml
<!-- Spring Boot Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer Prometheus Registry -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Проверка:** После добавления выполнить `mvn clean install` для проверки зависимостей.

---

### 1.2. Обновить `analytics-service/pom.xml`

**Файл:** `analytics-service/pom.xml`

**Действие:** Добавить те же зависимости в секцию `<dependencies>`:

```xml
<!-- Spring Boot Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer Prometheus Registry -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Проверка:** После добавления выполнить `mvn clean install` для проверки зависимостей.

---

## ШАГ 2: Настройка Actuator в application.properties

### 2.1. Настроить Actuator для main-app

**Файл:** `main-app/src/main/resources/application.properties`

**Действие:** Добавить в конец файла следующие настройки:

```properties
# ===============================
# SPRING BOOT ACTUATOR НАСТРОЙКИ
# ===============================

# Включить endpoints
management.endpoints.web.exposure.include=health,metrics,prometheus,info
# Базовый путь для actuator endpoints
management.endpoints.web.base-path=/actuator
# Включить метрики HTTP запросов
management.metrics.web.server.request.autotime.enabled=true
# Включить метрики для всех endpoints
management.metrics.web.server.request.metric-name=http.server.requests
# Настройки для Prometheus
management.metrics.export.prometheus.enabled=true
```

---

### 2.2. Настроить Actuator для analytics-service

**Файл:** `analytics-service/src/main/resources/application.properties`

**Действие:** Добавить те же настройки в конец файла.

---

## ШАГ 3: Настройка Security для доступа к Actuator endpoints

### 3.1. Обновить SecurityConfig для main-app

**Файл:** `main-app/src/main/java/online/ityura/springdigitallibrary/security/SecurityConfig.java`

**Действие:** В методе `securityFilterChain` добавить разрешение для Actuator endpoints в секцию `.requestMatchers()`:

```java
.requestMatchers("/actuator/**").permitAll()
```

**Важно:** Разместить это правило перед более общими правилами авторизации.

**Пример расположения:**
```java
.requestMatchers("/api/v1/auth/**").permitAll()
.requestMatchers("/api/v1/health").permitAll()
.requestMatchers("/actuator/**").permitAll()  // <-- Добавить здесь
.requestMatchers("/api/v1/kuberinfo").permitAll()
```

---

### 3.2. Проверить SecurityConfig для analytics-service

**Действие:** Если в analytics-service есть SecurityConfig, добавить аналогичное правило. Если нет - Actuator endpoints будут доступны по умолчанию.

---

## ШАГ 4: Создание компонента для кастомных метрик ошибок

### 4.1. Создать класс MetricsService для main-app

**Файл:** `main-app/src/main/java/online/ityura/springdigitallibrary/metrics/MetricsService.java`

**Действие:** Создать новый класс:

```java
package online.ityura.springdigitallibrary.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
    
    private final MeterRegistry meterRegistry;
    private final Counter http4xxErrors;
    private final Counter http5xxErrors;
    private final Counter httpErrorsByStatus;
    
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Общие счетчики для 4xx и 5xx ошибок
        this.http4xxErrors = Counter.builder("http.errors.4xx.total")
                .description("Total number of 4xx HTTP errors")
                .register(meterRegistry);
        
        this.http5xxErrors = Counter.builder("http.errors.5xx.total")
                .description("Total number of 5xx HTTP errors")
                .register(meterRegistry);
        
        // Счетчик для всех ошибок по статусу
        this.httpErrorsByStatus = Counter.builder("http.errors.by.status")
                .description("HTTP errors by status code")
                .tag("status", "unknown")
                .register(meterRegistry);
    }
    
    /**
     * Увеличить счетчик ошибок по статусу кода
     */
    public void incrementErrorCounter(int statusCode, String path, String method, String errorType) {
        // Увеличить общий счетчик по статусу
        Counter.builder("http.errors.by.status")
                .tag("status", String.valueOf(statusCode))
                .tag("path", path != null ? path : "unknown")
                .tag("method", method != null ? method : "unknown")
                .tag("error_type", errorType != null ? errorType : "unknown")
                .description("HTTP errors by status code, path, method and error type")
                .register(meterRegistry)
                .increment();
        
        // Увеличить счетчики 4xx или 5xx
        if (statusCode >= 400 && statusCode < 500) {
            http4xxErrors.increment();
        } else if (statusCode >= 500) {
            http5xxErrors.increment();
        }
    }
    
    /**
     * Записать время выполнения запроса
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }
    
    /**
     * Остановить таймер и записать метрику
     */
    public void recordTimer(Timer.Sample sample, String path, String method, int statusCode) {
        sample.stop(Timer.builder("http.request.duration")
                .description("HTTP request duration")
                .tag("path", path != null ? path : "unknown")
                .tag("method", method != null ? method : "unknown")
                .tag("status", String.valueOf(statusCode))
                .register(meterRegistry));
    }
}
```

---

### 4.2. Интегрировать MetricsService в GlobalExceptionHandler

**Файл:** `main-app/src/main/java/online/ityura/springdigitallibrary/exception/GlobalExceptionHandler.java`

**Действие:** 
1. Добавить поле `MetricsService` через конструктор (dependency injection)
2. В каждом методе `@ExceptionHandler` после создания `ErrorResponse`, но перед возвратом `ResponseEntity`, вызвать:
   ```java
   metricsService.incrementErrorCounter(
       status.value(), 
       request.getRequestURI(), 
       request.getMethod(),
       errorCode
   );
   ```

**Пример для метода `handleResponseStatusException`:**
```java
@ExceptionHandler(ResponseStatusException.class)
public ResponseEntity<ErrorResponse> handleResponseStatusException(
        ResponseStatusException ex, 
        HttpServletRequest request) {
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
    String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
    String errorCode = determineErrorCode(status, message);
    
    ErrorResponse response = ErrorResponse.builder()
            .status(status.value())
            .error(errorCode)
            .message(message)
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .build();
    
    // Добавить метрику
    metricsService.incrementErrorCounter(
        status.value(), 
        request.getRequestURI(), 
        request.getMethod(),
        errorCode
    );
    
    return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response);
}
```

**Повторить для всех методов:** `handleRuntimeException`, `handleMethodArgumentNotValidException`, `handleHttpMessageNotReadableException`

---

### 4.3. Создать аналогичный MetricsService для analytics-service (опционально)

**Действие:** Если в analytics-service есть обработка ошибок, создать аналогичный сервис.

---

## ШАГ 5: Создание конфигурации Prometheus

### 5.1. Создать директорию для конфигурации Prometheus

**Действие:** Создать директорию `for_local_run_only/prometheus/`

---

### 5.2. Создать файл prometheus.yml

**Файл:** `for_local_run_only/prometheus/prometheus.yml`

**Действие:** Создать файл со следующим содержимым:

```yaml
global:
  scrape_interval: 15s  # Интервал сбора метрик
  evaluation_interval: 15s  # Интервал оценки правил алертов
  external_labels:
    cluster: 'local'
    environment: 'development'

# Конфигурация для правил алертов (опционально, для будущего использования)
rule_files:
  # - "alerting.rules.yml"

# Конфигурация для сбора метрик (scrape configs)
scrape_configs:
  # Prometheus сам себя мониторит
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
        labels:
          service: 'prometheus'

  # Backend сервис (main-app)
  - job_name: 'backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']  # Внутренний порт контейнера
        labels:
          service: 'backend'
          application: 'spring-digital-library'
    scrape_interval: 10s  # Можно настроить отдельно для каждого сервиса

  # Analytics Service
  - job_name: 'analytics-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['analytics-service:8090']  # Внутренний порт контейнера
        labels:
          service: 'analytics-service'
          application: 'analytics-service'
    scrape_interval: 10s
```

---

## ШАГ 6: Обновление docker-compose.yml

### 6.1. Добавить сервис Prometheus

**Файл:** `for_local_run_only/docker-compose.yml`

**Действие:** Добавить новый сервис `prometheus` после сервиса `kibana` (перед секцией `networks:`):

```yaml
  # Prometheus для сбора метрик
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      # Монтировать конфигурацию Prometheus
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      # Volume для хранения данных Prometheus (опционально для персистентности)
      - ${PROMETHEUS_DATA_PATH:-C:/spring-digital-bookstore/prometheus_data}:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=15d'  # Хранить метрики 15 дней
      - '--web.console.libraries=/usr/share/prometheus/console_libraries'
      - '--web.console.templates=/usr/share/prometheus/consoles'
    networks:
      - spring-library-network
    depends_on:
      backend:
        condition: service_started
      analytics-service:
        condition: service_started
    healthcheck:
      test: ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:9090/-/healthy || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 30s
    restart: unless-stopped
```

---

### 6.2. Добавить сервис Grafana

**Действие:** Добавить новый сервис `grafana` сразу после сервиса `prometheus`:

```yaml
  # Grafana для визуализации метрик
  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      # Настройки администратора Grafana
      GF_SECURITY_ADMIN_USER: ${GRAFANA_ADMIN_USER:-admin}
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_ADMIN_PASSWORD:-admin}
      # Отключить вход через Google/GitHub (для локальной разработки)
      GF_AUTH_ANONYMOUS_ENABLED: "false"
      # Настройки для автоматической настройки datasource
      GF_INSTALL_PLUGINS: ""
    volumes:
      # Volume для хранения данных Grafana (дашборды, настройки)
      - ${GRAFANA_DATA_PATH:-C:/spring-digital-bookstore/grafana_data}:/var/lib/grafana
      # Автоматическая настройка datasource (опционально)
      - ./grafana/provisioning/datasources:/etc/grafana/provisioning/datasources:ro
      # Автоматическая загрузка дашбордов (опционально)
      - ./grafana/provisioning/dashboards:/etc/grafana/provisioning/dashboards:ro
      - ./grafana/dashboards:/var/lib/grafana/dashboards:ro
    networks:
      - spring-library-network
    depends_on:
      prometheus:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:3000/api/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s
    restart: unless-stopped
```

---

## ШАГ 7: Создание автоматической настройки Grafana

### 7.1. Создать структуру директорий

**Действие:** Создать следующие директории:
- `for_local_run_only/grafana/provisioning/datasources/`
- `for_local_run_only/grafana/provisioning/dashboards/`
- `for_local_run_only/grafana/dashboards/`

---

### 7.2. Создать конфигурацию datasource для Prometheus

**Файл:** `for_local_run_only/grafana/provisioning/datasources/prometheus.yml`

**Действие:** Создать файл:

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
    jsonData:
      timeInterval: "15s"
      httpMethod: POST
```

---

### 7.3. Создать конфигурацию для автоматической загрузки дашбордов

**Файл:** `for_local_run_only/grafana/provisioning/dashboards/dashboards.yml`

**Действие:** Создать файл:

```yaml
apiVersion: 1

providers:
  - name: 'Default'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards
      foldersFromFilesStructure: true
```

---

## ШАГ 8: Создание базовых дашбордов Grafana (опционально, можно сделать позже)

### 8.1. Создать дашборд "Application Overview"

**Файл:** `for_local_run_only/grafana/dashboards/application-overview.json`

**Действие:** Создать JSON файл с дашбордом. Можно создать через UI Grafana и экспортировать, или использовать готовый шаблон.

**Основные панели для включения:**
- HTTP Request Rate
- HTTP Error Rate (4xx, 5xx)
- Response Time (p50, p95, p99)
- Active Requests
- Error Count by Status Code
- Top Error Endpoints

---

## ШАГ 9: Тестирование и проверка

### 9.1. Пересобрать Docker образы (если нужно)

**Действие:** Если образы собираются локально, выполнить:
```bash
# Для main-app
cd main-app
mvn clean package
docker build -t your-image-name .

# Для analytics-service
cd analytics-service
mvn clean package
docker build -t your-analytics-image-name .
```

---

### 9.2. Запустить docker-compose

**Действие:** В директории `for_local_run_only/` выполнить:
```bash
docker-compose up -d
```

---

### 9.3. Проверить доступность Actuator endpoints

**Действие:** Проверить в браузере или через curl:

1. **Backend Actuator Prometheus:**
   ```
   http://localhost:8088/actuator/prometheus
   ```
   Должны отображаться метрики в формате Prometheus.

2. **Analytics Service Actuator Prometheus:**
   ```
   http://localhost:8090/actuator/prometheus
   ```

3. **Backend Actuator Health:**
   ```
   http://localhost:8088/actuator/health
   ```

---

### 9.4. Проверить Prometheus

**Действие:** Открыть в браузере:
```
http://localhost:9090
```

**Проверки:**
1. Перейти в **Status → Targets** - должны быть видны все targets (backend, analytics-service, prometheus) со статусом UP
2. Перейти в **Graph** - попробовать выполнить запрос: `http_server_requests_seconds_count`
3. Проверить, что метрики собираются

---

### 9.5. Проверить Grafana

**Действие:** Открыть в браузере:
```
http://localhost:3000
```

**Логин:** admin / admin (или значения из переменных окружения)

**Проверки:**
1. Перейти в **Configuration → Data Sources**
2. Проверить, что Prometheus datasource добавлен и работает (кнопка "Save & Test")
3. Перейти в **Explore** - попробовать выполнить запрос: `http_server_requests_seconds_count`
4. Проверить, что дашборды загружены (если созданы)

---

### 9.6. Сгенерировать тестовые ошибки

**Действие:** Выполнить запросы, которые вызывают ошибки:
- 400: Невалидный запрос
- 404: Несуществующий endpoint
- 500: Внутренняя ошибка (если есть такой endpoint для тестирования)

**Проверка:** В Prometheus проверить метрики:
- `http_errors_4xx_total`
- `http_errors_5xx_total`
- `http_errors_by_status`

---

## ШАГ 10: Создание дашбордов в Grafana (ручная настройка)

### 10.1. Создать дашборд "Application Overview"

**Действие:** В Grafana UI:
1. Перейти в **Dashboards → New Dashboard**
2. Добавить панели:
   - **HTTP Request Rate:** `rate(http_server_requests_seconds_count[5m])`
   - **HTTP Error Rate (4xx):** `rate(http_errors_4xx_total[5m])`
   - **HTTP Error Rate (5xx):** `rate(http_errors_5xx_total[5m])`
   - **Response Time (p95):** `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))`
   - **Error Count by Status:** `sum by (status) (rate(http_errors_by_status[5m]))`

---

### 10.2. Создать дашборд "Error Analysis"

**Действие:** Создать новый дашборд с панелями:
- **Error Count by Status Code:** `sum by (status) (http_errors_by_status)`
- **Error Rate by Endpoint:** `sum by (path) (rate(http_errors_by_status[5m]))`
- **Error Rate by Error Type:** `sum by (error_type) (rate(http_errors_by_status[5m]))`
- **Error Timeline:** График `rate(http_errors_by_status[5m])` по времени

---

### 10.3. Экспортировать дашборды

**Действие:** После создания дашбордов:
1. Открыть дашборд
2. Нажать **Settings (шестеренка) → JSON Model**
3. Скопировать JSON
4. Сохранить в файл `for_local_run_only/grafana/dashboards/`

---

## ШАГ 11: Финальная проверка и документация

### 11.1. Проверить все компоненты

**Проверочный список:**
- [ ] Actuator endpoints доступны для обоих сервисов
- [ ] Prometheus собирает метрики от обоих сервисов
- [ ] Все targets в Prometheus имеют статус UP
- [ ] Grafana подключена к Prometheus
- [ ] Дашборды отображают данные
- [ ] Кастомные метрики ошибок работают
- [ ] При генерации ошибок метрики обновляются

---

### 11.2. Документировать доступные endpoints

**Документировать:**
- Prometheus UI: `http://localhost:9090`
- Grafana UI: `http://localhost:3000`
- Backend Actuator: `http://localhost:8088/actuator/prometheus`
- Analytics Actuator: `http://localhost:8090/actuator/prometheus`

---

## Дополнительные улучшения (опционально)

### A. Настройка алертов в Prometheus

**Файл:** `for_local_run_only/prometheus/alerting.rules.yml`

**Создать правила для:**
- Высокий error rate (> 5%)
- Высокий response time (p95 > 1s)
- Высокое использование памяти (> 80%)
- Много 5xx ошибок (> 10 в минуту)

---

### B. Настройка уведомлений в Grafana

**Действие:** Настроить каналы уведомлений (email, Slack и т.д.) для алертов.

---

### C. Добавление метрик для Kafka

**Действие:** Если доступны метрики Kafka, добавить панели для мониторинга Kafka в дашборды.

---

## Примечания

1. **Безопасность:** Для продакшена необходимо добавить аутентификацию для Actuator endpoints
2. **Персистентность данных:** Данные Prometheus и Grafana хранятся в volumes, указанных в docker-compose.yml
3. **Версионирование:** Рекомендуется зафиксировать версии образов Prometheus и Grafana для стабильности
4. **Производительность:** Настройки scrape_interval можно оптимизировать в зависимости от нагрузки

---

## Порядок выполнения шагов

1. ✅ ШАГ 1: Добавление зависимостей
2. ✅ ШАГ 2: Настройка Actuator
3. ✅ ШАГ 3: Настройка Security
4. ✅ ШАГ 4: Создание компонента метрик
5. ✅ ШАГ 5: Создание конфигурации Prometheus
6. ✅ ШАГ 6: Обновление docker-compose.yml
7. ✅ ШАГ 7: Создание автоматической настройки Grafana
8. ✅ ШАГ 8: Создание базовых дашбордов (опционально)
9. ✅ ШАГ 9: Тестирование и проверка
10. ✅ ШАГ 10: Создание дашбордов в Grafana
11. ✅ ШАГ 11: Финальная проверка

---

**Дата создания плана:** 2025-01-XX  
**Версия:** 1.0
