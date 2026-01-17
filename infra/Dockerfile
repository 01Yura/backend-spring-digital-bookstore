# Многоэтапная сборка для оптимизации размера образа
# Этап 1: Сборка приложения
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Копируем pom.xml и загружаем зависимости (кэшируется если pom.xml не изменился)
COPY pom.xml .
# Копируем pom.xml модулей для правильной сборки
COPY main-app/pom.xml ./main-app/
COPY common-dto/pom.xml ./common-dto/
COPY analytics-service/pom.xml ./analytics-service/
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B -pl main-app -am

# Копируем исходный код и собираем приложение
COPY main-app/src ./main-app/src
COPY common-dto/src ./common-dto/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -Dmaven.test.skip=true -B -pl main-app -am

# Этап 2: Финальный образ с минимальным размером
FROM eclipse-temurin:21-jre-alpine

# Создаем пользователя для запуска приложения (безопасность)
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Копируем исходные изображения книг в отдельную директорию внутри контейнера.
# DataInitializer будет использовать эту директорию как источник картинок при первом запуске.
COPY main-app/src/main/resources/pictures /opt/spring-digital-bookstore/pictures-source

# Копируем исходные PDF файлы книг в отдельную директорию внутри контейнера.
# DataInitializer будет использовать эту директорию как источник PDF файлов при первом запуске.
COPY main-app/src/main/resources/pdf /opt/spring-digital-bookstore/pdf-source

# Создаем директории для хранения изображений и PDF файлов книг
# Делаем это до переключения на пользователя spring, чтобы иметь права на создание директорий
# Если volume будет смонтирован, эти директории будут использованы как точки монтирования
RUN mkdir -p /opt/spring-digital-bookstore/pictures && \
    mkdir -p /opt/spring-digital-bookstore/pdf && \
    chown -R spring:spring /opt/spring-digital-bookstore

# Копируем JAR файл из этапа сборки
COPY --from=build /app/main-app/target/main-app-*.jar app.jar

# Меняем владельца файла
RUN chown spring:spring app.jar

# Переключаемся на пользователя spring
USER spring

# Открываем порт (по умолчанию Spring Boot использует 8080)
EXPOSE 8080

# Переменные окружения для JVM оптимизации
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Запускаем приложение
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

