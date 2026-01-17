# Описание реализации функции восстановления пароля

## Обзор

Функция восстановления пароля позволяет пользователям, забывшим свой пароль, запросить сброс пароля через email. Процесс реализован по аналогии с верификацией email и использует ту же архитектуру токенов.

## Архитектурные решения

### 1. Использование существующей инфраструктуры

- **Модель токена:** Сущность `PasswordResetToken` создана по аналогии с `EmailVerificationToken`
- **Сервис:** `PasswordResetService` реализован по аналогии с `EmailVerificationService`
- **Email сервис:** `EmailService` расширен методом `sendPasswordResetEmail()`
- **Repository:** `PasswordResetTokenRepository` создан по аналогии с `EmailVerificationTokenRepository`
- **Безопасность:** Эндпоинты автоматически публичные через существующее правило `/api/v1/auth/**` в `SecurityConfig`

### 2. Безопасность

- Токены действительны ограниченное время (1 час по умолчанию, короче чем для верификации)
- Токены одноразовые (помечаются как использованные после сброса)
- Не раскрывается информация о существовании email в системе (security best practice)
- Валидация нового пароля по тем же правилам, что и при регистрации
- Пароли хешируются через `PasswordEncoder` (BCrypt)

## Реализованные компоненты

### 1. Модель данных

#### 1.1. Сущность PasswordResetToken

**Файл:** `main-app/src/main/java/online/ityura/springdigitallibrary/model/PasswordResetToken.java`

**Структура:**

- `id` (Long) - первичный ключ, автоинкремент
- `token` (String, unique) - UUID токен, уникальный
- `user` (User, ManyToOne, LAZY) - связь с пользователем
- `expiresAt` (LocalDateTime) - время истечения токена
- `createdAt` (LocalDateTime) - время создания (автоматически через `@PrePersist`)
- `used` (Boolean) - флаг использования (по умолчанию false)

**Особенности:**

