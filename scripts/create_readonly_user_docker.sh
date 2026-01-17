#!/bin/bash

# Скрипт для создания пользователя PostgreSQL с правами read-only
# Работает как при запуске с хоста, так и внутри Docker контейнера
# 
# Использование с хоста (рекомендуется, если порт 5432 проброшен):
#   bash scripts/create_readonly_user_docker.sh [username] [password]
#   или с переменными окружения:
#   DB_ADMIN_USER=user DB_ADMIN_PASSWORD=pass bash scripts/create_readonly_user_docker.sh
#
# Использование внутри контейнера (способ 1 - копирование):
#   docker cp create_readonly_user_docker.sh spring-digital-library-db:/tmp/create_readonly_user.sh
#   docker exec -it spring-digital-library-db bash /tmp/create_readonly_user.sh [username] [password]
#
# Использование внутри контейнера (способ 2 - через stdin):
#   docker exec -i spring-digital-library-db bash -s -- [username] [password] < create_readonly_user_docker.sh

set -e  # Остановка при ошибке

# Параметры подключения к БД
# По умолчанию настроено для запуска с хоста (подключение к контейнеру через localhost:5432)
# Можно переопределить через переменные окружения:
#   DB_ADMIN_USER, DB_ADMIN_PASSWORD, DB_HOST, DB_PORT, DB_NAME
DB_NAME="${DB_NAME}"
DB_ADMIN_USER="${DB_ADMIN_USER}"
DB_ADMIN_PASSWORD="${DB_ADMIN_PASSWORD}"
DB_HOST="${DB_HOST:-localhost}"  # localhost для подключения к контейнеру с хоста
DB_PORT="${DB_PORT:-5432}"       # порт должен совпадать с проброшенным портом контейнера


# Параметры нового пользователя (приоритет: переменные окружения > аргументы командной строки > значения по умолчанию)
READONLY_USER="${READONLY_USER:-${1:-readonly_user}}"
if [ -z "$READONLY_PASSWORD" ] && [ -n "$2" ]; then
    READONLY_PASSWORD="$2"
elif [ -z "$READONLY_PASSWORD" ]; then
    READONLY_PASSWORD="pass_123_XYZ!"
fi

# Функция для экранирования пароля в SQL
# Экранирует одинарные кавычки (удваивает их) и обратные слэши
escape_sql_string() {
    local str="$1"
    # Экранируем обратные слэши (должно быть первым)
    str="${str//\\/\\\\}"
    # Экранируем одинарные кавычки (удваиваем их)
    str="${str//\'/\'\'}"
    echo "$str"
}

# Цвета для вывода
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Создание пользователя PostgreSQL с правами read-only${NC}"
echo "=========================================="
echo "База данных: $DB_NAME"
echo "Новый пользователь: $READONLY_USER"
echo "=========================================="
echo ""

# Проверка наличия psql
if ! command -v psql &> /dev/null; then
    echo -e "${YELLOW}psql не найден на хосте. Попытка запуска внутри контейнера...${NC}"
    
    # Проверяем наличие Docker и контейнера
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}Ошибка: psql не найден и Docker недоступен.${NC}"
        echo -e "${YELLOW}Установите psql или Docker для продолжения.${NC}"
        exit 1
    fi
    
    # Проверяем, запущен ли контейнер
    if ! docker ps --format '{{.Names}}' | grep -q "^spring-digital-library-db$"; then
        echo -e "${RED}Ошибка: Контейнер spring-digital-library-db не запущен.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}Контейнер найден. Запуск скрипта внутри контейнера...${NC}"
    echo ""
    
    # Запускаем скрипт внутри контейнера
    # Передаем переменные окружения через -e опции
    docker exec -i \
        -e DB_NAME="$DB_NAME" \
        -e DB_ADMIN_USER="$DB_ADMIN_USER" \
        -e DB_ADMIN_PASSWORD="$DB_ADMIN_PASSWORD" \
        -e DB_HOST="${DB_HOST:-localhost}" \
        -e DB_PORT="${DB_PORT:-5432}" \
        -e READONLY_USER="$READONLY_USER" \
        -e READONLY_PASSWORD="$READONLY_PASSWORD" \
        spring-digital-library-db bash -s -- "$@" <<'CONTAINER_SCRIPT'
set -e

