FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Копируем parent pom.xml (нужен для зависимостей)
COPY pom.xml /app/pom-original.xml

# Создаем временный pom.xml без main-app модуля
# Используем sed для удаления строки с main-app модулем
RUN sed '/<module>main-app<\/module>/d' /app/pom-original.xml > /app/pom.xml

# Копируем common-dto модуль (зависимость analytics-service)
COPY common-dto/pom.xml /app/common-dto/
COPY common-dto/src /app/common-dto/src

# Копируем analytics-service
COPY analytics-service/pom.xml /app/analytics-service/
COPY analytics-service/src /app/analytics-service/src

# Собираем только нужные модули (common-dto и analytics-service)
# -pl: projects list (список модулей для сборки)
# -am: also-make (собрать также зависимости)
RUN mvn clean install -DskipTests -pl common-dto,analytics-service -am

# Финальный образ
FROM eclipse-temurin:21-jre
WORKDIR /app

# Копируем JAR файл
COPY --from=build /app/analytics-service/target/analytics-service-*.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]
