#!/bin/sh
# Скрипт для автоматического создания Data Views в Kibana
# Запускается внутри контейнера Kibana

KIBANA_URL="http://localhost:5601"
MAX_RETRIES=60
RETRY_DELAY=5

echo "Ожидание готовности Kibana..."

# Ждем, пока Kibana станет доступна
for i in $(seq 1 $MAX_RETRIES); do
    if wget -q --spider "$KIBANA_URL/api/status" 2>/dev/null || curl -f -s "$KIBANA_URL/api/status" > /dev/null 2>&1; then
        echo "Kibana готова!"
        break
    fi
    echo "Попытка $i/$MAX_RETRIES: Kibana еще не готова, ждем $RETRY_DELAY секунд..."
    sleep $RETRY_DELAY
done

# Ждем еще немного для полной инициализации
sleep 15

echo ""
echo "Создание Data Views (Index Patterns)..."

# Функция для создания Data View
create_data_view() {
    local title=$1
    local pattern=$2
    local time_field=${3:-"@timestamp"}
    
    # Используем wget или curl (в зависимости от того, что доступно)
    if command -v wget > /dev/null 2>&1; then
        response=$(wget -qO- --header="Content-Type: application/json" \
            --header="kbn-xsrf: true" \
            --post-data="{\"data_view\":{\"title\":\"$pattern\",\"name\":\"$title\",\"timeFieldName\":\"$time_field\"}}" \
            "$KIBANA_URL/api/data_views/data_view" 2>&1)
        http_code=$?
    else
        response=$(curl -s -w "\n%{http_code}" -X POST "$KIBANA_URL/api/data_views/data_view" \
            -H "Content-Type: application/json" \
            -H "kbn-xsrf: true" \
            -d "{\"data_view\":{\"title\":\"$pattern\",\"name\":\"$title\",\"timeFieldName\":\"$time_field\"}}" 2>&1)
        http_code=$(echo "$response" | tail -n1)
    fi
    
    if [ "$http_code" -eq 0 ] || [ "$http_code" -eq 200 ] || [ "$http_code" -eq 201 ]; then
        echo "✓ Создан Data View: $title ($pattern)"
        return 0
    elif echo "$response" | grep -q "409\|already exists"; then
        echo "⚠ Data View уже существует: $title ($pattern)"
        return 0
    else
        echo "✗ Ошибка при создании Data View $title (код: $http_code)"
        return 1
    fi
}

# Создаем Data Views
create_data_view "Main App Logs" "main-app-logs-*" "@timestamp"
create_data_view "Analytics Service Logs" "analytics-service-logs-*" "@timestamp"
create_data_view "All Logs" "*-logs-*" "@timestamp"

echo ""
echo "Готово!"