# Параметры подключения к БД (внутри контейнера используем localhost)
DB_NAME="${DB_NAME:-spring_digital_bookstore}"
DB_ADMIN_USER="${DB_ADMIN_USER:-nobugs228}"
DB_ADMIN_PASSWORD="${DB_ADMIN_PASSWORD:-nobugs228PASSWORD!#}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"

# Параметры нового пользователя (приоритет: переменные окружения > аргументы > значения по умолчанию)
if [ -z "$READONLY_USER" ] && [ -n "$1" ]; then
    READONLY_USER="$1"
elif [ -z "$READONLY_USER" ]; then
    READONLY_USER="readonly_user"
fi

if [ -z "$READONLY_PASSWORD" ] && [ -n "$2" ]; then
    READONLY_PASSWORD="$2"
elif [ -z "$READONLY_PASSWORD" ]; then
    READONLY_PASSWORD="pass_123_XYZ!"
fi

# Функция для экранирования пароля в SQL
escape_sql_string() {
    local str="$1"
    str="${str//\\/\\\\}"
    str="${str//\'/\'\'}"
    echo "$str"
}

# Цвета для вывода
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}Создание пользователя PostgreSQL с правами read-only${NC}"
echo "=========================================="
echo "База данных: $DB_NAME"
echo "Новый пользователь: $READONLY_USER"
echo "=========================================="
echo ""

export PGPASSWORD="$DB_ADMIN_PASSWORD"

echo -e "${YELLOW}Проверка подключения к базе данных...${NC}"
if ! psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d "$DB_NAME" -c "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${RED}Ошибка: Не удалось подключиться к базе данных${NC}"
    exit 1
fi

echo -e "${GREEN}Подключение успешно!${NC}"
echo ""

USER_EXISTS=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='$READONLY_USER'")

if [ "$USER_EXISTS" = "1" ]; then
    echo -e "${YELLOW}Пользователь '$READONLY_USER' уже существует.${NC}"
    echo "Удаление существующего пользователя..."
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d postgres <<EOF
REVOKE ALL PRIVILEGES ON DATABASE $DB_NAME FROM $READONLY_USER;
EOF
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d "$DB_NAME" <<EOF
REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM $READONLY_USER;
REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM $READONLY_USER;
REVOKE USAGE ON SCHEMA public FROM $READONLY_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM $READONLY_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM $READONLY_USER;
EOF
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d postgres <<EOF
DROP ROLE IF EXISTS $READONLY_USER;
EOF
    echo -e "${GREEN}Пользователь удален.${NC}"
fi

echo -e "${YELLOW}Создание пользователя '$READONLY_USER'...${NC}"
ESCAPED_PASSWORD=$(escape_sql_string "$READONLY_PASSWORD")
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d postgres <<EOF
CREATE ROLE $READONLY_USER WITH LOGIN PASSWORD '$ESCAPED_PASSWORD';
EOF

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Пользователь создан успешно!${NC}"
else
    echo -e "${RED}Ошибка при создании пользователя${NC}"
    exit 1
fi

psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d postgres <<EOF
GRANT CONNECT ON DATABASE $DB_NAME TO $READONLY_USER;
EOF

psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d "$DB_NAME" <<EOF
GRANT USAGE ON SCHEMA public TO $READONLY_USER;
EOF

psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d "$DB_NAME" <<EOF
GRANT SELECT ON ALL TABLES IN SCHEMA public TO $READONLY_USER;
EOF

psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d "$DB_NAME" <<EOF
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO $READONLY_USER;
EOF

psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d "$DB_NAME" <<EOF
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO $READONLY_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO $READONLY_USER;
EOF

echo ""
echo -e "${GREEN}=========================================="
echo "Пользователь успешно создан!"
echo "==========================================${NC}"
echo ""
echo "Параметры подключения:"
echo "  Database: $DB_NAME"
echo "  Username: $READONLY_USER"
echo "  Password: $READONLY_PASSWORD"
echo ""
echo -e "${YELLOW}ВНИМАНИЕ: Сохраните пароль в безопасном месте!${NC}"
CONTAINER_SCRIPT
    
    # Выходим после запуска внутри контейнера
    exit $?
fi

# Устанавливаем пароль для psql через переменную окружения
export PGPASSWORD="$DB_ADMIN_PASSWORD"

