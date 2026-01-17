# Процесс регистрации и верификации email

## Обзор

Система регистрации требует обязательной верификации email перед входом в систему. При регистрации пользователь создается с флагом `isVerified = false`, и ему отправляется письмо с токеном верификации. JWT токены выдаются только после успешной верификации email при входе в систему.

## Процесс регистрации

### 1. Запрос на регистрацию

**Endpoint:** `POST /api/v1/auth/register`

**Request Body:**

```json
{
  "nickname": "john_doe",
  "email": "john@example.com",
  "password": "Password123!"
}
```

**Валидация полей:**

- `nickname`: обязательное, 3-50 символов, только буквы, цифры, дефисы, подчеркивания и точки
- `email`: обязательное, валидный email формат
- `password`: обязательное, минимум 8 символов, должен содержать:
  - минимум одну цифру
  - минимум одну строчную букву
  - минимум одну заглавную букву
  - минимум один специальный символ (!@#$%^&+=)
  - без пробелов

### 2. Обработка регистрации на бэкенде

**Файл:** `main-app/src/main/java/online/ityura/springdigitallibrary/service/AuthService.java`

Процесс регистрации:

1. **Проверка уникальности email:**

   - Если email уже существует → `409 CONFLICT` с сообщением "Email already exists"

2. **Создание пользователя:**

   - Пароль хешируется с помощью `PasswordEncoder`
   - Роль устанавливается как `Role.USER`
   - Флаг `isVerified` устанавливается в `false`
   - Пользователь сохраняется в базу данных

3. **Генерация токена верификации:**

   - Вызывается `EmailVerificationService.generateVerificationToken(user)`
   - Старые токены пользователя удаляются
   - Генерируется новый UUID токен
   - Токен сохраняется в базу с временем истечения (по умолчанию 24 часа)
   - Флаг `used` устанавливается в `false`

4. **Отправка письма:**

   - Вызывается `EmailService.sendVerificationEmail(email, token)`
   - Создается письмо с темой "Подтвердите ваш email"
   - В письме содержится ссылка: `{baseUrl}/api/v1/auth/verify-email?token={token}`
   - Если отправка письма не удалась, ошибка логируется, но регистрация считается успешной

5. **Формирование ответа:**
   - Возвращается `RegisterResponse` со статусом `201 CREATED`
   - **Важно:** JWT токены НЕ включаются в ответ

### 3. Ответ при успешной регистрации

**HTTP Status:** `201 CREATED`

**Response Body:**

```json
{
  "userId": 1,
  "email": "john@example.com",
  "role": "USER",
  "message": "Registration successful! Please check your email and click the verification link to activate your account."
}
```

**Поля ответа:**

- `userId` (Long) - ID созданного пользователя
- `email` (String) - Email пользователя
- `role` (Role) - Роль пользователя (USER)
- `message` (String) - Инструкция для пользователя

**Отсутствующие поля (намеренно):**

- `accessToken` - НЕ выдается при регистрации
- `refreshToken` - НЕ выдается при регистрации
- `tokenType` - НЕ выдается при регистрации

### 4. Возможные ошибки при регистрации

**400 BAD REQUEST** - Ошибки валидации:

```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "fieldErrors": {
    "password": "Password must contain at least one digit, one lower case, one upper case, one special character, no spaces, and be at least 8 characters long",
    "nickname": "Nickname must contain only letters, digits, dashes, underscores, and dots. Spaces and other special characters are not allowed",
    "email": "Email should be valid"
  },
  "timestamp": "2025-12-17T13:20:00Z",
  "path": "/api/v1/auth/register"
}
```

**409 CONFLICT** - Email уже существует:

```json
{
  "status": 409,
  "error": "EMAIL_ALREADY_EXISTS",
  "message": "Email already exists",
  "timestamp": "2025-12-17T13:20:00Z",
  "path": "/api/v1/auth/register"
}
```

## Процесс верификации email

### 1. Письмо с токеном верификации

После регистрации пользователь получает письмо на указанный email адрес.

**Тема письма:** "Подтвердите ваш email"

**Содержимое письма:**

- Приветствие и благодарность за регистрацию
- Кнопка/ссылка для подтверждения email
- URL верификации: `{baseUrl}/api/v1/auth/verify-email?token={token}`
- Информация о том, что ссылка действительна 24 часа
- Инструкция проигнорировать письмо, если регистрация не выполнялась

**Формат ссылки:**

```
{app.email.verification.base-url}/api/v1/auth/verify-email?token={uuid-token}
```

Пример: `http://localhost:8080/api/v1/auth/verify-email?token=550e8400-e29b-41d4-a716-446655440000`

### 2. Запрос на верификацию

**Endpoint:** `GET /api/v1/auth/verify-email?token={token}`

**Параметры:**

- `token` (String, обязательный) - UUID токен из письма

### 3. Обработка верификации на бэкенде

**Файл:** `main-app/src/main/java/online/ityura/springdigitallibrary/service/EmailVerificationService.java`

Процесс верификации:

1. **Поиск токена:**

   - Токен ищется в базе данных
   - Если токен не найден → `404 NOT FOUND` с сообщением "Verification token not found"

2. **Проверка использования:**

   - Если токен уже использован (`used = true`) → `410 GONE` с сообщением "Verification token has already been used"

3. **Проверка срока действия:**

   - Если токен истек (`expiresAt < now`) → `410 GONE` с сообщением "Verification token has expired"
   - По умолчанию токен действителен 24 часа (настраивается через `app.email.verification.token-expiration-hours`)

4. **Верификация пользователя:**

   - Находится пользователь, связанный с токеном
   - Устанавливается `user.isVerified = true`
   - Пользователь сохраняется в базу данных

5. **Пометка токена как использованного:**
   - Устанавливается `verificationToken.used = true`
   - Токен сохраняется в базу данных

### 4. Ответ при успешной верификации

**HTTP Status:** `200 OK`

**Response Body:**

```json
{
  "message": "Email successfully verified"
}
```

### 5. Возможные ошибки при верификации

**404 NOT FOUND** - Токен не найден:

```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Verification token not found",
  "timestamp": "2025-12-17T13:20:00Z",
  "path": "/api/v1/auth/verify-email"
}
```

**410 GONE** - Токен истек или уже использован:

```json
{
  "status": 410,
  "error": "GONE",
  "message": "Verification token has expired",
  "timestamp": "2025-12-17T13:20:00Z",
  "path": "/api/v1/auth/verify-email"
}
```

или

```json
{
  "status": 410,
  "error": "GONE",
  "message": "Verification token has already been used",
  "timestamp": "2025-12-17T13:20:00Z",
  "path": "/api/v1/auth/verify-email"
}
```

## Повторная отправка письма верификации

### 1. Запрос на повторную отправку

**Endpoint:** `POST /api/v1/auth/resend-verification`

**Request Body:**

```json
{
  "email": "john@example.com"
}
```

**Валидация:**

- `email`: обязательное, валидный email формат

### 2. Обработка повторной отправки

**Файл:** `main-app/src/main/java/online/ityura/springdigitallibrary/service/EmailVerificationService.java`

Процесс:

1. **Поиск пользователя:**

   - Пользователь ищется по email
   - Если пользователь не найден → `404 NOT FOUND` с сообщением "User not found"

2. **Проверка статуса верификации:**

   - Если email уже верифицирован (`isVerified = true`) → `400 BAD REQUEST` с сообщением "Email is already verified"

3. **Генерация нового токена:**

   - Старые токены пользователя удаляются
   - Генерируется новый UUID токен
   - Токен сохраняется с новым временем истечения (24 часа)

4. **Отправка письма:**
   - Отправляется новое письмо с новым токеном верификации

### 3. Ответ при успешной отправке

**HTTP Status:** `200 OK`

**Response Body:**

```json
{
  "message": "Verification email sent successfully"
}
```

### 4. Возможные ошибки

**400 BAD REQUEST** - Email уже верифицирован:

```json
{
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Email is already verified",
  "timestamp": "2025-12-17T13:20:00Z",
  "path": "/api/v1/auth/resend-verification"
}
```

**404 NOT FOUND** - Пользователь не найден:

```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "User not found",
  "timestamp": "2025-12-17T13:20:00Z",
  "path": "/api/v1/auth/resend-verification"
}
```

## Вход в систему (после верификации)

### 1. Запрос на вход

**Endpoint:** `POST /api/v1/auth/login`

**Request Body:**

```json
{
  "email": "john@example.com",
  "password": "Password123!"
}
```

### 2. Обработка входа

**Файл:** `main-app/src/main/java/online/ityura/springdigitallibrary/service/AuthService.java`

Процесс:

1. **Аутентификация:**

   - Проверяются email и пароль через `AuthenticationManager`
   - Если неверные учетные данные → `401 UNAUTHORIZED` с сообщением "Bad credentials"

2. **Проверка верификации email:**

   - Находится пользователь по email
   - Проверяется флаг `isVerified`
   - Если `isVerified = false` → `403 FORBIDDEN` с сообщением "Email not verified. Please check your email and click the verification link."

3. **Генерация JWT токенов:**
   - Генерируется `accessToken` (действителен 5 минут)
   - Генерируется `refreshToken` (действителен 24 часа)
   - Оба токена содержат email и роль пользователя

### 3. Ответ при успешном входе

**HTTP Status:** `200 OK`

**Response Body:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer"
}
```

### 4. Возможные ошибки

**401 UNAUTHORIZED** - Неверные учетные данные:

```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Bad credentials",
  "timestamp": "2025-12-17T13:20:00Z",
  "path": "/api/v1/auth/login"
}
```

**403 FORBIDDEN** - Email не верифицирован:

```json
{
  "status": 403,
  "error": "FORBIDDEN",
  "message": "Email not verified. Please check your email and click the verification link.",
  "timestamp": "2025-12-17T13:20:00Z",
  "path": "/api/v1/auth/login"
}
```

## Безопасность

### Проверка верификации в защищенных эндпоинтах

**Файл:** `main-app/src/main/java/online/ityura/springdigitallibrary/security/JwtAuthenticationFilter.java`

Даже если пользователь каким-то образом получил валидный JWT токен, но его email не верифицирован, он не сможет получить доступ к защищенным эндпоинтам:

1. **JwtAuthenticationFilter** проверяет JWT токен в заголовке `Authorization: Bearer {token}`
2. Если токен валиден, извлекается email пользователя
3. Проверяется флаг `isVerified` в базе данных
4. Если `isVerified = false` → возвращается `403 FORBIDDEN` с сообщением "Email not verified. Please check your email and click the verification link."

**Исключения (не требуют JWT и верификации):**

- `/api/v1/auth/register` - регистрация
- `/api/v1/auth/login` - вход (проверка верификации в сервисе)
- `/api/v1/auth/verify-email` - верификация email
- `/api/v1/auth/resend-verification` - повторная отправка письма
- `/api/v1/auth/refresh` - обновление токена
- Публичные эндпоинты книг (GET запросы)
- Swagger документация

## Полный поток регистрации и входа

```
1. Пользователь заполняет форму регистрации
   ↓