- Таблица: `password_reset_tokens`
- Индексы на `token` и `user_id` для быстрого поиска
- Уникальное ограничение на `token`
- `@PrePersist` для автоматической установки `createdAt`
- Lombok аннотации: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`

#### 1.2. Repository

**Файл:** `main-app/src/main/java/online/ityura/springdigitallibrary/repository/PasswordResetTokenRepository.java`

**Методы:**

- `Optional<PasswordResetToken> findByToken(String token)` - поиск по токену
- `Optional<PasswordResetToken> findByUser(User user)` - поиск по пользователю
- `void deleteByUser(User user)` - удаление всех токенов пользователя
- `void deleteByExpiresAtBefore(LocalDateTime dateTime)` - удаление истекших токенов (для очистки)

### 2. Сервисный слой

#### 2.1. PasswordResetService

**Файл:** `main-app/src/main/java/online/ityura/springdigitallibrary/service/PasswordResetService.java`

**Зависимости:**

- `PasswordResetTokenRepository`
- `UserRepository`
- `EmailService`
- `PasswordEncoder` (BCrypt)
- `@Value("${app.password.reset.token-expiration-hours:1}")` - время жизни токена (по умолчанию 1 час)

**Методы:**

1. **`generatePasswordResetToken(String email)`**

   - `@Transactional` - транзакционный метод
   - Ищет пользователя по email через `userRepository.findByEmail(email)`
   - Если пользователь не найден → логирует и тихо возвращается (не выбрасывает исключение - security best practice)
   - Удаляет старые токены пользователя через `tokenRepository.deleteByUser(user)`
   - Генерирует новый UUID токен через `UUID.randomUUID().toString()`
   - Создает `PasswordResetToken` с временем истечения `LocalDateTime.now().plusHours(tokenExpirationHours)`
   - Сохраняет токен в БД
   - Отправляет письмо через `emailService.sendPasswordResetEmail(user.getEmail(), token)`
   - Логирует успешную отправку или ошибку
   - При ошибке отправки письма выбрасывает `RuntimeException`

2. **`validateToken(String token)`**

   - Проверяет валидность токена восстановления пароля без сброса пароля
   - Используется для GET запроса при переходе по ссылке из письма
   - Ищет токен в БД через `tokenRepository.findByToken(token)`
   - Если токен не найден → `404 NOT FOUND` с сообщением "Password reset token not found"
   - Проверяет, не использован ли токен (`used = true`) → `410 GONE` с сообщением "Password reset token has already been used"
   - Проверяет срок действия (`expiresAt.isBefore(LocalDateTime.now())`) → `410 GONE` с сообщением "Password reset token has expired"
   - Не изменяет состояние токена (не помечает как использованный)
   - Не изменяет пароль пользователя

3. **`resetPassword(String token, String newPassword)`**
   - `@Transactional` - транзакционный метод
   - Ищет токен в БД через `tokenRepository.findByToken(token)`
   - Если токен не найден → `404 NOT FOUND` с сообщением "Password reset token not found"
   - Проверяет, не использован ли токен (`used = true`) → `410 GONE` с сообщением "Password reset token has already been used"
   - Проверяет срок действия (`expiresAt.isBefore(LocalDateTime.now())`) → `410 GONE` с сообщением "Password reset token has expired"
   - Получает пользователя из токена
   - Валидация пароля выполняется на уровне DTO через аннотации `@Valid`
   - Хеширует новый пароль через `passwordEncoder.encode(newPassword)`
   - Обновляет `user.passwordHash` и сохраняет пользователя
   - Помечает токен как использованный (`used = true`) и сохраняет
   - Логирует успешный сброс пароля

#### 2.2. Расширение EmailService

**Файл:** `main-app/src/main/java/online/ityura/springdigitallibrary/service/EmailService.java`

**Новый метод:** `sendPasswordResetEmail(String toEmail, String resetToken)`

**Реализация:**

- Использует `JavaMailSender` для отправки
- Кодировка: UTF-8
- Отправитель: `fromEmail` (из настроек `spring.mail.properties.mail.smtp.from`)
- Тема письма: "Восстановление пароля"
- Формирует URL: `{baseUrl}/api/v1/auth/reset-password?token={resetToken}`
- Использует `app.email.verification.base-url` для формирования ссылки
- Создает HTML и текстовые версии письма
- При ошибке выбрасывает `RuntimeException` с сообщением "Failed to send password reset email"

**Содержимое письма (HTML):**

- Приветствие
- Информация о запросе на сброс пароля
- Кнопка "Сбросить пароль" со ссылкой
- Текстовая ссылка для копирования
- Информация о сроке действия (1 час)
- Предупреждение: если запрос не делался, проигнорировать письмо
- Инструкция: после сброса пароля можно войти в систему с новым паролем
- Подпись: "Команда Spring Digital Library"

**Содержимое письма (текстовая версия):**

- Аналогичное содержимое в текстовом формате

### 3. DTO

#### 3.1. ForgotPasswordRequest

**Файл:** `main-app/src/main/java/online/ityura/springdigitallibrary/dto/request/ForgotPasswordRequest.java`

**Поля:**

- `email` (String)
  - `@NotBlank(message = "Email is required")`
  - `@Email(message = "Email should be valid")`
  - Swagger: `@Schema(description = "Email пользователя", example = "john@example.com", requiredMode = Schema.RequiredMode.REQUIRED)`

**Наследование:** `extends BaseDto`

#### 3.2. ResetPasswordRequest

**Файл:** `main-app/src/main/java/online/ityura/springdigitallibrary/dto/request/ResetPasswordRequest.java`

**Поля:**

- `token` (String)

  - `@NotBlank(message = "Token is required")`
  - Swagger: `@Schema(description = "Токен восстановления пароля", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)`

- `newPassword` (String)
  - `@NotBlank(message = "Password is required")`
  - `@Size(min = 8, message = "Password must be at least 8 characters long")`
  - `@Pattern(regexp = "^(?=\\S+$)(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&+=]).{8,}$", message = "Password must contain at least one digit, one lower case, one upper case, one special character, no spaces, and be at least 8 characters long")`
  - Swagger: `@Schema(description = "Новый пароль", example = "NewPassword123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8)`

**Наследование:** `extends BaseDto`

**Примечание:** Валидация пароля идентична валидации в `RegisterRequest` (тот же regex паттерн).

#### 3.3. Response DTO

Используется существующий `MessageResponse` для всех ответов:

- `message` (String) - текстовое сообщение

### 4. Контроллер

#### 4.1. Эндпоинты в AuthController

**Файл:** `main-app/src/main/java/online/ityura/springdigitallibrary/controller/AuthController.java`

**Новые эндпоинты:**

1. **POST `/api/v1/auth/forgot-password`**

   - Принимает `@Valid @RequestBody ForgotPasswordRequest`
   - Вызывает `passwordResetService.generatePasswordResetToken(request.getEmail())`
   - Всегда возвращает `200 OK` с `MessageResponse`
   - Response body: `{ "message": "If the email exists, a password reset link has been sent" }`
   - Swagger аннотации:
     - `@Operation` с описанием
     - `@ApiResponses` с примерами для 200 и 400

2. **GET `/api/v1/auth/reset-password?token={token}`**

   - Принимает `@RequestParam String token` из query параметра
   - Вызывает `passwordResetService.validateToken(token)`
   - Используется при переходе по ссылке из письма (браузер делает GET запрос)
   - Возвращает `200 OK` при валидном токене
   - Response body: `{ "message": "Token is valid" }`
   - Swagger аннотации:
     - `@Operation` с описанием проверки токена
     - `@ApiResponses` с примерами для 200, 404, 410

3. **POST `/api/v1/auth/reset-password`**
   - Принимает `@Valid @RequestBody ResetPasswordRequest`
   - Вызывает `passwordResetService.resetPassword(request.getToken(), request.getNewPassword())`
   - Возвращает `200 OK` при успехе
   - Response body: `{ "message": "Password has been reset successfully" }`
   - Swagger аннотации:
     - `@Operation` с описанием
     - `@ApiResponses` с примерами для 200, 400, 404, 410

**Зависимости контроллера:**

- `AuthService`
- `EmailVerificationService`
- `PasswordResetService` (новый)

### 5. Конфигурация

#### 5.1. Настройки в application.properties

**Файл:** `main-app/src/main/resources/application.properties`

**Добавлена секция:**

```properties
# ===============================
# НАСТРОЙКИ ВОССТАНОВЛЕНИЯ ПАРОЛЯ
# ===============================

# Время жизни токена восстановления пароля (в часах)
# Можно переопределить через переменную окружения APP_PASSWORD_RESET_TOKEN_EXPIRATION_HOURS
app.password.reset.token-expiration-hours=${APP_PASSWORD_RESET_TOKEN_EXPIRATION_HOURS:1}
```

**Используемые существующие настройки:**

- `app.email.verification.base-url` - для формирования ссылок в письмах

### 6. Безопасность

#### 6.1. Публичные эндпоинты

**Файл:** `main-app/src/main/java/online/ityura/springdigitallibrary/security/SecurityConfig.java`

Эндпоинты автоматически публичные через существующее правило:

```java
.requestMatchers("/api/v1/auth/**").permitAll()
```

**Примечание:** Отдельные исключения для `/api/v1/auth/forgot-password` и `/api/v1/auth/reset-password` не требуются, так как они уже покрыты общим правилом `/api/v1/auth/**`.

## API Спецификация

### 1. Запрос на восстановление пароля

**Endpoint:** `POST /api/v1/auth/forgot-password`

**Request Headers:**

```
Content-Type: application/json
```

**Request Body:**

```json
{
  "email": "john@example.com"
}
```

**Валидация:**

- `email`: обязательное поле, валидный email формат

**Response (200 OK):**

```json
{
  "message": "If the email exists, a password reset link has been sent"
}
```

**Особенности:**

- Всегда возвращает 200 OK, даже если email не найден (security best practice)
- Не раскрывает информацию о существовании пользователя в системе
- Если email существует, отправляется письмо с токеном
- Если email не существует, операция просто завершается без отправки письма

**Возможные ошибки:**

**400 BAD REQUEST** - Ошибки валидации:

```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "fieldErrors": {
    "email": "Email should be valid"
  },
  "timestamp": "2025-12-17T13:20:00Z",
  "path": "/api/v1/auth/forgot-password"
}
```

### 2. Проверка токена восстановления пароля

**Endpoint:** `GET /api/v1/auth/reset-password?token={token}`

**Параметры:**

- `token` (String, обязательный, query parameter) - UUID токен из письма

**Response (200 OK):**

```json
{
  "message": "Token is valid"
}
```

**Возможные ошибки:**

**404 NOT FOUND** - Токен не найден:

```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Password reset token not found",
  "timestamp": "2025-12-17T13:20:00Z",
  "path": "/api/v1/auth/reset-password"
}
```

**410 GONE** - Токен истек:

```json
{
  "status": 410,
  "error": "GONE",
  "message": "Password reset token has expired",
  "timestamp": "2025-12-17T13:20:00Z",
  "path": "/api/v1/auth/reset-password"
}
```

**410 GONE** - Токен уже использован:

```json
{
  "status": 410,
  "error": "GONE",
  "message": "Password reset token has already been used",
  "timestamp": "2025-12-17T13:20:00Z",
  "path": "/api/v1/auth/reset-password"
}
```

**Особенности:**

- Используется при переходе по ссылке из письма (браузер автоматически делает GET запрос)
- Не сбрасывает пароль, только проверяет валидность токена
- Не изменяет состояние токена (не помечает как использованный)
- Фронтенд может использовать этот эндпоинт для проверки токена перед показом формы

### 3. Сброс пароля

**Endpoint:** `POST /api/v1/auth/reset-password`

**Request Headers:**

```
Content-Type: application/json
```

**Request Body:**

```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "NewPassword123!"
}
```

**Валидация:**

- `token`: обязательное поле
- `newPassword`: обязательное поле, должно соответствовать правилам:
  - Минимум 8 символов
  - Минимум одна цифра (0-9)
  - Минимум одна строчная буква (a-z)
  - Минимум одна заглавная буква (A-Z)
  - Минимум один специальный символ (!@#$%^&+=)
  - Без пробелов
  - Regex: `^(?=\\S+$)(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&+=]).{8,}$`

**Response (200 OK):**

```json
{
  "message": "Password has been reset successfully"
}
```

**Возможные ошибки:**

**400 BAD REQUEST** - Ошибки валидации:

```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "fieldErrors": {
    "newPassword": "Password must contain at least one digit, one lower case, one upper case, one special character, no spaces, and be at least 8 characters long"
  },
  "timestamp": "2025-12-17T13:20:00Z",
  "path": "/api/v1/auth/reset-password"
}
```

**404 NOT FOUND** - Токен не найден:

```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Password reset token not found",
  "timestamp": "2025-12-17T13:20:00Z",
  "path": "/api/v1/auth/reset-password"
}
```

**410 GONE** - Токен истек:

```json
{
  "status": 410,
  "error": "GONE",
  "message": "Password reset token has expired",
  "timestamp": "2025-12-17T13:20:00Z",
  "path": "/api/v1/auth/reset-password"
}
```

**410 GONE** - Токен уже использован:

```json
{
  "status": 410,
  "error": "GONE",
  "message": "Password reset token has already been used",
  "timestamp": "2025-12-17T13:20:00Z",
  "path": "/api/v1/auth/reset-password"
}
```

## Письмо с токеном восстановления

**Тема:** "Восстановление пароля"

**Отправитель:** Из настройки `spring.mail.properties.mail.smtp.from` (по умолчанию `noreply@localhost`)

**Формат ссылки:**

```
{app.email.verification.base-url}/api/v1/auth/reset-password?token={uuid-token}
```

**Пример:** `http://localhost:8080/api/v1/auth/reset-password?token=550e8400-e29b-41d4-a716-446655440000`

**Содержимое письма:**

**HTML версия:**

- Заголовок: "Восстановление пароля"
- Приветствие: "Здравствуйте!"
- Текст: "Мы получили запрос на восстановление пароля для вашего аккаунта. Если это были вы, пожалуйста, перейдите по ссылке ниже для сброса пароля:"
- Кнопка: "Сбросить пароль" (ссылка)
- Текстовая ссылка для копирования
- Важно: "Ссылка действительна в течение 1 часа."
- Предупреждение: "Если вы не запрашивали восстановление пароля, пожалуйста, проигнорируйте это письмо. Ваш пароль останется без изменений."
- Инструкция: "После сброса пароля вы сможете войти в систему с новым паролем."
- Подпись: "С уважением, Команда Spring Digital Library"

**Текстовая версия:**

- Аналогичное содержимое в текстовом формате

## Полный поток восстановления пароля

```
1. Пользователь забыл пароль
   ↓
2. POST /api/v1/auth/forgot-password
   Request: { "email": "user@example.com" }
   ↓
3. Бэкенд ищет пользователя по email через UserRepository
   ↓
4. Если пользователь НЕ найден:
   - Логируется информация: "Password reset requested for non-existent email"
   - Метод тихо возвращается (не выбрасывает исключение)
   - Ответ: 200 OK { "message": "If the email exists, a password reset link has been sent" }
   ↓
5. Если пользователь найден:
   - Удаляются старые токены восстановления через deleteByUser()
   - Генерируется новый UUID токен
   - Создается PasswordResetToken с expiresAt = now + 1 час
   - Токен сохраняется в БД
   - Отправляется письмо через EmailService.sendPasswordResetEmail()
   - Логируется: "Password reset email sent to: {email}"
   - Ответ: 200 OK { "message": "If the email exists, a password reset link has been sent" }
   ↓
6. Пользователь открывает письмо и переходит по ссылке
   Ссылка: {baseUrl}/api/v1/auth/reset-password?token={token}
   ↓
7. Браузер автоматически делает GET запрос на /api/v1/auth/reset-password?token={token}
   ↓
8. Бэкенд проверяет токен через PasswordResetService.validateToken()
   ↓
9. Если токен валиден:
    - Ответ: 200 OK { "message": "Token is valid" }
    - Фронтенд извлекает токен из query параметра и показывает форму для ввода нового пароля
   ↓
10. Если токен невалиден:
    - Ответ: 404 или 410 с соответствующим сообщением
    - Фронтенд показывает ошибку и предлагает запросить новую ссылку
   ↓
11. POST /api/v1/auth/reset-password
   Request: { "token": "...", "newPassword": "..." }
   ↓
12. Валидация DTO (@Valid):
    - token: обязательное
    - newPassword: обязательное, валидация по regex
    ↓
13. Бэкенд ищет токен через PasswordResetTokenRepository.findByToken()
    ↓
14. Если токен не найден:
    - Ответ: 404 NOT FOUND { "message": "Password reset token not found" }
    ↓
15. Если токен найден, проверяется:
    - used == true? → 410 GONE { "message": "Password reset token has already been used" }
    - expiresAt < now? → 410 GONE { "message": "Password reset token has expired" }
    ↓
16. Если токен валиден:
    - Получается пользователь из токена
    - Новый пароль хешируется через PasswordEncoder.encode()
    - Обновляется user.passwordHash
    - Пользователь сохраняется в БД
    - Токен помечается как used = true и сохраняется
    - Логируется: "Password reset successfully for user: {email}"
    - Ответ: 200 OK { "message": "Password has been reset successfully" }
    ↓
17. Пользователь может войти в систему с новым паролем через POST /api/v1/auth/login
```

## Инструкции для фронтенда

### 1. Страница "Забыли пароль?"

**URL:** `/forgot-password`

**Форма:**

- Поле: Email (тип email, обязательное)
- Кнопка: "Отправить ссылку для восстановления"

**Обработка:**

```javascript
async function forgotPassword(email) {
  try {
    const response = await fetch("/api/v1/auth/forgot-password", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ email }),
    });

    if (response.ok) {
      // Всегда показываем успешное сообщение (security best practice)
      const data = await response.json();
      showMessage(data.message);
      // Можно перенаправить на страницу с инструкцией
      navigateTo("/forgot-password-success");
    } else if (response.status === 400) {
      const error = await response.json();
      showValidationErrors(error.fieldErrors);
    } else {
      showError("An error occurred. Please try again.");
    }
  } catch (error) {
    showError("Network error. Please check your connection.");
  }
}
```

### 2. Страница успешной отправки

**URL:** `/forgot-password-success`

**Содержимое:**

- Сообщение об отправке письма
- Инструкция проверить email (включая папку спам)
- Информация о сроке действия ссылки (1 час)
- Ссылка на страницу входа

### 3. Страница сброса пароля

**URL:** `/reset-password?token={token}`

**Обработка токена из URL:**

```javascript
// Извлечь токен из query параметров
const urlParams = new URLSearchParams(window.location.search);
const token = urlParams.get("token");

if (!token) {
  showError("Invalid reset link. Token is missing.");
  navigateTo("/forgot-password");
  return;
}

// Проверить валидность токена через GET запрос
async function validateToken(token) {
  try {
    const response = await fetch(`/api/v1/auth/reset-password?token=${token}`, {
      method: "GET",
    });

    if (response.ok) {
      // Токен валиден, показываем форму
      showPasswordResetForm(token);
    } else if (response.status === 404) {
      showError("Password reset link is invalid. Please request a new one.");
      navigateTo("/forgot-password");
    } else if (response.status === 410) {
      const error = await response.json();
      if (error.message.includes("expired")) {
        showError("Password reset link has expired. Please request a new one.");
      } else if (error.message.includes("already been used")) {
        showError(
          "Password reset link has already been used. Please request a new one."
        );
      } else {
        showError(
          "Password reset link is no longer valid. Please request a new one."
        );
      }
      navigateTo("/forgot-password");
    } else {
      showError("An error occurred. Please try again.");
    }
  } catch (error) {
    showError("Network error. Please check your connection.");
  }
}

// Вызываем при загрузке страницы
validateToken(token);
```

**Форма:**

- Поле: Новый пароль (тип password, с валидацией на фронтенде)
  - Минимум 8 символов
  - Минимум одна цифра
  - Минимум одна строчная буква
  - Минимум одна заглавная буква
  - Минимум один специальный символ (!@#$%^&+=)
  - Без пробелов
- Поле: Подтверждение пароля (тип password)
- Кнопка: "Сбросить пароль"

**Обработка:**

```javascript
async function resetPassword(token, newPassword) {
  try {
    const response = await fetch("/api/v1/auth/reset-password", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ token, newPassword }),
    });

    if (response.ok) {
      const data = await response.json();
      showMessage(data.message);
      // Перенаправляем на страницу входа
      setTimeout(() => {
        navigateTo("/login");
      }, 2000);
    } else if (response.status === 404) {
      const error = await response.json();
      showError("Password reset link is invalid. Please request a new one.");
      navigateTo("/forgot-password");
    } else if (response.status === 410) {
      const error = await response.json();
      if (error.message.includes("expired")) {
        showError("Password reset link has expired. Please request a new one.");
      } else if (error.message.includes("already been used")) {
        showError(
          "Password reset link has already been used. Please request a new one."
        );
      } else {
        showError(
          "Password reset link is no longer valid. Please request a new one."
        );
      }
      navigateTo("/forgot-password");
    } else if (response.status === 400) {
      const error = await response.json();
      showValidationErrors(error.fieldErrors);
    } else {
      showError("An error occurred. Please try again.");
    }
  } catch (error) {
    showError("Network error. Please check your connection.");
  }
}
```

## Тестирование

### 1. Unit тесты (рекомендуется)

**Файл:** `main-app/src/test/java/online/ityura/springdigitallibrary/service/PasswordResetServiceTest.java`

**Тесты:**

- `testGeneratePasswordResetToken_UserExists_ShouldSendEmail()` - успешная генерация токена
- `testGeneratePasswordResetToken_UserNotExists_ShouldNotThrowException()` - несуществующий email (тихо возвращается)
- `testGeneratePasswordResetToken_ShouldDeleteOldTokens()` - удаление старых токенов
- `testResetPassword_ValidToken_ShouldResetPassword()` - успешный сброс пароля
- `testValidateToken_ValidToken_ShouldReturnSuccess()` - успешная проверка токена
- `testValidateToken_TokenNotFound_ShouldThrow404()` - токен не найден
- `testValidateToken_TokenExpired_ShouldThrow410()` - истекший токен
- `testValidateToken_TokenUsed_ShouldThrow410()` - использованный токен
- `testValidateToken_ShouldNotMarkTokenAsUsed()` - токен не помечается как использованный при проверке
- `testResetPassword_ValidToken_ShouldResetPassword()` - успешный сброс пароля
- `testResetPassword_TokenNotFound_ShouldThrow404()` - токен не найден
- `testResetPassword_TokenExpired_ShouldThrow410()` - истекший токен
- `testResetPassword_TokenUsed_ShouldThrow410()` - использованный токен
- `testResetPassword_ShouldMarkTokenAsUsed()` - токен помечается как использованный
- `testResetPassword_ShouldHashPassword()` - пароль хешируется

### 2. Integration тесты (рекомендуется)

**Файл:** `main-app/src/test/java/online/ityura/springdigitallibrary/controller/AuthControllerTest.java`

**Тесты:**

- `testForgotPassword_ValidEmail_ShouldReturn200()` - успешный запрос
- `testForgotPassword_InvalidEmail_ShouldReturn400()` - невалидный email
- `testForgotPassword_NonExistentEmail_ShouldReturn200()` - несуществующий email (все равно 200)
- `testValidateResetToken_ValidToken_ShouldReturn200()` - успешная проверка токена (GET)
- `testValidateResetToken_InvalidToken_ShouldReturn404()` - невалидный токен (GET)
- `testValidateResetToken_ExpiredToken_ShouldReturn410()` - истекший токен (GET)
- `testValidateResetToken_UsedToken_ShouldReturn410()` - использованный токен (GET)
- `testResetPassword_ValidToken_ShouldReturn200()` - успешный сброс (POST)
- `testResetPassword_InvalidToken_ShouldReturn404()` - невалидный токен (POST)
- `testResetPassword_ExpiredToken_ShouldReturn410()` - истекший токен (POST)
- `testResetPassword_UsedToken_ShouldReturn410()` - использованный токен (POST)
- `testResetPassword_InvalidPassword_ShouldReturn400()` - невалидный пароль (POST)

### 3. Локальное тестирование с MailHog

Использовать MailHog для проверки отправки писем (как описано в `email-verification-registration-description.md`):

1. Запустить MailHog через Docker Compose
2. Отправить запрос `POST /api/v1/auth/forgot-password` с валидным email
3. Проверить письмо в MailHog UI (обычно `http://localhost:8025`)
4. Перейти по ссылке из письма (или скопировать токен)
5. Проверить GET запрос `GET /api/v1/auth/reset-password?token={token}` - должен вернуть 200 OK
6. Отправить запрос `POST /api/v1/auth/reset-password` с токеном и новым паролем
7. Проверить, что пароль изменился (попробовать войти с новым паролем)

## Миграция базы данных

### Автоматическое создание таблицы

Если используется JPA с `spring.jpa.hibernate.ddl-auto=update` или `create`, таблица создастся автоматически при запуске приложения.

### SQL для ручного создания таблицы (PostgreSQL)

```sql
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT idx_token UNIQUE (token)
);

CREATE INDEX idx_token ON password_reset_tokens(token);
CREATE INDEX idx_user_id ON password_reset_tokens(user_id);
```

**Примечание:** Для PostgreSQL используется `BIGSERIAL` вместо `AUTO_INCREMENT`.

## Резюме

### Ключевые моменты реализации

1. ✅ Используется та же архитектура, что и для верификации email
2. ✅ Токены действительны 1 час (настраивается через `app.password.reset.token-expiration-hours`)
3. ✅ Токены одноразовые (помечаются как использованные после сброса)
4. ✅ Не раскрывается информация о существовании email (security best practice)
5. ✅ Валидация пароля идентична валидации при регистрации (тот же regex)
6. ✅ Пароли хешируются через BCrypt (PasswordEncoder)
7. ✅ Публичные эндпоинты (не требуют JWT) через существующее правило `/api/v1/auth/**`
8. ✅ Полное логирование операций для отладки
9. ✅ Swagger документация для всех эндпоинтов
10. ✅ GET эндпоинт для проверки токена при переходе по ссылке из письма (браузер делает GET запрос)

### API Endpoints

| Метод | Endpoint                       | Описание                 | Требует JWT | Требует верификации |
| ----- | ------------------------------ | ------------------------ | ----------- | ------------------- |
| POST  | `/api/v1/auth/forgot-password` | Запрос на восстановление | ❌          | ❌                  |
| GET   | `/api/v1/auth/reset-password`  | Проверка токена          | ❌          | ❌                  |
| POST  | `/api/v1/auth/reset-password`  | Сброс пароля по токену   | ❌          | ❌                  |

### Созданные файлы

**Новые файлы:**

1. `model/PasswordResetToken.java` - сущность токена
2. `repository/PasswordResetTokenRepository.java` - репозиторий для токенов
3. `service/PasswordResetService.java` - сервис восстановления пароля
4. `dto/request/ForgotPasswordRequest.java` - DTO запроса на восстановление
5. `dto/request/ResetPasswordRequest.java` - DTO запроса на сброс пароля

**Изменения в существующих файлах:**

1. `service/EmailService.java` - добавлен метод `sendPasswordResetEmail()` и вспомогательные методы `buildPasswordResetEmailHtml()`, `buildPasswordResetEmailText()`
2. `controller/AuthController.java` - добавлены три новых эндпоинта (POST forgot-password, GET reset-password, POST reset-password) с Swagger аннотациями
3. `application.properties` - добавлена настройка `app.password.reset.token-expiration-hours`

**Файлы, которые НЕ требуют изменений:**

1. `security/SecurityConfig.java` - эндпоинты уже публичные через `/api/v1/auth/**`

### Порядок реализации (выполнено)

1. ✅ Модель и Repository (Этап 1)
2. ✅ Сервисный слой (Этап 2)
3. ✅ DTO (Этап 3)
4. ✅ Контроллер (Этап 4)
5. ✅ Конфигурация (Этап 5)
6. ✅ Безопасность (Этап 6 - проверено, изменения не требуются)
7. ⏳ Тестирование (рекомендуется)
8. ⏳ Документация для фронтенда (опционально)

### Дополнительные рекомендации

1. **Rate Limiting** (опционально): Для защиты от злоупотреблений рекомендуется добавить rate limiting:

   - Максимум 3 запроса на восстановление пароля с одного IP в час
   - Можно использовать Spring Boot Actuator или библиотеку Bucket4j

2. **Очистка истекших токенов** (опционально): Создать scheduled task для периодической очистки:

   ```java
   @Scheduled(cron = "0 0 2 * * ?") // Каждый день в 2:00
   public void cleanupExpiredTokens() {
       tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
   }
   ```

3. **Мониторинг**: Добавить метрики для отслеживания:
   - Количество запросов на восстановление пароля
   - Количество успешных сбросов пароля
   - Количество истекших/использованных токенов