# Проверка подключения к БД
echo -e "${YELLOW}Проверка подключения к базе данных...${NC}"
echo -e "${YELLOW}Хост: $DB_HOST, Порт: $DB_PORT, Пользователь: $DB_ADMIN_USER, БД: $DB_NAME${NC}"
if ! psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d "$DB_NAME" -c "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${RED}Ошибка: Не удалось подключиться к базе данных${NC}"
    echo -e "${RED}Проверьте параметры: DB_HOST=$DB_HOST, DB_PORT=$DB_PORT, DB_ADMIN_USER=$DB_ADMIN_USER, DB_NAME=$DB_NAME${NC}"
    echo -e "${YELLOW}Подсказка: Установите переменную окружения DB_ADMIN_PASSWORD если пароль отличается${NC}"
    echo -e "${YELLOW}Или запустите скрипт внутри контейнера: docker exec -i spring-digital-library-db bash -s < create_readonly_user_docker.sh${NC}"
    exit 1
fi

echo -e "${GREEN}Подключение успешно!${NC}"
echo ""

# Проверка существования пользователя
echo -e "${YELLOW}Проверка существования пользователя '$READONLY_USER'...${NC}"
USER_EXISTS=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='$READONLY_USER'")

if [ "$USER_EXISTS" = "1" ]; then
    echo -e "${YELLOW}Пользователь '$READONLY_USER' уже существует.${NC}"
    echo "Удаление существующего пользователя..."
    # Отзываем все права на базе данных
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d postgres <<EOF
REVOKE ALL PRIVILEGES ON DATABASE $DB_NAME FROM $READONLY_USER;
EOF
    # Отзываем все права на объектах в базе данных
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d "$DB_NAME" <<EOF
REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM $READONLY_USER;
REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM $READONLY_USER;
REVOKE USAGE ON SCHEMA public FROM $READONLY_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM $READONLY_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM $READONLY_USER;
EOF
    # Теперь можно безопасно удалить пользователя
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d postgres <<EOF
DROP ROLE IF EXISTS $READONLY_USER;
EOF
    echo -e "${GREEN}Пользователь удален.${NC}"
fi

# Создание пользователя
echo -e "${YELLOW}Создание пользователя '$READONLY_USER'...${NC}"
ESCAPED_PASSWORD=$(escape_sql_string "$READONLY_PASSWORD")
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d postgres <<EOF
CREATE ROLE $READONLY_USER WITH LOGIN PASSWORD '$ESCAPED_PASSWORD';
EOF

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Пользователь создан успешно!${NC}"
else
    echo -e "${RED}Ошибка при создании пользователя${NC}"
    exit 1
fi

# Предоставление прав на подключение к базе данных
echo -e "${YELLOW}Предоставление прав на подключение к базе данных...${NC}"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d postgres <<EOF
GRANT CONNECT ON DATABASE $DB_NAME TO $READONLY_USER;
EOF

# Предоставление прав на использование схемы public
echo -e "${YELLOW}Предоставление прав на использование схемы public...${NC}"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d "$DB_NAME" <<EOF
GRANT USAGE ON SCHEMA public TO $READONLY_USER;
EOF

# Предоставление прав SELECT на все существующие таблицы
echo -e "${YELLOW}Предоставление прав SELECT на все существующие таблицы...${NC}"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d "$DB_NAME" <<EOF
GRANT SELECT ON ALL TABLES IN SCHEMA public TO $READONLY_USER;
EOF

# Предоставление прав SELECT на все будущие таблицы (по умолчанию)
echo -e "${YELLOW}Настройка прав по умолчанию для будущих таблиц...${NC}"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d "$DB_NAME" <<EOF
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO $READONLY_USER;
EOF

# Предоставление прав на использование последовательностей (для чтения)
echo -e "${YELLOW}Предоставление прав на последовательности...${NC}"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d "$DB_NAME" <<EOF
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO $READONLY_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO $READONLY_USER;
EOF

echo ""
echo -e "${GREEN}=========================================="
echo "Пользователь успешно создан!"
echo "==========================================${NC}"
echo ""
echo "Параметры подключения:"
echo "  Database: $DB_NAME"
echo "  Username: $READONLY_USER"
echo "  Password: $READONLY_PASSWORD"
echo ""
echo -e "${YELLOW}ВНИМАНИЕ: Сохраните пароль в безопасном месте!${NC}"