2. POST /api/v1/auth/register
   Request: { nickname, email, password }
   ↓
3. Бэкенд создает пользователя (isVerified = false)
   ↓
4. Генерируется токен верификации (UUID, действителен 24 часа)
   ↓
5. Отправляется письмо с ссылкой верификации
   ↓
6. Ответ 201 CREATED: { userId, email, role, message }
   (БЕЗ токенов!)
   ↓
7. Пользователь видит сообщение: "Проверьте email и подтвердите регистрацию"
   ↓
8. Пользователь открывает письмо и переходит по ссылке
   ↓
9. GET /api/v1/auth/verify-email?token={token}
   ↓
10. Бэкенд проверяет токен (существует, не использован, не истек)
    ↓
11. Устанавливается isVerified = true
    ↓
12. Ответ 200 OK: { message: "Email successfully verified" }
    ↓
13. Пользователь может войти в систему
    ↓
14. POST /api/v1/auth/login
    Request: { email, password }
    ↓
15. Бэкенд проверяет учетные данные и isVerified = true
    ↓
16. Ответ 200 OK: { accessToken, refreshToken, tokenType: "Bearer" }
    ↓
17. Пользователь авторизован и может использовать защищенные эндпоинты
```

## Инструкции для фронтенда

### 1. Обработка регистрации

**КРИТИЧНО:** После успешной регистрации НЕ нужно:

- ❌ Автоматически вызывать `/api/v1/auth/login`
- ❌ Сохранять токены в localStorage/sessionStorage (их нет в ответе)
- ❌ Устанавливать состояние "авторизован"

**ПРАВИЛЬНО:**

```javascript
async function register(userData) {
  const response = await fetch("/api/v1/auth/register", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(userData),
  });

  if (response.status === 201) {
    const data = await response.json();

    // Показать сообщение пользователю
    showMessage(data.message);
    // или
    showMessage(
      "Registration successful! Please check your email and click the verification link to activate your account."
    );

    // Перенаправить на страницу с инструкцией
    navigateTo("/registration-success");
  } else if (response.status === 409) {
    showError("Email already exists");
  } else if (response.status === 400) {
    const error = await response.json();
    showValidationErrors(error.fieldErrors);
  }
}
```

### 2. Страница успешной регистрации

Рекомендуется создать страницу `/registration-success`, которая показывает:

- ✅ Сообщение об успешной регистрации
- 📧 Инструкцию проверить email
- 🔗 Информацию о том, что нужно перейти по ссылке из письма
- ⏳ Информацию о сроке действия ссылки (24 часа)
- 🔄 Кнопку для запроса повторной отправки письма

### 3. Обработка верификации email

Если фронтенд обрабатывает редирект с токеном верификации:

```javascript
// URL будет: /api/v1/auth/verify-email?token={token}
// или фронтенд может обработать редирект с токеном

