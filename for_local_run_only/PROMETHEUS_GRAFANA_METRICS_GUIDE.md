# Руководство по использованию метрик в Prometheus и Grafana

## Проверка доступности метрик

### 1. Проверка Actuator endpoints

Метрики доступны через следующие endpoints:

- **Backend (порт 8088):** http://localhost:8088/actuator/prometheus
- **Analytics Service (порт 8090):** http://localhost:8090/actuator/prometheus

### 2. Проверка Prometheus

- **Prometheus UI:** http://localhost:9090
- **Проверка targets:** http://localhost:9090/targets (все должны быть в статусе "UP")

### 3. Проверка Grafana

- **Grafana UI:** http://localhost:3000
- **Логин:** admin / admin
- **Datasource:** Prometheus должен быть автоматически настроен

---

## Примеры запросов в Prometheus

### Основные метрики HTTP запросов

#### 1. Общее количество HTTP запросов
```
http_server_requests_seconds_count
```

#### 2. Количество запросов по статусу
```
http_server_requests_seconds_count{status="200"}
http_server_requests_seconds_count{status="404"}
http_server_requests_seconds_count{status="500"}
```

#### 3. Rate запросов (запросов в секунду)
```
rate(http_server_requests_seconds_count[5m])
```

#### 4. Rate запросов по статусу
```
rate(http_server_requests_seconds_count{status="200"}[5m])
rate(http_server_requests_seconds_count{status=~"4.."}[5m])  # Все 4xx ошибки
rate(http_server_requests_seconds_count{status=~"5.."}[5m])  # Все 5xx ошибки
```

#### 5. Время ответа (p95, p99)
```
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))
```

### Метрики ошибок (кастомные)

#### 1. Общее количество 4xx ошибок
```
http_errors_4xx_total
```

#### 2. Общее количество 5xx ошибок
```
http_errors_5xx_total
```

#### 3. Rate ошибок
```
rate(http_errors_4xx_total[5m])
rate(http_errors_5xx_total[5m])
```

#### 4. Ошибки по статусу, пути и типу
```
http_errors_by_status_total
http_errors_by_status_total{status="404"}
http_errors_by_status_total{path="/api/v1/books/57/image"}
http_errors_by_status_total{error_type="BOOK_NOT_FOUND"}
```

#### 5. Rate ошибок по статусу
```
sum by (status) (rate(http_errors_by_status_total[5m]))
```

#### 6. Rate ошибок по endpoint
```
sum by (path) (rate(http_errors_by_status_total[5m]))
```

### Метрики по сервисам

#### 1. Фильтрация по сервису (backend)
```
http_server_requests_seconds_count{job="backend"}
```

#### 2. Фильтрация по сервису (analytics-service)
```
http_server_requests_seconds_count{job="analytics-service"}
```

### JVM метрики

#### 1. Использование памяти
```
jvm_memory_used_bytes{area="heap"}
jvm_memory_committed_bytes{area="heap"}
```

#### 2. Количество потоков
```
jvm_threads_live_threads
```

#### 3. CPU использование
```
process_cpu_usage
system_cpu_usage
```

---

## Примеры запросов в Grafana

### Создание панели в Grafana

1. Откройте Grafana: http://localhost:3000
2. Войдите (admin/admin)
3. Перейдите в **Explore** (иконка компаса слева)
4. Выберите datasource **Prometheus**
5. Введите запрос и нажмите **Run query**

### Полезные запросы для дашбордов

#### 1. HTTP Request Rate (запросов в секунду)
```
sum(rate(http_server_requests_seconds_count[5m]))
```

#### 2. HTTP Request Rate по статусу
```
sum(rate(http_server_requests_seconds_count{status="200"}[5m])) by (status)
sum(rate(http_server_requests_seconds_count{status=~"4.."}[5m])) by (status)
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (status)
```

#### 3. Error Rate (4xx и 5xx)
```
sum(rate(http_errors_4xx_total[5m]))
sum(rate(http_errors_5xx_total[5m]))
```

#### 4. Response Time (p95)
```
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))
```

#### 5. Ошибки по статусу
```
sum by (status) (rate(http_errors_by_status_total[5m]))
```

#### 6. Ошибки по endpoint
```
topk(10, sum by (path) (rate(http_errors_by_status_total[5m])))
```

#### 7. Активные запросы
```
sum(http_server_requests_active_seconds_count)
```

#### 8. Использование памяти JVM
```
sum(jvm_memory_used_bytes{area="heap"}) by (job)
```

---

## Создание дашборда в Grafana

### Шаг 1: Создать новый дашборд

