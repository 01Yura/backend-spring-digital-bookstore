# Настройка дашбордов в Grafana

## Автоматическая загрузка дашбордов

Дашборды автоматически загружаются из директории `grafana/dashboards/` при запуске Grafana через docker-compose.

### Структура файлов

```
for_local_run_only/
└── grafana/
    ├── dashboards/
    │   └── application-metrics.json  # Дашборд с метриками приложения
    └── provisioning/
        ├── dashboards/
        │   └── dashboards.yml       # Конфигурация автоматической загрузки
        └── datasources/
            └── prometheus.yml       # Конфигурация Prometheus datasource
```

### Как это работает

1. **Provisioning конфигурация** (`grafana/provisioning/dashboards/dashboards.yml`):
   - Указывает Grafana автоматически загружать дашборды из `/var/lib/grafana/dashboards`
   - Обновляет дашборды каждые 10 секунд
   - Позволяет редактировать дашборды через UI

2. **Docker volume** (`docker-compose.yml`):
   - Монтирует локальную директорию `./grafana/dashboards` в контейнер как `/var/lib/grafana/dashboards`
   - Любые изменения в JSON файлах автоматически подхватываются Grafana

3. **JSON дашборды**:
   - Файлы в формате JSON с определением панелей, запросов и настроек
   - Grafana автоматически импортирует их при запуске

---

## Использование

### Просмотр дашборда

1. Откройте Grafana: http://localhost:3000
2. Войдите (admin/admin)
3. Перейдите в **Dashboards** (иконка папки слева)
4. Найдите дашборд **"Application Metrics Dashboard"**
5. Откройте его

### Добавление нового дашборда

1. Создайте JSON файл в директории `for_local_run_only/grafana/dashboards/`
2. Используйте формат, как в `application-metrics.json`
3. Перезапустите Grafana или подождите 10 секунд (автообновление)

### Редактирование существующего дашборда

#### Через UI Grafana:
1. Откройте дашборд
2. Нажмите на иконку шестеренки (Settings) в правом верхнем углу
3. Выберите **JSON Model**
4. Отредактируйте JSON
5. Нажмите **Save**

#### Через файл:
1. Отредактируйте JSON файл в `for_local_run_only/grafana/dashboards/`
2. Сохраните файл
3. Grafana автоматически обновит дашборд через 10 секунд

---

## Экспорт дашборда из Grafana

Если вы создали дашборд через UI и хотите сохранить его как JSON файл:

1. Откройте дашборд в Grafana
2. Нажмите на иконку шестеренки (Settings)
3. Выберите **JSON Model**
4. Скопируйте весь JSON
5. Сохраните в файл `for_local_run_only/grafana/dashboards/your-dashboard.json`

**Важно:** Убедитесь, что JSON валидный. Можно использовать онлайн валидатор JSON.

---

## Структура JSON дашборда

Основные поля:

```json
{
  "dashboard": {
    "title": "Название дашборда",
    "tags": ["tag1", "tag2"],
    "refresh": "10s",  // Автообновление
    "panels": [
      {
        "id": 1,
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0},  // Позиция и размер
        "type": "timeseries",  // Тип панели
        "title": "Название панели",
        "targets": [
          {
            "expr": "promql_query_here",
            "refId": "A",
            "legendFormat": "{{job}}"
          }
        ]
      }
    ]
  }
}
```

### Типы панелей

- `timeseries` - График временных рядов
- `stat` - Статистика (одно значение)
- `table` - Таблица
- `gauge` - Индикатор
- `bar` - Столбчатая диаграмма
- `piechart` - Круговая диаграмма

### Grid позиционирование

- `h` - Высота (в единицах сетки, обычно 1 единица = 30px)
- `w` - Ширина (в единицах сетки, обычно 1 единица = 30px)
- `x` - Позиция по X (горизонтально)
- `y` - Позиция по Y (вертикально)

Сетка Grafana обычно имеет ширину 24 единицы.

---

## Панели в Application Metrics Dashboard

Дашборд включает следующие панели:

1. **HTTP Request Rate** - Скорость HTTP запросов (req/sec)
2. **HTTP Request Rate by Status** - Запросы по статусам (200, 4xx, 5xx)
3. **HTTP Error Rate** - Скорость ошибок (4xx и 5xx)
4. **Response Time** - Время ответа (p95, p99)
5. **JVM Heap Memory Usage** - Использование heap памяти
6. **JVM Threads** - Количество потоков JVM
7. **Database Connection Pool** - Использование пула соединений БД
8. **Kafka Messages** - Отправленные и полученные сообщения Kafka
9. **Top 10 Error Endpoints** - Топ 10 endpoints с ошибками
10. **Active Requests** - Количество активных запросов
11. **CPU Usage** - Использование CPU
12. **GC Pause Time** - Время пауз сборщика мусора

---

## Добавление новых панелей

### Пример: Добавление панели для метрики

```json
{
  "id": 13,
  "gridPos": {"h": 8, "w": 12, "x": 0, "y": 48},
  "type": "timeseries",
  "title": "Название панели",
  "targets": [
    {
      "expr": "your_promql_query_here",
      "refId": "A",
      "legendFormat": "{{job}}"
    }
  ],
  "fieldConfig": {
    "defaults": {
      "color": {"mode": "palette-classic"},
      "custom": {
        "drawStyle": "line",
        "lineInterpolation": "linear",
        "lineWidth": 1,
        "fillOpacity": 10,
        "gradientMode": "none",
        "spanNulls": false,
        "stacking": {"group": "A", "mode": "none"},
        "thresholdsStyle": {"mode": "off"}
      },
      "mappings": [],
      "thresholds": {
        "mode": "absolute",
        "steps": [
          {"color": "green", "value": null}
        ]
      },
      "unit": "short"
    }
  },
  "options": {
    "tooltip": {"mode": "multi"},
    "legend": {
      "calcs": ["lastNotNull", "max"],
      "displayMode": "table",
      "placement": "bottom"
    }
  }
}
```

Добавьте этот объект в массив `panels` в JSON файле дашборда.

---

## Перезапуск Grafana для применения изменений

Если изменения не применяются автоматически:

```bash
# Перезапустить только Grafana
docker-compose restart grafana

# Или перезапустить все сервисы
docker-compose restart
```

---

## Проверка загрузки дашбордов

1. Откройте Grafana: http://localhost:3000
2. Перейдите в **Dashboards**
3. Должен быть виден дашборд **"Application Metrics Dashboard"**

Если дашборд не появился:

1. Проверьте логи Grafana:
   ```bash
   docker-compose logs grafana | grep -i dashboard
   ```

2. Проверьте, что файл существует:
   ```bash
   ls -la for_local_run_only/grafana/dashboards/
   ```

3. Проверьте JSON на валидность (используйте онлайн валидатор)

4. Убедитесь, что volume правильно смонтирован в docker-compose.yml

---

## Полезные ссылки

- **Grafana UI:** http://localhost:3000
- **Prometheus UI:** http://localhost:9090
- **Документация Grafana:** https://grafana.com/docs/grafana/latest/dashboards/
- **JSON Model Reference:** https://grafana.com/docs/grafana/latest/dashboards/json-model/
