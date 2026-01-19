# Полный справочник метрик для мониторинга приложения

## Содержание

1. [HTTP Метрики](#http-метрики)
2. [Кастомные метрики ошибок](#кастомные-метрики-ошибок)
3. [JVM Метрики](#jvm-метрики)
4. [Метрики базы данных (HikariCP)](#метрики-базы-данных-hikaricp)
5. [Метрики Kafka](#метрики-kafka)
6. [Метрики Spring Data Repository](#метрики-spring-data-repository)
7. [Метрики Spring Security](#метрики-spring-security)
8. [Системные метрики](#системные-метрики)
9. [Метрики приложения](#метрики-приложения)
10. [Полезные PromQL запросы](#полезные-promql-запросы)

---

## HTTP Метрики

### Основные метрики HTTP запросов

#### `http_server_requests_seconds_count`
**Тип:** Counter  
**Описание:** Общее количество HTTP запросов  
**Теги:**
- `method` - HTTP метод (GET, POST, PUT, DELETE и т.д.)
- `status` - HTTP статус код (200, 404, 500 и т.д.)
- `uri` - URI запроса
- `outcome` - Результат запроса (SUCCESS, CLIENT_ERROR, SERVER_ERROR)
- `exception` - Тип исключения (если было)

**Примеры запросов:**
```promql
# Общее количество запросов
http_server_requests_seconds_count

# Количество запросов по статусу
http_server_requests_seconds_count{status="200"}
http_server_requests_seconds_count{status="404"}
http_server_requests_seconds_count{status="500"}

# Количество запросов по методу
http_server_requests_seconds_count{method="GET"}
http_server_requests_seconds_count{method="POST"}

# Количество запросов по endpoint
http_server_requests_seconds_count{uri="/api/v1/books"}
```

#### `http_server_requests_seconds_sum`
**Тип:** Summary  
**Описание:** Суммарное время выполнения всех HTTP запросов (в секундах)  
**Теги:** Те же, что и `http_server_requests_seconds_count`

**Примеры запросов:**
```promql
# Среднее время ответа
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# Среднее время ответа по статусу
rate(http_server_requests_seconds_sum{status="200"}[5m]) / rate(http_server_requests_seconds_count{status="200"}[5m])
```

#### `http_server_requests_seconds_bucket`
**Тип:** Histogram  
**Описание:** Гистограмма времени выполнения запросов  
**Теги:** Те же, что и `http_server_requests_seconds_count` + `le` (less or equal)

**Примеры запросов:**
```promql
# 95-й перцентиль времени ответа
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# 99-й перцентиль времени ответа
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))

# 50-й перцентиль (медиана)
histogram_quantile(0.50, rate(http_server_requests_seconds_bucket[5m]))

# Перцентили по endpoint
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{uri="/api/v1/books"}[5m]))
```

#### `http_server_requests_seconds_max`
**Тип:** Gauge  
**Описание:** Максимальное время выполнения запроса  
**Теги:** Те же, что и `http_server_requests_seconds_count`

#### `http_server_requests_active_seconds_count`
**Тип:** Summary  
**Описание:** Количество активных запросов в данный момент  
**Теги:** Те же, что и `http_server_requests_seconds_count`

**Примеры запросов:**
```promql
# Текущее количество активных запросов
sum(http_server_requests_active_seconds_count)
```

---

## Кастомные метрики ошибок

### `http_errors_4xx_total`
**Тип:** Counter  
**Описание:** Общее количество HTTP ошибок 4xx (клиентские ошибки)  
**Теги:** Нет

**Примеры запросов:**
```promql
# Общее количество 4xx ошибок
http_errors_4xx_total

# Rate 4xx ошибок (ошибок в секунду)
rate(http_errors_4xx_total[5m])

# Rate 4xx ошибок за последний час
rate(http_errors_4xx_total[1h])
```

### `http_errors_5xx_total`
**Тип:** Counter  
**Описание:** Общее количество HTTP ошибок 5xx (серверные ошибки)  
**Теги:** Нет

**Примеры запросов:**
```promql
# Общее количество 5xx ошибок
http_errors_5xx_total

# Rate 5xx ошибок (ошибок в секунду)
rate(http_errors_5xx_total[5m])

# Процент ошибок от общего количества запросов
rate(http_errors_5xx_total[5m]) / rate(http_server_requests_seconds_count[5m]) * 100
```

### `http_errors_by_status_total`
**Тип:** Counter  
**Описание:** Ошибки по статусу кода, пути, методу и типу ошибки  
**Теги:**
- `status` - HTTP статус код (400, 404, 500 и т.д.)
- `path` - Путь запроса
- `method` - HTTP метод
- `error_type` - Тип ошибки (BOOK_NOT_FOUND, INTERNAL_SERVER_ERROR и т.д.)

**Примеры запросов:**
```promql
# Ошибки по статусу
sum by (status) (http_errors_by_status_total)

# Rate ошибок по статусу
sum by (status) (rate(http_errors_by_status_total[5m]))

# Топ 10 endpoints с ошибками
topk(10, sum by (path) (rate(http_errors_by_status_total[5m])))

# Ошибки по типу
sum by (error_type) (rate(http_errors_by_status_total[5m]))

# Ошибки по методу
sum by (method) (rate(http_errors_by_status_total[5m]))
```

### `http_request_duration_seconds`
**Тип:** Timer (если используется кастомный Timer)  
**Описание:** Время выполнения HTTP запросов (кастомная метрика)  
**Теги:**
- `path` - Путь запроса
- `method` - HTTP метод
- `status` - HTTP статус код

---

## JVM Метрики

### Метрики памяти

#### `jvm_memory_used_bytes`
**Тип:** Gauge  
**Описание:** Используемая память JVM  
**Теги:**
- `area` - Область памяти (heap, nonheap)
- `id` - ID области памяти (G1 Eden Space, G1 Old Gen, Metaspace и т.д.)

**Примеры запросов:**
```promql
# Использование heap памяти
sum(jvm_memory_used_bytes{area="heap"}) by (job)

# Использование nonheap памяти
sum(jvm_memory_used_bytes{area="nonheap"}) by (job)

# Использование памяти по областям
jvm_memory_used_bytes{area="heap", id="G1 Old Gen"}
jvm_memory_used_bytes{area="heap", id="G1 Eden Space"}
```

#### `jvm_memory_committed_bytes`
**Тип:** Gauge  
**Описание:** Выделенная (committed) память JVM  
**Теги:** Те же, что и `jvm_memory_used_bytes`

**Примеры запросов:**
```promql
# Выделенная heap память
sum(jvm_memory_committed_bytes{area="heap"}) by (job)
```

#### `jvm_memory_max_bytes`
**Тип:** Gauge  
**Описание:** Максимальная доступная память JVM  
**Теги:** Те же, что и `jvm_memory_used_bytes`

**Примеры запросов:**
```promql
# Процент использования heap памяти
sum(jvm_memory_used_bytes{area="heap"}) by (job) / sum(jvm_memory_max_bytes{area="heap"}) by (job) * 100
```

### Метрики сборщика мусора (GC)

#### `jvm_gc_pause_seconds`
**Тип:** Summary  
**Описание:** Время паузы сборщика мусора  
**Теги:**
- `action` - Действие GC (end of minor GC, end of major GC)
- `cause` - Причина GC (G1 Evacuation Pause, Metadata GC Threshold)
- `gc` - Тип GC (G1 Young Generation, G1 Old Generation)

**Примеры запросов:**
```promql
# Количество GC пауз
sum(rate(jvm_gc_pause_seconds_count[5m])) by (job)

# Среднее время GC паузы
sum(rate(jvm_gc_pause_seconds_sum[5m])) by (job) / sum(rate(jvm_gc_pause_seconds_count[5m])) by (job)

# Максимальное время GC паузы
jvm_gc_pause_seconds_max
```

#### `jvm_gc_memory_allocated_bytes_total`
**Тип:** Counter  
**Описание:** Общее количество выделенной памяти  
**Теги:** Нет

#### `jvm_gc_memory_promoted_bytes_total`
**Тип:** Counter  
**Описание:** Количество памяти, перешедшей из young в old generation  
**Теги:** Нет

#### `jvm_gc_overhead`
**Тип:** Gauge  
**Описание:** Процент времени CPU, потраченного на GC (0-1)  
**Теги:** Нет

**Примеры запросов:**
```promql
# Процент времени на GC
jvm_gc_overhead * 100
```

### Метрики потоков

#### `jvm_threads_live_threads`
**Тип:** Gauge  
**Описание:** Количество живых потоков  
**Теги:** Нет

**Примеры запросов:**
```promql
# Количество потоков
jvm_threads_live_threads

# Количество потоков по сервисам
jvm_threads_live_threads{job="backend"}
jvm_threads_live_threads{job="analytics-service"}
```

#### `jvm_threads_daemon_threads`
**Тип:** Gauge  
**Описание:** Количество daemon потоков  
**Теги:** Нет

#### `jvm_threads_peak_threads`
**Тип:** Gauge  
**Описание:** Пиковое количество потоков с момента запуска  
**Теги:** Нет

#### `jvm_threads_states_threads`
**Тип:** Gauge  
**Описание:** Количество потоков по состояниям  
**Теги:**
- `state` - Состояние потока (runnable, blocked, waiting, timed-waiting)

**Примеры запросов:**
```promql
# Потоки по состояниям
jvm_threads_states_threads{state="runnable"}
jvm_threads_states_threads{state="blocked"}
jvm_threads_states_threads{state="waiting"}
```

### Метрики классов

#### `jvm_classes_loaded_classes`
**Тип:** Gauge  
**Описание:** Количество загруженных классов  
**Теги:** Нет

#### `jvm_classes_loaded_count_classes_total`
**Тип:** Counter  
**Описание:** Общее количество загруженных классов с момента запуска  
**Теги:** Нет

#### `jvm_classes_unloaded_classes_total`
**Тип:** Counter  
**Описание:** Общее количество выгруженных классов  
**Теги:** Нет

### Метрики компиляции

#### `jvm_compilation_time_ms_total`
**Тип:** Counter  
**Описание:** Общее время компиляции JIT  
**Теги:**
- `compiler` - Компилятор (HotSpot 64-Bit Tiered Compilers)

---

## Метрики базы данных (HikariCP)

### Метрики соединений

#### `hikaricp_connections`
**Тип:** Gauge  
**Описание:** Общее количество соединений в пуле  
**Теги:**
- `pool` - Имя пула (обычно HikariPool-1)

**Примеры запросов:**
```promql
# Общее количество соединений
hikaricp_connections

# Активные соединения
hikaricp_connections_active

# Простаивающие соединения
hikaricp_connections_idle
```

#### `hikaricp_connections_active`
**Тип:** Gauge  
**Описание:** Количество активных соединений  
**Теги:** `pool`

#### `hikaricp_connections_idle`
**Тип:** Gauge  
**Описание:** Количество простаивающих соединений  
**Теги:** `pool`

#### `hikaricp_connections_max`
**Тип:** Gauge  
**Описание:** Максимальное количество соединений в пуле  
**Теги:** `pool`

#### `hikaricp_connections_min`
**Тип:** Gauge  
**Описание:** Минимальное количество соединений в пуле  
**Теги:** `pool`

#### `hikaricp_connections_pending`
**Тип:** Gauge  
**Описание:** Количество потоков, ожидающих соединение  
**Теги:** `pool`

**Примеры запросов:**
```promql
# Процент использования пула соединений
hikaricp_connections_active / hikaricp_connections_max * 100

# Ожидающие соединения (проблема!)
hikaricp_connections_pending > 0
```

### Метрики времени

#### `hikaricp_connections_acquire_seconds`
**Тип:** Summary  
**Описание:** Время получения соединения из пула  
**Теги:** `pool`

**Примеры запросов:**
```promql
# Среднее время получения соединения
rate(hikaricp_connections_acquire_seconds_sum[5m]) / rate(hikaricp_connections_acquire_seconds_count[5m])

# 95-й перцентиль времени получения соединения
histogram_quantile(0.95, rate(hikaricp_connections_acquire_seconds_bucket[5m]))
```

#### `hikaricp_connections_usage_seconds`
**Тип:** Summary  
**Описание:** Время использования соединения  
**Теги:** `pool`

#### `hikaricp_connections_creation_seconds`
**Тип:** Summary  
**Описание:** Время создания нового соединения  
**Теги:** `pool`

### Метрики ошибок

#### `hikaricp_connections_timeout_total`
**Тип:** Counter  
**Описание:** Количество таймаутов при получении соединения  
**Теги:** `pool`

**Примеры запросов:**
```promql
# Rate таймаутов соединений
rate(hikaricp_connections_timeout_total[5m])
```

### JDBC метрики

#### `jdbc_connections_active`
**Тип:** Gauge  
**Описание:** Количество активных JDBC соединений  
**Теги:**
- `name` - Имя data source

#### `jdbc_connections_idle`
**Тип:** Gauge  
**Описание:** Количество простаивающих JDBC соединений  
**Теги:** `name`

#### `jdbc_connections_max`
**Тип:** Gauge  
**Описание:** Максимальное количество JDBC соединений  
**Теги:** `name`

---

## Метрики Kafka

### Метрики KafkaTemplate (Producer)

#### `spring_kafka_template_seconds`
**Тип:** Summary  
**Описание:** Время выполнения операций KafkaTemplate  
**Теги:**
- `name` - Имя KafkaTemplate (обычно kafkaTemplate)
- `result` - Результат (success, failure)
- `exception` - Тип исключения (если было)

**Примеры запросов:**
```promql
# Количество отправленных сообщений
sum(rate(spring_kafka_template_seconds_count{result="success"}[5m])) by (job)

# Количество ошибок отправки
sum(rate(spring_kafka_template_seconds_count{result="failure"}[5m])) by (job)

# Среднее время отправки сообщения
sum(rate(spring_kafka_template_seconds_sum[5m])) by (job) / sum(rate(spring_kafka_template_seconds_count[5m])) by (job)
```

### Метрики KafkaListener (Consumer)

#### `spring_kafka_listener_seconds`
**Тип:** Summary  
**Описание:** Время обработки сообщений KafkaListener  
**Теги:**
- `name` - Имя listener контейнера
- `result` - Результат (success, failure)
- `exception` - Тип исключения (если было)

**Примеры запросов:**
```promql
# Количество обработанных сообщений
sum(rate(spring_kafka_listener_seconds_count{result="success"}[5m])) by (job, name)

# Количество ошибок обработки
sum(rate(spring_kafka_listener_seconds_count{result="failure"}[5m])) by (job, name)

# Среднее время обработки сообщения
sum(rate(spring_kafka_listener_seconds_sum[5m])) by (job) / sum(rate(spring_kafka_listener_seconds_count[5m])) by (job)

# 95-й перцентиль времени обработки
histogram_quantile(0.95, rate(spring_kafka_listener_seconds_bucket[5m]))
```

---

## Метрики Spring Data Repository

#### `spring_data_repository_invocations_seconds`
**Тип:** Summary  
**Описание:** Время выполнения операций репозитория  
**Теги:**
- `repository` - Имя репозитория (BookRepository, UserRepository и т.д.)
- `method` - Имя метода (findAll, findById, save и т.д.)
- `state` - Состояние (SUCCESS, FAILURE)
- `exception` - Тип исключения (если было)

**Примеры запросов:**
```promql
# Количество вызовов репозиториев
sum(rate(spring_data_repository_invocations_seconds_count[5m])) by (repository, method)

# Среднее время выполнения запросов к репозиторию
sum(rate(spring_data_repository_invocations_seconds_sum[5m])) by (repository) / sum(rate(spring_data_repository_invocations_seconds_count[5m])) by (repository)

# Топ 10 самых медленных методов репозиториев
topk(10, sum(rate(spring_data_repository_invocations_seconds_sum[5m])) by (repository, method))
```

---

## Метрики Spring Security

#### `spring_security_authorizations_seconds`
**Тип:** Summary  
**Описание:** Время выполнения авторизации  
**Теги:**
- `spring_security_authentication_type` - Тип аутентификации
- `spring_security_authorization_decision` - Решение авторизации (true, false)
- `spring_security_object` - Объект авторизации (request, method)
- `error` - Тип ошибки (если было)

**Примеры запросов:**
```promql
# Количество успешных авторизаций
sum(rate(spring_security_authorizations_seconds_count{spring_security_authorization_decision="true"}[5m]))

# Количество отказов в авторизации
sum(rate(spring_security_authorizations_seconds_count{spring_security_authorization_decision="false"}[5m]))

# Rate отказов в авторизации
rate(spring_security_authorizations_seconds_count{error="AccessDeniedException"}[5m])
```

#### `spring_security_http_secured_requests_seconds`
**Тип:** Summary  
**Описание:** Время обработки защищенных HTTP запросов  
**Теги:**
- `error` - Тип ошибки (если было)

---

## Системные метрики

### Метрики процесса

#### `process_cpu_usage`
**Тип:** Gauge  
**Описание:** Использование CPU процессом (0-1)  
**Теги:** Нет

**Примеры запросов:**
```promql
# Использование CPU в процентах
process_cpu_usage * 100

# Использование CPU по сервисам
process_cpu_usage{job="backend"}
process_cpu_usage{job="analytics-service"}
```

#### `process_cpu_time_ns_total`
**Тип:** Counter  
**Описание:** Общее время CPU, использованное процессом (в наносекундах)  
**Теги:** Нет

#### `process_uptime_seconds`
**Тип:** Gauge  
**Описание:** Время работы процесса (uptime)  
**Теги:** Нет

**Примеры запросов:**
```promql
# Время работы приложения
process_uptime_seconds / 3600  # в часах
```

#### `process_start_time_seconds`
**Тип:** Gauge  
**Описание:** Время запуска процесса (Unix timestamp)  
**Теги:** Нет

#### `process_files_open_files`
**Тип:** Gauge  
**Описание:** Количество открытых файлов  
**Теги:** Нет

#### `process_files_max_files`
**Тип:** Gauge  
**Описание:** Максимальное количество открытых файлов  
**Теги:** Нет

### Метрики системы

#### `system_cpu_usage`
**Тип:** Gauge  
**Описание:** Использование CPU системой (0-1)  
**Теги:** Нет

#### `system_cpu_count`
**Тип:** Gauge  
**Описание:** Количество CPU ядер  
**Теги:** Нет

#### `system_load_average_1m`
**Тип:** Gauge  
**Описание:** Средняя загрузка системы за 1 минуту  
**Теги:** Нет

**Примеры запросов:**
```promql
# Загрузка системы
system_load_average_1m

# Загрузка системы относительно количества CPU
system_load_average_1m / system_cpu_count
```

### Метрики диска

#### `disk_free_bytes`
**Тип:** Gauge  
**Описание:** Свободное место на диске  
**Теги:**
- `path` - Путь к диску

#### `disk_total_bytes`
**Тип:** Gauge  
**Описание:** Общий размер диска  
**Теги:** `path`

**Примеры запросов:**
```promql
# Процент свободного места
disk_free_bytes / disk_total_bytes * 100

# Процент использованного места
(1 - disk_free_bytes / disk_total_bytes) * 100
```

---

## Метрики приложения

#### `application_ready_time_seconds`
**Тип:** Gauge  
**Описание:** Время, необходимое для готовности приложения  
**Теги:**
- `main_application_class` - Главный класс приложения

#### `application_started_time_seconds`
**Тип:** Gauge  
**Описание:** Время запуска приложения  
**Теги:** `main_application_class`

### Метрики логирования

#### `logback_events_total`
**Тип:** Counter  
**Описание:** Количество лог-событий  
**Теги:**
- `level` - Уровень логирования (trace, debug, info, warn, error)

**Примеры запросов:**
```promql
# Rate ошибок в логах
rate(logback_events_total{level="error"}[5m])

# Rate предупреждений
rate(logback_events_total{level="warn"}[5m])

# Распределение по уровням
sum by (level) (rate(logback_events_total[5m]))
```

### Метрики Executor

#### `executor_active_threads`
**Тип:** Gauge  
**Описание:** Количество активных потоков executor  
**Теги:**
- `name` - Имя executor

#### `executor_pool_size_threads`
**Тип:** Gauge  
**Описание:** Текущий размер пула потоков  
**Теги:** `name`

#### `executor_pool_max_threads`
**Тип:** Gauge  
**Описание:** Максимальный размер пула потоков  
**Теги:** `name`

#### `executor_queued_tasks`
**Тип:** Gauge  
**Описание:** Количество задач в очереди  
**Теги:** `name`

**Примеры запросов:**
```promql
# Процент использования пула потоков
executor_pool_size_threads / executor_pool_max_threads * 100

# Задачи в очереди (проблема!)
executor_queued_tasks > 0
```

### Метрики планировщика задач

#### `tasks_scheduled_execution_seconds`
**Тип:** Summary  
**Описание:** Время выполнения запланированных задач  
**Теги:**
- `code_namespace` - Пространство имен класса
- `code_function` - Имя функции
- `outcome` - Результат (SUCCESS, FAILURE)
- `exception` - Тип исключения (если было)

**Примеры запросов:**
```promql
# Количество выполненных задач
sum(rate(tasks_scheduled_execution_seconds_count[5m])) by (code_function)

# Среднее время выполнения задачи
sum(rate(tasks_scheduled_execution_seconds_sum[5m])) by (code_function) / sum(rate(tasks_scheduled_execution_seconds_count[5m])) by (code_function)
```

---

## Полезные PromQL запросы

### Общая производительность

```promql
# HTTP Request Rate (запросов в секунду)
sum(rate(http_server_requests_seconds_count[5m])) by (job)

# HTTP Request Rate по статусу
sum(rate(http_server_requests_seconds_count{status="200"}[5m])) by (job)
sum(rate(http_server_requests_seconds_count{status=~"4.."}[5m])) by (job)
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (job)

# Response Time (p95)
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, job))

# Response Time (p99)
histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, job))

# Error Rate
sum(rate(http_errors_4xx_total[5m])) by (job)
sum(rate(http_errors_5xx_total[5m])) by (job)

# Error Percentage
sum(rate(http_errors_5xx_total[5m])) by (job) / sum(rate(http_server_requests_seconds_count[5m])) by (job) * 100
```

### Мониторинг ресурсов

```promql
# Использование памяти (heap)
sum(jvm_memory_used_bytes{area="heap"}) by (job) / sum(jvm_memory_max_bytes{area="heap"}) by (job) * 100

# Использование CPU
process_cpu_usage * 100

# Количество потоков
jvm_threads_live_threads

# Использование пула соединений БД
hikaricp_connections_active / hikaricp_connections_max * 100
```

### Топ запросы и ошибки

```promql
# Топ 10 самых медленных endpoints
topk(10, sum(rate(http_server_requests_seconds_sum[5m])) by (uri, job))

# Топ 10 endpoints с ошибками
topk(10, sum(rate(http_errors_by_status_total[5m])) by (path, job))

# Топ 10 endpoints по количеству запросов
topk(10, sum(rate(http_server_requests_seconds_count[5m])) by (uri, job))
```

### Алерты (примеры)

```promql
# Высокий error rate (> 1% ошибок)
sum(rate(http_errors_5xx_total[5m])) / sum(rate(http_server_requests_seconds_count[5m])) > 0.01

# Высокий response time (p95 > 1 секунда)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1

# Высокое использование памяти (> 80%)
sum(jvm_memory_used_bytes{area="heap"}) / sum(jvm_memory_max_bytes{area="heap"}) > 0.8

# Много ожидающих соединений БД
hikaricp_connections_pending > 5

# Много ошибок в логах (> 10 в минуту)
rate(logback_events_total{level="error"}[5m]) > 0.17
```

---

## Фильтрация по сервисам

Все метрики можно фильтровать по сервисам используя тег `job`:

```promql
# Метрики только для backend
http_server_requests_seconds_count{job="backend"}

# Метрики только для analytics-service
http_server_requests_seconds_count{job="analytics-service"}

# Метрики для всех сервисов
http_server_requests_seconds_count
```

---

## Полезные ссылки

- **Prometheus UI:** http://localhost:9090
- **Grafana UI:** http://localhost:3000
- **Backend Actuator:** http://localhost:8088/actuator/prometheus
- **Analytics Actuator:** http://localhost:8090/actuator/prometheus

---

## Примечания

1. Все метрики с суффиксом `_total` являются счетчиками (Counter) и должны использоваться с функцией `rate()` для получения скорости изменения
2. Метрики типа Summary автоматически создают `_count`, `_sum` и `_max`
3. Метрики типа Histogram автоматически создают `_bucket` для расчета перцентилей
4. Интервал `[5m]` означает последние 5 минут, можно использовать `[1m]`, `[15m]`, `[1h]` и т.д.
5. Функция `rate()` вычисляет скорость изменения метрики за указанный интервал
6. Функция `histogram_quantile()` вычисляет перцентили из гистограмм