1. Перейдите в **Dashboards** → **New Dashboard**
2. Нажмите **Add visualization**

### Шаг 2: Добавить панели

#### Панель 1: HTTP Request Rate
- **Query:** `sum(rate(http_server_requests_seconds_count[5m]))`
- **Visualization:** Time series
- **Title:** HTTP Request Rate

#### Панель 2: Error Rate
- **Query A:** `sum(rate(http_errors_4xx_total[5m]))` (Label: 4xx Errors)
- **Query B:** `sum(rate(http_errors_5xx_total[5m]))` (Label: 5xx Errors)
- **Visualization:** Time series
- **Title:** HTTP Error Rate

#### Панель 3: Response Time (p95)
- **Query:** `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))`
- **Visualization:** Time series
- **Title:** Response Time (p95)

#### Панель 4: Ошибки по статусу (таблица)
- **Query:** `sum by (status) (rate(http_errors_by_status_total[5m]))`
- **Visualization:** Table
- **Title:** Errors by Status Code

#### Панель 5: Топ ошибок по endpoint
- **Query:** `topk(10, sum by (path) (rate(http_errors_by_status_total[5m])))`
- **Visualization:** Bar chart
- **Title:** Top 10 Error Endpoints

#### Панель 6: JVM Memory Usage
- **Query:** `sum(jvm_memory_used_bytes{area="heap"}) by (job)`
- **Visualization:** Time series
- **Title:** JVM Heap Memory Usage

### Шаг 3: Сохранить дашборд

1. Нажмите **Save dashboard**
2. Введите название (например, "Application Metrics")
3. Нажмите **Save**

---

## Поиск метрик в Prometheus

### Через UI Prometheus

1. Откройте http://localhost:9090
2. Перейдите на вкладку **Graph**
3. В поле запроса начните вводить название метрики (например, `http_`)
4. Prometheus предложит автодополнение

### Список доступных метрик

Основные метрики, которые экспортируются:

- `http_server_requests_seconds_count` - количество HTTP запросов
- `http_server_requests_seconds_sum` - сумма времени выполнения запросов
- `http_server_requests_seconds_bucket` - гистограмма времени выполнения
- `http_errors_4xx_total` - общее количество 4xx ошибок
- `http_errors_5xx_total` - общее количество 5xx ошибок
- `http_errors_by_status_total` - ошибки по статусу, пути и типу
- `jvm_memory_used_bytes` - использование памяти JVM
- `jvm_threads_live_threads` - количество живых потоков
- `process_cpu_usage` - использование CPU процессом
- `hikaricp_connections_active` - активные соединения с БД
- `spring_kafka_listener_seconds_count` - количество обработанных Kafka сообщений

---

## Отладка проблем

### Проблема: Не вижу метрик в Prometheus

1. Проверьте статус targets: http://localhost:9090/targets
2. Если target в статусе DOWN:
   - Проверьте, что сервис запущен: `docker-compose ps`
   - Проверьте доступность actuator endpoint: http://localhost:8088/actuator/prometheus
   - Проверьте логи Prometheus: `docker-compose logs prometheus`

### Проблема: Не вижу метрик в Grafana

1. Проверьте datasource:
   - Перейдите в **Configuration** → **Data Sources**
   - Убедитесь, что Prometheus datasource существует
   - Нажмите **Save & Test** - должно быть "Data source is working"
2. Если datasource не работает:
   - Проверьте, что Prometheus доступен: http://localhost:9090
   - Проверьте URL datasource (должен быть `http://prometheus:9090`)

### Проблема: Метрики не обновляются

1. Проверьте scrape_interval в prometheus.yml (должен быть 10-15 секунд)
2. Убедитесь, что приложение генерирует трафик (делайте запросы к API)
3. Проверьте, что метрики экспортируются: http://localhost:8088/actuator/prometheus

---

## Полезные ссылки

- **Prometheus UI:** http://localhost:9090
- **Grafana UI:** http://localhost:3000
- **Backend Actuator:** http://localhost:8088/actuator/prometheus
- **Analytics Actuator:** http://localhost:8090/actuator/prometheus

---

## Быстрый старт

### Проверить, что метрики работают:

1. Откройте Prometheus: http://localhost:9090
2. В поле запроса введите: `http_server_requests_seconds_count`
3. Нажмите **Execute**
4. Должны появиться результаты

### Создать простой график в Grafana:

1. Откройте Grafana: http://localhost:3000
2. Перейдите в **Explore**
3. Выберите datasource **Prometheus**
4. Введите запрос: `sum(rate(http_server_requests_seconds_count[5m]))`
5. Нажмите **Run query**
6. Должен появиться график