async function verifyEmail(token) {
  const response = await fetch(`/api/v1/auth/verify-email?token=${token}`);

  if (response.ok) {
    const data = await response.json();
    showMessage(data.message); // "Email successfully verified"
    navigateTo("/login");
  } else if (response.status === 404) {
    showError(
      "Verification token not found. Please request a new verification email."
    );
  } else if (response.status === 410) {
    const error = await response.json();
    if (error.message.includes("expired")) {
      showError(
        "Verification link has expired. Please request a new verification email."
      );
    } else {
      showError(
        "Verification link has already been used. Please request a new verification email."
      );
    }
  }
}
```

### 4. Повторная отправка письма верификации

```javascript
async function resendVerificationEmail(email) {
  const response = await fetch("/api/v1/auth/resend-verification", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ email }),
  });

  if (response.ok) {
    showMessage(
      "Verification email sent successfully. Please check your inbox."
    );
  } else if (response.status === 404) {
    showError("User not found");
  } else if (response.status === 400) {
    const error = await response.json();
    if (error.message.includes("already verified")) {
      showError("Email is already verified. You can log in now.");
      navigateTo("/login");
    } else {
      showValidationErrors(error.fieldErrors);
    }
  }
}
```

### 5. Обработка входа после верификации

```javascript
async function login(email, password) {
  const response = await fetch("/api/v1/auth/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ email, password }),
  });

  if (response.ok) {
    const data = await response.json();

    // Сохранить токены
    localStorage.setItem("accessToken", data.accessToken);
    localStorage.setItem("refreshToken", data.refreshToken);

    // Установить состояние "авторизован"
    setAuthState({ isAuthenticated: true, user: { email } });

    // Перенаправить на главную страницу
    navigateTo("/");
  } else if (response.status === 401) {
    showError("Invalid email or password");
  } else if (response.status === 403) {
    showError(
      "Email not verified. Please check your email and click the verification link."
    );
    // Можно показать кнопку для повторной отправки письма
  }
}
```

## Конфигурация

### Настройки в application.properties

```properties
# Базовый URL для ссылок верификации
app.email.verification.base-url=http://localhost:8080

# Время жизни токена верификации (в часах)
app.email.verification.token-expiration-hours=24

# Настройки SMTP для отправки писем
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=your-email@example.com
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.from=noreply@example.com
```

## Тестирование

### Локальное тестирование с MailHog

Для локального тестирования можно использовать MailHog для перехвата отправленных писем:

1. Запустить MailHog через Docker:

```bash
docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog
```

2. Настроить `application.properties`:

```properties
spring.mail.host=localhost
spring.mail.port=1025
```

3. Открыть веб-интерфейс MailHog: `http://localhost:8025`

4. Все отправленные письма будут видны в MailHog, включая токены верификации

## Резюме

### Ключевые моменты

1. ✅ При регистрации JWT токены **НЕ выдаются**
2. ✅ Пользователь создается с `isVerified = false`
3. ✅ Отправляется письмо с токеном верификации (действителен 24 часа)
4. ✅ Вход в систему возможен **только после верификации email**
5. ✅ Даже с валидным JWT токеном неверифицированный пользователь не может получить доступ к защищенным эндпоинтам
6. ✅ Можно запросить повторную отправку письма верификации

### API Endpoints

| Метод | Endpoint                                  | Описание                        | Требует JWT | Требует верификации        |
| ----- | ----------------------------------------- | ------------------------------- | ----------- | -------------------------- |
| POST  | `/api/v1/auth/register`                   | Регистрация нового пользователя | ❌          | ❌                         |
| GET   | `/api/v1/auth/verify-email?token={token}` | Верификация email               | ❌          | ❌                         |
| POST  | `/api/v1/auth/resend-verification`        | Повторная отправка письма       | ❌          | ❌                         |
| POST  | `/api/v1/auth/login`                      | Вход в систему                  | ❌          | ✅ (после входа)           |
| POST  | `/api/v1/auth/refresh`                    | Обновление токена               | ❌          | ✅ (проверяется в фильтре) |
