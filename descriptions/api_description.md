# API Description - Spring Digital Bookstore

## Общая информация

**Название проекта:** Spring Digital Bookstore  
**Версия:** 1.0  
**Base URL:** `http://localhost:8080` (локально) или `http://localhost:8088` (Docker)  
**API Prefix:** `/api/v1`  
**Технологии:** Spring Boot 4.0.0, Java 21, PostgreSQL, JWT, Stripe, Telegram Bot API, OpenAI API, Google Gemini API

## Архитектура

### Роли пользователей

- **USER** - обычный пользователь (может просматривать книги, оставлять отзывы и рейтинги, скачивать PDF, покупать книги)
- **ADMIN** - администратор (все права USER + управление книгами и пользователями)

### Аутентификация

Система использует JWT (JSON Web Tokens) для аутентификации:

- **Access Token** - действителен 5 минут (300000 мс)
- **Refresh Token** - действителен 24 часа (86400000 мс)
- **Тип токена:** Bearer Token
- **Заголовок:** `Authorization: Bearer <accessToken>`

### База данных

- **СУБД:** PostgreSQL
- **Диалект:** `org.hibernate.dialect.PostgreSQLDialect`
- **DDL режим:** настраивается через `spring.jpa.hibernate.ddl-auto` (create/update/validate/none)

---

## Сущности (Entity)

### 1. User (Пользователь)

**Таблица:** `users`

| Поле         | Тип           | Описание                 | Ограничения                                                                |
| ------------ | ------------- | ------------------------ | -------------------------------------------------------------------------- |
| id           | Long          | Уникальный идентификатор | Primary Key, Auto Increment                                                |
| nickname     | String        | Никнейм пользователя     | NOT NULL, 3-50 символов, только буквы, цифры, дефисы, подчеркивания, точки |
| email        | String        | Email пользователя       | NOT NULL, UNIQUE, валидный email                                           |
| passwordHash | String        | Хеш пароля               | NOT NULL, BCrypt                                                           |
| role         | Role (enum)   | Роль пользователя        | NOT NULL, USER или ADMIN                                                   |
| isVerified   | Boolean       | Статус верификации email  | NOT NULL, default: false                                                   |
| createdAt    | LocalDateTime | Дата создания            | NOT NULL, автоматически устанавливается                                    |

**Enum Role:**

- `USER`
- `ADMIN`

### 2. Book (Книга)

**Таблица:** `books`

| Поле            | Тип           | Описание                 | Ограничения                                                            |
| --------------- | ------------- | ------------------------ | ---------------------------------------------------------------------- |
| id              | Long          | Уникальный идентификатор | Primary Key, Auto Increment                                            |
| title           | String        | Название книги           | NOT NULL                                                               |
| author          | Author        | Автор книги              | NOT NULL, ManyToOne                                                    |
| description     | String        | Описание книги           | TEXT                                                                   |
| publishedYear   | Integer       | Год публикации           | 1000-9999                                                              |
| genre           | Genre (enum)  | Жанр книги               | См. список жанров ниже                                                 |
| deletionLocked  | Boolean       | Флаг блокировки удаления | NOT NULL, default: false                                               |
| ratingAvg       | BigDecimal    | Средний рейтинг          | NOT NULL, precision: 4, scale: 2, default: 0.00, диапазон: 0.00-10.00  |
| ratingCount     | Integer       | Количество оценок        | NOT NULL, default: 0                                                   |
| imagePath       | String        | Путь к изображению       | Может быть null                                                        |
| pdfPath         | String        | Путь к PDF файлу         | Может быть null                                                        |
| price           | BigDecimal    | Цена книги в USD         | NOT NULL, precision: 10, scale: 2, default: 0.00                       |
| discountPercent | BigDecimal    | Процент скидки           | NOT NULL, precision: 5, scale: 2, default: 0.00, диапазон: 0.00-100.00 |
| createdAt       | LocalDateTime | Дата создания            | NOT NULL, автоматически устанавливается                                |
| updatedAt       | LocalDateTime | Дата обновления          | Автоматически обновляется                                              |

**Уникальное ограничение:** `(title, author_id)` - комбинация названия и автора должна быть уникальной

**Enum Genre:**

- `FICTION` - Художественная литература
- `NON_FICTION` - Нехудожественная литература
- `MYSTERY` - Детектив
- `THRILLER` - Триллер
- `ROMANCE` - Романтика
- `SCIENCE_FICTION` - Научная фантастика
- `FANTASY` - Фэнтези
- `HORROR` - Ужасы
- `HISTORICAL` - Историческая литература
- `BIOGRAPHY` - Биография
- `AUTOBIOGRAPHY` - Автобиография
- `MEMOIR` - Мемуары
- `PHILOSOPHY` - Философия
- `PSYCHOLOGY` - Психология
- `SELF_HELP` - Саморазвитие
- `BUSINESS` - Бизнес
- `TECHNOLOGY` - Технологии
- `SCIENCE` - Наука
- `EDUCATION` - Образование
- `COOKING` - Кулинария
- `TRAVEL` - Путешествия
- `POETRY` - Поэзия
- `DRAMA` - Драма
- `COMEDY` - Комедия
- `ADVENTURE` - Приключения
- `WESTERN` - Вестерн
- `YOUNG_ADULT` - Молодежная литература
- `CHILDREN` - Детская литература
- `PORNO` - Порно
- `FOR_NERDS` - Для задротов
- `FOR_PEOPLE_WITHOUT_PERSONAL_LIFE` - Для людей без личной жизни

### 3. Author (Автор)

**Таблица:** `authors`

| Поле      | Тип           | Описание                 | Ограничения                                     |
| --------- | ------------- | ------------------------ | ----------------------------------------------- |
| id        | Long          | Уникальный идентификатор | Primary Key, Auto Increment                     |
| fullName  | String        | Полное имя автора        | NOT NULL, UNIQUE                                |
| createdAt | LocalDateTime | Дата создания            | Автоматически устанавливается через @PrePersist |

### 4. Review (Отзыв)

**Таблица:** `reviews`

| Поле      | Тип           | Описание                 | Ограничения                             |
| --------- | ------------- | ------------------------ | --------------------------------------- |
| id        | Long          | Уникальный идентификатор | Primary Key, Auto Increment             |
| book      | Book          | Книга                    | NOT NULL, ManyToOne                     |
| user      | User          | Пользователь             | NOT NULL, ManyToOne                     |
| text      | String        | Текст отзыва             | NOT NULL, TEXT                          |
| createdAt | LocalDateTime | Дата создания            | NOT NULL, автоматически устанавливается |
| updatedAt | LocalDateTime | Дата обновления          | Автоматически обновляется               |

**Уникальное ограничение:** `(user_id, book_id)` - один пользователь может оставить только один отзыв на книгу

### 5. Rating (Рейтинг)

**Таблица:** `ratings`

| Поле      | Тип           | Описание                 | Ограничения                             |
| --------- | ------------- | ------------------------ | --------------------------------------- |
| id        | Long          | Уникальный идентификатор | Primary Key, Auto Increment             |
| book      | Book          | Книга                    | NOT NULL, ManyToOne                     |
| user      | User          | Пользователь             | NOT NULL, ManyToOne                     |
| value     | Short         | Значение рейтинга        | NOT NULL, диапазон: 1-10                |
| createdAt | LocalDateTime | Дата создания            | NOT NULL, автоматически устанавливается |
| updatedAt | LocalDateTime | Дата обновления          | Автоматически обновляется               |

**Уникальное ограничение:** `(user_id, book_id)` - один пользователь может поставить только один рейтинг на книгу

### 6. Purchase (Покупка)

**Таблица:** `purchases`

| Поле                  | Тип                   | Описание                 | Ограничения                                         |
| --------------------- | --------------------- | ------------------------ | --------------------------------------------------- |
| id                    | Long                  | Уникальный идентификатор | Primary Key, Auto Increment                         |
| user                  | User                  | Пользователь             | NOT NULL, ManyToOne, CASCADE DELETE                 |
| book                  | Book                  | Книга                    | NOT NULL, ManyToOne                                 |
| stripePaymentIntentId | String                | ID платежа от Stripe     | NOT NULL, UNIQUE (session ID или payment intent ID) |
| amountPaid            | BigDecimal            | Сумма оплаты             | NOT NULL, precision: 10, scale: 2                   |
| status                | PurchaseStatus (enum) | Статус покупки           | NOT NULL, default: PENDING                          |
| createdAt             | LocalDateTime         | Дата создания            | NOT NULL, автоматически устанавливается             |
| updatedAt             | LocalDateTime         | Дата обновления          | Автоматически обновляется                           |

**Уникальное ограничение:** `(user_id, book_id)` - один пользователь может иметь только одну покупку на книгу

**Enum PurchaseStatus:**

- `PENDING` - Ожидает оплаты
- `COMPLETED` - Оплата завершена
- `FAILED` - Оплата не удалась
- `REFUNDED` - Возврат средств

---

## API Endpoints

### Базовый URL

Все эндпоинты начинаются с `/api/v1`

### Аутентификация

#### POST /api/v1/auth/register

**Описание:** Регистрация нового пользователя. После регистрации на указанный email будет отправлено письмо с ссылкой для подтверждения. Пользователь должен подтвердить email перед входом в систему. JWT токены НЕ выдаются при регистрации.  
**Авторизация:** Не требуется  
**Content-Type:** `application/json`

**Request Body:**

```json
{
  "nickname": "john_doe",
  "email": "john@example.com",
  "password": "Password123!"
}
```

**Валидация:**

- `nickname`: 3-50 символов, только буквы, цифры, дефисы, подчеркивания, точки
- `email`: валидный email, уникальный
- `password`: минимум 8 символов, должна содержать: цифру, строчную букву, заглавную букву, спецсимвол, без пробелов

**Response (201 Created):**

```json
{
  "userId": 1,
  "email": "john@example.com",
  "role": "USER",
  "message": "Registration successful! Please check your email and click the verification link to activate your account."
}
```

**Ошибки:**

- `400` - Валидация не прошла (fieldErrors содержит детали)
- `409` - Email уже существует (`EMAIL_ALREADY_EXISTS`)

**Примечания:**

- После регистрации пользователь получает `isVerified = false`
- На email отправляется письмо с токеном верификации
- Токен верификации действителен 24 часа (настраивается через `app.email.verification.token-expiration-hours`)

---

#### POST /api/v1/auth/login

**Описание:** Вход в систему  
**Авторизация:** Не требуется  
**Content-Type:** `application/json`

**Request Body:**

```json
{
  "email": "admin@gmail.com",
  "password": "admin"
}
```

**Response (200 OK):**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer"
}
```

**Ошибки:**

- `400` - Валидация не прошла
- `401` - Неверные учетные данные (`UNAUTHORIZED`)

**Примечания:**

- Access token действителен 5 минут
- Refresh token действителен 24 часа
- Токен передается в заголовке: `Authorization: Bearer <accessToken>`

---

#### POST /api/v1/auth/refresh

**Описание:** Обновление access токена  
**Авторизация:** Не требуется  
**Content-Type:** `application/json`

**Request Body:**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response (200 OK):** Аналогично login (новая пара токенов)

**Ошибки:**

- `400` - Валидация не прошла
- `401` - Refresh токен недействителен или истек (`UNAUTHORIZED`)

---

#### GET /api/v1/auth/verify-email

**Описание:** Верификация email пользователя по токену, полученному в письме  
**Авторизация:** Не требуется

**Query Parameters:**

| Параметр | Тип    | Обязательный | Описание                    |
| -------- | ------ | ------------ | --------------------------- |
| token    | string | Да           | UUID токен из письма         |

**Response (200 OK):**

```json
{
  "message": "Email successfully verified"
}
```

**Ошибки:**

- `404` - Токен не найден (`NOT_FOUND`)
- `410` - Токен истек или уже использован (`GONE`)

**Примечания:**

- Токен верификации действителен 24 часа (настраивается через `app.email.verification.token-expiration-hours`)
- После успешной верификации пользователь может войти в систему
- Токен можно использовать только один раз

---

#### POST /api/v1/auth/resend-verification

**Описание:** Повторная отправка письма с токеном верификации на указанный email. Доступно только для неверифицированных пользователей.  
**Авторизация:** Не требуется  
**Content-Type:** `application/json`

**Request Body:**

```json
{
  "email": "john@example.com"
}
```

**Валидация:**

- `email`: валидный email, обязательное поле

**Response (200 OK):**

```json
{
  "message": "Verification email sent successfully"
}
```

**Ошибки:**

- `400` - Валидация не прошла или email уже верифицирован (`VALIDATION_ERROR`, `BAD_REQUEST`)
- `404` - Пользователь не найден (`NOT_FOUND`)

---

#### POST /api/v1/auth/forgot-password

**Описание:** Запрос на восстановление пароля. Отправляет письмо с ссылкой для восстановления пароля на указанный email. Всегда возвращает 200 OK, даже если email не найден (security best practice).  
**Авторизация:** Не требуется  
**Content-Type:** `application/json`

**Request Body:**

```json
{
  "email": "john@example.com"
}
```

**Валидация:**

- `email`: валидный email, обязательное поле

**Response (200 OK):**

```json
{
  "message": "If the email exists, a password reset link has been sent"
}
```

**Ошибки:**

- `400` - Валидация не прошла (`VALIDATION_ERROR`)

**Примечания:**

- Токен восстановления пароля действителен 1 час (настраивается через `app.password.reset.token-expiration-hours`)
- Для безопасности система не раскрывает информацию о существовании email

---

#### GET /api/v1/auth/reset-password

**Описание:** Проверка валидности токена восстановления пароля. Используется при переходе по ссылке из письма. Не сбрасывает пароль, только проверяет токен.  
**Авторизация:** Не требуется

**Query Parameters:**

| Параметр | Тип    | Обязательный | Описание                    |
| -------- | ------ | ------------ | --------------------------- |
| token    | string | Да           | UUID токен из письма         |

**Response (200 OK):**

```json
{
  "message": "Token is valid"
}
```

**Ошибки:**

- `404` - Токен не найден (`NOT_FOUND`)
- `410` - Токен истек или уже использован (`GONE`)

---

#### POST /api/v1/auth/reset-password

**Описание:** Сброс пароля пользователя по токену восстановления, полученному в письме.  
**Авторизация:** Не требуется  
**Content-Type:** `application/json`

**Request Body:**

```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "NewPassword123!"
}
```

**Валидация:**

- `token`: обязательное поле
- `newPassword`: минимум 8 символов, должна содержать: цифру, строчную букву, заглавную букву, спецсимвол, без пробелов

**Response (200 OK):**

```json
{
  "message": "Password has been reset successfully"
}
```

**Ошибки:**

- `400` - Валидация не прошла (`VALIDATION_ERROR`)
- `404` - Токен не найден (`NOT_FOUND`)
- `410` - Токен истек или уже использован (`GONE`)

**Примечания:**

- После успешного сброса пароля токен помечается как использованный
- Пользователь может сразу войти в систему с новым паролем

---

### Пользователь

#### GET /api/v1/users/me

**Описание:** Получить информацию о текущем пользователе (по access токену)  
**Авторизация:** Требуется (Bearer Token)

**Response (200 OK):**

```json
{
  "id": 1,
  "nickname": "john_doe",
  "email": "john@example.com",
  "role": "USER"
}
```

**Ошибки:**

- `401` - Не авторизован (`UNAUTHORIZED`)
- `404` - Пользователь не найден (`USER_NOT_FOUND`)

---

### Книги (публичные эндпоинты)

#### GET /api/v1/books

**Описание:** Получить список книг с пагинацией, сортировкой и фильтрацией  
**Авторизация:** Не требуется

**Query Parameters:**

| Параметр | Тип    | Обязательный | По умолчанию | Описание                      |
| -------- | ------ | ------------ | ------------ | ----------------------------- |
| page     | int    | Нет          | 0            | Номер страницы (начиная с 0)  |
| size     | int    | Нет          | 10           | Размер страницы               |
| sort     | string | Нет          | "title,asc"  | Сортировка (поле,направление) |
| genre    | Genre  | Нет          | -            | Фильтрация по жанру           |

**Доступные поля для сортировки:**

- `title` - название
- `author.fullName` - автор
- `ratingAvg` - рейтинг
- `genre` - жанр
- `createdAt` - дата добавления
- `publishedYear` - год публикации
- `updatedAt` - дата обновления

**Направления сортировки:**

- `asc` - по возрастанию
- `desc` - по убыванию

**Примеры запросов:**

- `GET /api/v1/books?page=0&size=20`
- `GET /api/v1/books?genre=FICTION&page=0&size=10`
- `GET /api/v1/books?sort=ratingAvg,desc&page=0`
- `GET /api/v1/books?genre=TECHNOLOGY&sort=ratingAvg,desc`

**Response (200 OK):**

```json
{
  "content": [
    {
      "id": 1,
      "title": "Spring Boot Guide",
      "author": {
        "id": 1,
        "fullName": "John Smith"
      },
      "description": "Comprehensive guide to Spring Boot framework",
      "publishedYear": 2023,
      "genre": "TECHNOLOGY",
      "ratingAvg": 8.5,
      "ratingCount": 10,
      "hasFile": true,
      "imagePath": "/path/to/image.png",
      "price": 9.99,
      "discountPercent": 10.0,
      "finalPrice": 8.99,
      "createdAt": "2025-12-17T13:20:00",
      "updatedAt": "2025-12-17T13:20:00",
      "reviews": []
    }
  ],
  "pageable": {
    "sort": {
      "sorted": true,
      "unsorted": false
    },
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 100,
  "totalPages": 10,
  "size": 10,
  "number": 0,
  "first": true,
  "last": false,
  "numberOfElements": 10,
  "empty": false
}
```

---

#### GET /api/v1/books/{bookId}

**Описание:** Получить детальную информацию о книге  
**Авторизация:** Не требуется

**Path Parameters:**

- `bookId` (Long, required) - ID книги

**Response (200 OK):** BookResponse (аналогично элементу из списка)

**Ошибки:**

- `404` - Книга не найдена (`BOOK_NOT_FOUND`)

---

#### GET /api/v1/books/{bookId}/image

**Описание:** Получить изображение книги  
**Авторизация:** Не требуется

**Path Parameters:**

- `bookId` (Long, required) - ID книги

**Response (200 OK):** Изображение (Content-Type: `image/png` или `image/jpeg`)

**Ошибки:**

- `404` - Книга или изображение не найдены (`BOOK_NOT_FOUND`)

---

#### GET /api/v1/books/images/all

**Описание:** Получить все изображения книг в ZIP архиве  
**Авторизация:** Не требуется

**Response (200 OK):** ZIP файл (Content-Type: `application/zip`)

**Ошибки:**

- `404` - Не найдено книг с изображениями (`NOT_FOUND`)

---

#### GET /api/v1/books/{bookId}/reviews

**Описание:** Получить отзывы на книгу с пагинацией  
**Авторизация:** Не требуется

**Path Parameters:**

- `bookId` (Long, required) - ID книги

**Query Parameters:**

| Параметр | Тип    | Обязательный | По умолчанию     | Описание        |
| -------- | ------ | ------------ | ---------------- | --------------- |
| page     | int    | Нет          | 0                | Номер страницы  |
| size     | int    | Нет          | 20               | Размер страницы |
| sort     | string | Нет          | "createdAt,desc" | Сортировка      |

**Response (200 OK):**

```json
{
  "content": [
    {
      "id": 1,
      "bookId": 1,
      "user": {
        "id": 1,
        "nickname": "john_doe",
        "email": "john@example.com"
      },
      "text": "Отличная книга! Очень рекомендую.",
      "createdAt": "2025-12-17T13:20:00",
      "updatedAt": "2025-12-17T13:20:00"
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "size": 20,
  "number": 0
}
```

**Ошибки:**

- `404` - Книга не найдена (`BOOK_NOT_FOUND`)

---

### Книги (требуется авторизация)

#### GET /api/v1/books/{bookId}/download

**Описание:** Скачать PDF книги. Для платных книг требуется предварительная оплата. Бесплатные книги (price = 0) доступны сразу.  
**Авторизация:** Требуется (Bearer Token)

**Path Parameters:**

- `bookId` (Long, required) - ID книги

**Response (200 OK):** PDF файл (Content-Type: `application/pdf`)

**Ошибки:**

- `401` - Не авторизован (`UNAUTHORIZED`)
- `402` - Требуется оплата (книга платная и не была оплачена) (`PAYMENT_REQUIRED`)
- `404` - Книга или PDF файл не найдены (`BOOK_NOT_FOUND`)

---

### Рейтинги (требуется авторизация)

#### POST /api/v1/books/{bookId}/ratings

**Описание:** Создать рейтинг (1-10)  
**Авторизация:** Требуется (Bearer Token)  
**Content-Type:** `application/json`

**Path Parameters:**

- `bookId` (Long, required) - ID книги

**Request Body:**

```json
{
  "value": 8
}
```

**Валидация:**

- `value`: обязательное, диапазон 1-10

**Response (201 Created):**

```json
{
  "id": 1,
  "bookId": 1,
  "userId": 1,
  "value": 8,
  "createdAt": "2025-12-17T13:20:00",
  "updatedAt": "2025-12-17T13:20:00"
}
```

**Ошибки:**

- `400` - Валидация не прошла (`VALIDATION_ERROR`)
- `404` - Книга не найдена (`BOOK_NOT_FOUND`)
- `409` - Рейтинг уже существует для этой книги (`RATING_ALREADY_EXISTS`)

---

#### PUT /api/v1/books/{bookId}/ratings/my

**Описание:** Обновить свой рейтинг  
**Авторизация:** Требуется (Bearer Token)  
**Content-Type:** `application/json`

**Path Parameters:**

- `bookId` (Long, required) - ID книги

**Request Body:**

```json
{
  "value": 9
}
```

**Валидация:**

- `value`: обязательное, диапазон 1-10

**Response (200 OK):** RatingResponse

**Ошибки:**

- `400` - Валидация не прошла (`VALIDATION_ERROR`)
- `404` - Рейтинг не найден (`RATING_NOT_FOUND`)

---

### Отзывы (требуется авторизация)

#### POST /api/v1/books/{bookId}/reviews

**Описание:** Создать отзыв  
**Авторизация:** Требуется (Bearer Token)  
**Content-Type:** `application/json`

**Path Parameters:**

- `bookId` (Long, required) - ID книги

**Request Body:**

```json
{
  "text": "Отличная книга! Очень рекомендую к прочтению."
}
```

**Валидация:**

- `text`: обязательное, не пустое

**Response (201 Created):**

```json
{
  "id": 1,
  "bookId": 1,
  "user": {
    "id": 1,
    "nickname": "john_doe",
    "email": "john@example.com"
  },
  "text": "Отличная книга! Очень рекомендую к прочтению.",
  "createdAt": "2025-12-17T13:20:00",
  "updatedAt": "2025-12-17T13:20:00"
}
```

**Ошибки:**

- `400` - Валидация не прошла (`VALIDATION_ERROR`)
- `404` - Книга не найдена (`BOOK_NOT_FOUND`)
- `409` - Отзыв уже существует для этой книги (`REVIEW_ALREADY_EXISTS`)

---

#### PUT /api/v1/books/{bookId}/reviews/my

**Описание:** Обновить свой отзыв  
**Авторизация:** Требуется (Bearer Token)  
**Content-Type:** `application/json`

**Path Parameters:**

- `bookId` (Long, required) - ID книги

**Request Body:**

```json
{
  "text": "Обновленный отзыв: книга превзошла все ожидания!"
}
```

**Валидация:**

- `text`: обязательное, не пустое

**Response (200 OK):** ReviewResponse

**Ошибки:**

- `400` - Валидация не прошла (`VALIDATION_ERROR`)
- `404` - Отзыв не найден (`REVIEW_NOT_FOUND`)

---

#### GET /api/v1/reviews/my

**Описание:** Получить свои отзывы с пагинацией  
**Авторизация:** Требуется (Bearer Token)

**Query Parameters:**

| Параметр | Тип    | Обязательный | По умолчанию     | Описание        |
| -------- | ------ | ------------ | ---------------- | --------------- |
| page     | int    | Нет          | 0                | Номер страницы  |
| size     | int    | Нет          | 20               | Размер страницы |
| sort     | string | Нет          | "createdAt,desc" | Сортировка      |

**Response (200 OK):** Page<ReviewResponse>

---

### Сообщения читателям (публичные)

#### POST /api/v1/books/{bookId}/message/censored

**Описание:** Отправить вопрос о книге рандомному читателю (цензурно)  
**Авторизация:** Требуется (Bearer Token)  
**Content-Type:** `application/json`

**Path Parameters:**

- `bookId` (Long, required) - ID книги

**Request Body:**

```json
{
  "message": "Что вам больше всего понравилось в этой книге?"
}
```

**Валидация:**

- `message`: обязательное, не пустое, максимум 256 символов

**Response (200 OK):**

```json
{
  "message": "Ответ от читателя..."
}
```

**Примечание:** Ответ приходит не сразу, нужно подождать 10-20 секунд. Используется OpenAI API или Google Gemini API для генерации ответа.

**Ошибки:**

- `400` - Валидация не прошла (`VALIDATION_ERROR`)
- `401` - Не авторизован (`UNAUTHORIZED`)
- `404` - Книга не найдена (`BOOK_NOT_FOUND`)
- `500` - Ошибка при обращении к OpenAI/Gemini API (`INTERNAL_SERVER_ERROR`)

---

#### POST /api/v1/books/{bookId}/message/uncensored

**Описание:** Отправить вопрос о книге рандомному читателю (нецензурно)  
**Авторизация:** Требуется (Bearer Token)  
**Content-Type:** `application/json`

**Path Parameters:**

- `bookId` (Long, required) - ID книги

**Request Body:** Аналогично censored

**Response (200 OK):** Аналогично censored

**Примечание:** Используется для генерации ответов без цензуры.

---

### Платежи (требуется авторизация)

#### POST /api/v1/payment/checkout/{bookId}

**Описание:** Создать сессию оплаты для книги через Stripe. Возвращает URL для перенаправления на страницу оплаты Stripe. Если книга бесплатная (price = 0), возвращает сообщение об успехе без создания checkout session.  
**Авторизация:** Требуется (Bearer Token)

**Path Parameters:**

- `bookId` (Long, required) - ID книги

**Response (200 OK):**

Для платных книг:

```json
{
  "checkoutUrl": "https://checkout.stripe.com/pay/cs_test_..."
}
```

Для бесплатных книг:

```json
{
  "message": "Book is free, access granted"
}
```

**Ошибки:**

- `400` - Книга уже оплачена (`BOOK_ALREADY_PURCHASED`)
- `401` - Не авторизован (`UNAUTHORIZED`)
- `404` - Книга не найдена (`BOOK_NOT_FOUND`)
- `500` - Ошибка при создании checkout session (`INTERNAL_SERVER_ERROR`)

**Примечания:**

- После успешной оплаты Stripe редиректит на `/api/v1/payment/success?session_id=...&book_id=...`
- При отмене Stripe редиректит на `/api/v1/payment/cancel?book_id=...`
- Webhook endpoint обрабатывает события оплаты автоматически

---

#### POST /api/v1/payment/webhook

**Описание:** Webhook endpoint для обработки событий от Stripe. Вызывается автоматически Stripe, не требует вызова из клиентского приложения.  
**Авторизация:** Не требуется (проверяется подпись Stripe)

**Headers:**

- `Stripe-Signature` (required) - подпись события от Stripe

**Request Body:** JSON payload от Stripe

**Обрабатываемые события:**

- `checkout.session.completed` - успешная оплата
- `payment_intent.succeeded` - успешная оплата (резервный вариант)
- `payment_intent.payment_failed` - неудачная оплата

**Response (200 OK):** `"Success"`

**Ошибки:**

- `400` - Неверная подпись (`BAD_REQUEST`)

**Примечания:**

- Webhook secret настраивается через `stripe.webhook-secret`
- Подпись проверяется автоматически
- При успешной оплате создается/обновляется запись Purchase со статусом COMPLETED

---

#### GET /api/v1/payment/success

**Описание:** Публичная страница успешной оплаты. Stripe редиректит сюда после успешной оплаты.  
**Авторизация:** Не требуется

**Query Parameters:**

| Параметр   | Тип    | Обязательный | Описание         |
| ---------- | ------ | ------------ | ---------------- |
| session_id | string | Нет          | ID сессии Stripe |
| book_id    | number | Нет          | ID книги         |

**Поведение:**

- **Для браузерных запросов** (заголовок `Accept: text/html`): автоматически выполняет редирект (302) на страницу книги `/books/{bookId}?payment=success&session_id={sessionId}` (или `/books/{bookId}?payment=success`, если session_id отсутствует)
- **Для API-запросов** (заголовок `Accept: application/json`): возвращает JSON ответ

**Response для API-запросов (200 OK):**

```json
{
  "status": "success",
  "message": "Payment completed successfully! You can now download the book.",
  "session_id": "cs_test_...",
  "book_id": "1"
}
```

**Редирект для браузерных запросов (302 Found):**

- Если указан `app.frontend-url` в настройках: редирект на `{frontend-url}/books/{bookId}?payment=success&session_id={sessionId}`
- Если `app.frontend-url` не указан: возвращается JSON ответ с полем `redirectUrl`, содержащим относительный путь `/books/{bookId}?payment=success&session_id={sessionId}`

**Настройка frontend URL:**
В `application.properties` можно указать:

```properties
app.frontend-url=http://localhost:3000
```

---

#### GET /api/v1/payment/cancel

**Описание:** Публичная страница отмены оплаты. Stripe редиректит сюда при отмене оплаты.  
**Авторизация:** Не требуется

**Query Parameters:**

| Параметр | Тип    | Обязательный | Описание |
| -------- | ------ | ------------ | -------- |
| book_id  | number | Нет          | ID книги |

**Поведение:**

- **Для браузерных запросов** (заголовок `Accept: text/html`): автоматически выполняет редирект (302) на страницу книги `/books/{bookId}?payment=cancelled`
- **Для API-запросов** (заголовок `Accept: application/json`): возвращает JSON ответ

**Response для API-запросов (200 OK):**

```json
{
  "status": "cancelled",
  "message": "Payment was cancelled. You can try again later.",
  "book_id": "1"
}
```

**Редирект для браузерных запросов (302 Found):**

- Если указан `app.frontend-url` в настройках: редирект на `{frontend-url}/books/{bookId}?payment=cancelled`
- Если `app.frontend-url` не указан: возвращается JSON ответ (редирект не выполняется)

---

### Административные функции (требуется роль ADMIN)

#### Управление книгами

##### POST /api/v1/admin/books

**Описание:** Создать книгу  
**Авторизация:** Требуется (Bearer Token, роль ADMIN)  
**Content-Type:** `application/json`

**Request Body:**

```json
{
  "title": "Spring Boot Guide",
  "authorName": "John Smith",
  "description": "Comprehensive guide to Spring Boot framework",
  "publishedYear": 2023,
  "genre": "TECHNOLOGY",
  "price": 9.99,
  "discountPercent": 0
}
```

**Поля:**

| Поле            | Тип     | Обязательный | Описание                                          |
| --------------- | ------- | ------------ | ------------------------------------------------- |
| title           | string  | Да           | Название книги                                    |
| authorName      | string  | Да           | Имя автора                                        |
| description     | string  | Нет          | Описание книги                                    |
| publishedYear   | integer | Нет          | Год публикации (1000-9999)                        |
| genre           | Genre   | Нет          | Жанр книги                                        |
| price           | number  | Нет          | Цена книги в USD (по умолчанию 0.00 - бесплатная) |
| discountPercent | number  | Нет          | Процент скидки 0-100 (по умолчанию 0.00)          |

**Response (201 Created):** BookResponse

**Ошибки:**

- `400` - Валидация не прошла или несуществующий жанр (`VALIDATION_ERROR`, `INVALID_GENRE`)
- `403` - Недостаточно прав (`ACCESS_DENIED`)
- `409` - Книга с таким названием и автором уже существует (`BOOK_ALREADY_EXISTS`)

---

##### PUT /api/v1/admin/books/{bookId}

**Описание:** Полностью обновить книгу (все поля обязательны)  
**Авторизация:** Требуется (Bearer Token, роль ADMIN)  
**Content-Type:** `application/json`

**Path Parameters:**

- `bookId` (Long, required) - ID книги

**Request Body:**

```json
{
  "title": "Updated Spring Boot Guide",
  "authorName": "Jane Doe",
  "description": "Updated description",
  "publishedYear": 2024,
  "genre": "TECHNOLOGY",
  "price": 9.99,
  "discountPercent": 10.0
}
```

**Поля:**

| Поле            | Тип     | Обязательный | Описание                   |
| --------------- | ------- | ------------ | -------------------------- |
| title           | string  | Да           | Название книги             |
| authorName      | string  | Да           | Имя автора                 |
| description     | string  | Да           | Описание книги             |
| publishedYear   | integer | Да           | Год публикации (1000-9999) |
| genre           | Genre   | Да           | Жанр книги                 |
| price           | number  | Да           | Цена книги в USD           |
| discountPercent | number  | Да           | Процент скидки 0-100       |

**Response (200 OK):** BookResponse

**Ошибки:**

- `400` - Валидация не прошла (`VALIDATION_ERROR`)
- `404` - Книга не найдена (`BOOK_NOT_FOUND`)
- `403` - Недостаточно прав (`ACCESS_DENIED`)
- `409` - Конфликт уникальности (`BOOK_ALREADY_EXISTS`)

---

##### PATCH /api/v1/admin/books/{bookId}

**Описание:** Частично обновить книгу (все поля опциональны). Поддерживает два формата: JSON и multipart/form-data.  
**Авторизация:** Требуется (Bearer Token, роль ADMIN)

**Path Parameters:**

- `bookId` (Long, required) - ID книги

**Вариант 1: JSON формат**

**Content-Type:** `application/json`

**Request Body:**

```json
{
  "title": "Updated Title",
  "authorName": "New Author",
  "description": "Updated description",
  "publishedYear": 2024,
  "genre": "FICTION",
  "price": 9.99,
  "discountPercent": 10.0
}
```

**Поля:** Все поля опциональны

**Вариант 2: Multipart формат (с изображением и/или PDF)**

**Content-Type:** `multipart/form-data`

**Form Data:**

| Поле            | Тип     | Обязательный | Описание                                                                      |
| --------------- | ------- | ------------ | ----------------------------------------------------------------------------- |
| title           | string  | Нет          | Название книги                                                                |
| authorName      | string  | Нет          | Имя автора (при изменении проверяется уникальность комбинации title + author) |
| description     | string  | Нет          | Описание книги                                                                |
| publishedYear   | integer | Нет          | Год публикации (1000-9999)                                                    |
| genre           | string  | Нет          | Жанр книги                                                                    |
| price           | number  | Нет          | Цена книги в USD                                                              |
| discountPercent | number  | Нет          | Процент скидки 0-100                                                          |
| image           | file    | Нет          | Изображение книги (максимум 5MB)                                              |
| pdf             | file    | Нет          | PDF файл (application/pdf, максимум 5MB)                                      |

**Response (200 OK):** BookResponse

**Ошибки:**

- `400` - Валидация не прошла или файл не является PDF (`VALIDATION_ERROR`)
- `404` - Книга не найдена (`BOOK_NOT_FOUND`)
- `403` - Недостаточно прав (`ACCESS_DENIED`)
- `409` - Конфликт уникальности (книга с таким названием и автором уже существует) (`BOOK_ALREADY_EXISTS`)

**Примечания:**

- Все поля опциональны. Можно обновить только поля, только изображение, только PDF, или любую комбинацию
- PDF файл должен быть в формате application/pdf
- При загрузке нового PDF файла, если у книги уже есть PDF, старый файл будет переименован с timestamp, а новый сохранится с тем же именем

---

##### DELETE /api/v1/admin/books/{bookId}

**Описание:** Удалить книгу  
**Авторизация:** Требуется (Bearer Token, роль ADMIN)

**Path Parameters:**

- `bookId` (Long, required) - ID книги

**Response (204 No Content):** Пустое тело ответа

**Ошибки:**

- `403` - Удаление запрещено администратором (deletion_locked = true в БД) (`ACCESS_DENIED`)
- `404` - Книга не найдена (`BOOK_NOT_FOUND`)
- `409` - Удаление запрещено (есть связанные отзывы) (`BOOK_HAS_REVIEWS`)

---

##### POST /api/v1/admin/books/{bookId}/image

**Описание:** Загрузить изображение книги  
**Авторизация:** Требуется (Bearer Token, роль ADMIN)  
**Content-Type:** `multipart/form-data`

**Path Parameters:**

- `bookId` (Long, required) - ID книги

**Form Data:**

| Поле | Тип  | Обязательный | Описание                         |
| ---- | ---- | ------------ | -------------------------------- |
| file | file | Да           | Изображение книги (максимум 5MB) |

**Response (200 OK):**

```json
{
  "message": "Image uploaded successfully"
}
```

**Ошибки:**

- `400` - Файл не предоставлен или пустой (`VALIDATION_ERROR`)
- `404` - Книга не найдена (`BOOK_NOT_FOUND`)
- `403` - Недостаточно прав (`ACCESS_DENIED`)

---

##### POST /api/v1/admin/books/{bookId}/pdf

**Описание:** Загрузить PDF файл для книги  
**Авторизация:** Требуется (Bearer Token, роль ADMIN)  
**Content-Type:** `multipart/form-data`

**Path Parameters:**

- `bookId` (Long, required) - ID книги

**Form Data:**

| Поле | Тип  | Обязательный | Описание                                 |
| ---- | ---- | ------------ | ---------------------------------------- |
| file | file | Да           | PDF файл (application/pdf, максимум 5MB) |

**Response (200 OK):**

```json
{
  "message": "PDF uploaded successfully"
}
```

**Ошибки:**

- `400` - Файл не является PDF или файл пустой (`VALIDATION_ERROR`)
- `404` - Книга не найдена (`BOOK_NOT_FOUND`)
- `403` - Недостаточно прав (`ACCESS_DENIED`)

---

#### Управление пользователями

##### GET /api/v1/admin/users

**Описание:** Получить список всех пользователей  
**Авторизация:** Требуется (Bearer Token, роль ADMIN)

**Response (200 OK):**

```json
[
  {
    "id": 1,
    "nickname": "admin",
    "email": "admin@gmail.com",
    "role": "ADMIN",
    "createdAt": "2025-12-21T19:09:33.964107"
  }
]
```

**Ошибки:**

- `403` - Недостаточно прав (`ACCESS_DENIED`)

---

##### DELETE /api/v1/admin/users/{userId}

**Описание:** Удалить пользователя  
**Авторизация:** Требуется (Bearer Token, роль ADMIN)

**Path Parameters:**

- `userId` (Long, required) - ID пользователя

**Response (204 No Content):** Пустое тело ответа

**Ошибки:**

- `403` - Недостаточно прав или попытка удалить администратора (`ACCESS_DENIED`, `CANNOT_DELETE_ADMIN`)
- `404` - Пользователь не найден (`USER_NOT_FOUND`)

---

### Аналитика (требуется роль ADMIN)

#### GET /api/v1/admin/analytics/books/{bookId}

**Описание:** Получить статистику по конкретной книге  
**Авторизация:** Требуется (Bearer Token, роль ADMIN)

**Path Parameters:**

- `bookId` (Long, required) - ID книги

**Response (200 OK):**

```json
{
  "bookId": 1,
  "bookTitle": "Spring Boot Guide",
  "bookGenre": "TECHNOLOGY",
  "viewCount": 150,
  "downloadCount": 45,
  "purchaseCount": 30,
  "reviewCount": 12,
  "ratingCount": 25,
  "averageRating": 8.5,
  "totalRevenue": 299.70,
  "uniqueViewers": 120,
  "uniqueDownloaders": 40,
  "uniquePurchasers": 28,
  "aggregatedAt": "2025-12-17T13:20:00",
  "createdAt": "2025-12-17T13:20:00"
}
```

**Ошибки:**

- `403` - Недостаточно прав (`ACCESS_DENIED`)
- `404` - Аналитика для книги не найдена (`NOT_FOUND`)

---

#### GET /api/v1/admin/analytics/books/{bookId}/history

**Описание:** Получить историю статистики по книге за указанный период  
**Авторизация:** Требуется (Bearer Token, роль ADMIN)

**Path Parameters:**

- `bookId` (Long, required) - ID книги

**Query Parameters:**

| Параметр  | Тип           | Обязательный | По умолчанию                    | Описание                    |
| --------- | -------------- | ------------ | ------------------------------- | --------------------------- |
| startDate | LocalDateTime  | Нет          | 7 дней назад от текущей даты    | Начало периода (ISO 8601)   |
| endDate   | LocalDateTime  | Нет          | Текущая дата                    | Конец периода (ISO 8601)    |

**Пример запроса:**

```
GET /api/v1/admin/analytics/books/1/history?startDate=2025-12-10T00:00:00&endDate=2025-12-17T23:59:59
```

**Response (200 OK):**

```json
{
  "bookId": 1,
  "bookTitle": "Spring Boot Guide",
  "history": [
    {
      "bookId": 1,
      "bookTitle": "Spring Boot Guide",
      "bookGenre": "TECHNOLOGY",
      "viewCount": 150,
      "downloadCount": 45,
      "purchaseCount": 30,
      "reviewCount": 12,
      "ratingCount": 25,
      "averageRating": 8.5,
      "totalRevenue": 299.70,
      "uniqueViewers": 120,
      "uniqueDownloaders": 40,
      "uniquePurchasers": 28,
      "aggregatedAt": "2025-12-17T13:20:00",
      "createdAt": "2025-12-17T13:20:00"
    }
  ]
}
```

**Ошибки:**

- `403` - Недостаточно прав (`ACCESS_DENIED`)
- `404` - История аналитики для книги не найдена (`NOT_FOUND`)

---

#### GET /api/v1/admin/analytics/overview

**Описание:** Получить общую статистику системы  
**Авторизация:** Требуется (Bearer Token, роль ADMIN)

**Response (200 OK):**

```json
{
  "totalBooks": 150,
  "totalUsers": 500,
  "totalPurchases": 1200,
  "totalRevenue": 15000.00,
  "totalReviews": 450,
  "totalRatings": 800,
  "averageBookRating": 7.8,
  "aggregatedAt": "2025-12-17T13:20:00",
  "createdAt": "2025-12-17T13:20:00"
}
```

**Ошибки:**

- `403` - Недостаточно прав (`ACCESS_DENIED`)
- `404` - Аналитика системы не найдена (`NOT_FOUND`)

---

#### GET /api/v1/admin/analytics/popular

**Описание:** Получить список популярных книг  
**Авторизация:** Требуется (Bearer Token, роль ADMIN)

**Query Parameters:**

| Параметр | Тип    | Обязательный | По умолчанию | Описание                                                                    |
| -------- | ------ | ------------ | ------------ | --------------------------------------------------------------------------- |
| limit    | int    | Нет          | 10           | Количество книг для возврата                                                |
| sortBy   | string | Нет          | "views"      | Поле для сортировки: `views`, `downloads`, `purchases`, `revenue`           |

**Примеры запросов:**

- `GET /api/v1/admin/analytics/popular?limit=20&sortBy=downloads`
- `GET /api/v1/admin/analytics/popular?sortBy=revenue`

**Response (200 OK):**

```json
{
  "sortBy": "views",
  "books": [
    {
      "bookId": 1,
      "bookTitle": "Spring Boot Guide",
      "bookGenre": "TECHNOLOGY",
      "viewCount": 150,
      "downloadCount": 45,
      "purchaseCount": 30,
      "totalRevenue": 299.70
    }
  ]
}
```

**Ошибки:**

- `403` - Недостаточно прав (`ACCESS_DENIED`)

---

#### GET /api/v1/admin/analytics/overview/history

**Описание:** Получить историю общей статистики системы за указанный период  
**Авторизация:** Требуется (Bearer Token, роль ADMIN)

**Query Parameters:**

| Параметр  | Тип           | Обязательный | По умолчанию                 | Описание                    |
| --------- | -------------- | ------------ | ---------------------------- | --------------------------- |
| startDate | LocalDateTime  | Нет          | 7 дней назад от текущей даты | Начало периода (ISO 8601)   |
| endDate   | LocalDateTime  | Нет          | Текущая дата                 | Конец периода (ISO 8601)    |

**Response (200 OK):**

```json
{
  "history": [
    {
      "totalBooks": 150,
      "totalUsers": 500,
      "totalPurchases": 1200,
      "totalRevenue": 15000.00,
      "totalReviews": 450,
      "totalRatings": 800,
      "averageBookRating": 7.8,
      "aggregatedAt": "2025-12-17T13:20:00",
      "createdAt": "2025-12-17T13:20:00"
    }
  ]
}
```

**Ошибки:**

- `403` - Недостаточно прав (`ACCESS_DENIED`)

---

### Служба поддержки

#### POST /api/v1/support

**Описание:** Отправить текстовое сообщение в службу поддержки через Telegram бот  
**Авторизация:** Требуется (Bearer Token, роли USER или ADMIN)  
**Content-Type:** `application/json`

**Request Body:**

```json
{
  "message": "Обнаружен баг: при нажатии на кнопку приложение крашится",
  "telegram": "@username"
}
```

**Параметры:**

| Поле     | Тип    | Обязательный | Описание                                                                                                |
| -------- | ------ | ------------ | ------------------------------------------------------------------------------------------------------- |
| message  | string | Да\*         | Текст сообщения (максимум 1000 символов). \*Обязательно для `/support`, опционально для `/support/file` |
| telegram | string | Нет          | Telegram аккаунт пользователя для обратной связи (максимум 100 символов)                                |

**Response (200 OK):**

```json
{
  "message": "Сообщение успешно отправлено в службу поддержки",
  "contentType": "text"
}
```

**Ошибки:**

- `400` - Валидация не прошла (сообщение пустое или превышает 1000 символов) (`VALIDATION_ERROR`)
- `401` - Не авторизован (`UNAUTHORIZED`)
- `500` - Ошибка при отправке сообщения в Telegram (бот не настроен или проблемы с API) (`INTERNAL_SERVER_ERROR`)

**Примечания:**

- Email пользователя автоматически добавляется к сообщению
- Если указан Telegram, в сообщении будет указано, что ответ будет на email или в Telegram
- Если Telegram не указан, будет указано, что ответ будет на email

---

#### POST /api/v1/support/file

**Описание:** Отправить файл (изображение или видео) в службу поддержки через Telegram бот  
**Авторизация:** Требуется (Bearer Token, роли USER или ADMIN)  
**Content-Type:** `multipart/form-data`

**Request Parameters:**

| Поле     | Тип    | Обязательный | Описание                                                                 |
| -------- | ------ | ------------ | ------------------------------------------------------------------------ |
| file     | File   | Да           | Файл для отправки (изображение или видео)                                |
| message  | string | Нет          | Опциональное текстовое сообщение (caption, максимум 1000 символов)       |
| telegram | string | Нет          | Telegram аккаунт пользователя для обратной связи (максимум 100 символов) |

**Поддерживаемые форматы:**

- **Изображения:** JPG, JPEG, PNG, GIF (Content-Type: `image/*`)
- **Видео:** MP4, MOV, AVI (Content-Type: `video/*`)
- **Другие файлы:** Если файл не является изображением или видео, будет отправлена информация о файле в текстовом виде

**Максимальный размер файла:** 5MB (настраивается в `spring.servlet.multipart.max-file-size`)

**Response (200 OK):**

```json
{
  "message": "Файл успешно отправлен в службу поддержки",
  "contentType": "photo"
}
```

**Возможные значения `contentType`:**

- `"text"` - отправлено текстовое сообщение
- `"photo"` - отправлено изображение
- `"video"` - отправлено видео
- `"document"` - отправлена информация о файле в текстовом виде

**Ошибки:**

- `400` - Файл не предоставлен или пустой (`VALIDATION_ERROR`)
- `401` - Не авторизован (`UNAUTHORIZED`)
- `500` - Ошибка при отправке файла в Telegram (бот не настроен или проблемы с API) (`INTERNAL_SERVER_ERROR`)

**Примечания:**

- Сообщения отправляются администратору через Telegram бот
- Для работы эндпоинтов необходимо настроить Telegram бот (токен и Chat ID в `application.properties`)
- Все сообщения и файлы доставляются в личный чат администратора в Telegram
- Email пользователя автоматически добавляется к каждому сообщению

---

### Системные эндпоинты

#### GET /api/v1/health

**Описание:** Проверка состояния приложения  
**Авторизация:** Не требуется

**Response (200 OK):**

```json
{
  "status": "UP",
  "uptime": 12345,
  "timestamp": "2025-12-17T13:20:00Z"
}
```

**Поля ответа:**

| Поле      | Тип    | Описание                                |
| --------- | ------ | --------------------------------------- |
| status    | string | Статус приложения ("UP" или "DOWN")     |
| uptime    | long   | Время работы приложения в миллисекундах |
| timestamp | string | Временная метка в формате ISO 8601      |

---

#### GET /api/v1/kuberinfo

**Описание:** Информация о Kubernetes окружении  
**Авторизация:** Не требуется

**Response (200 OK):**

```json
{
  "pod": "pod-name",
  "node": "node-name",
  "os": "Linux"
}
```

**Поля ответа:**

| Поле | Тип    | Описание              |
| ---- | ------ | --------------------- |
| pod  | string | Имя pod в Kubernetes  |
| node | string | Имя node в Kubernetes |
| os   | string | Операционная система  |

**Примечание:** Информация доступна только при запуске в Kubernetes окружении.

---

## DTO (Data Transfer Objects)

### Request DTOs

#### RegisterRequest

```json
{
  "nickname": "string", // 3-50 символов, только буквы, цифры, дефисы, подчеркивания, точки
  "email": "string", // валидный email
  "password": "string" // минимум 8 символов, должна содержать: цифру, строчную, заглавную, спецсимвол, без пробелов
}
```

#### LoginRequest

```json
{
  "email": "string",
  "password": "string"
}
```

#### RefreshTokenRequest

```json
{
  "refreshToken": "string"
}
```

#### CreateBookRequest

```json
{
  "title": "string", // обязательное
  "authorName": "string", // обязательное
  "description": "string", // опциональное
  "publishedYear": 0, // опциональное, 1000-9999
  "genre": "Genre", // опциональное
  "price": 0.0, // опциональное, цена в USD (по умолчанию 0.00)
  "discountPercent": 0.0 // опциональное, процент скидки 0-100 (по умолчанию 0.00)
}
```

#### UpdateBookRequest

```json
{
  "title": "string", // опциональное
  "authorName": "string", // опциональное
  "description": "string", // опциональное
  "publishedYear": 0, // опциональное, 1000-9999
  "genre": "Genre", // опциональное
  "price": 0.0, // опциональное, цена в USD
  "discountPercent": 0.0 // опциональное, процент скидки 0-100
}
```

#### PutBookRequest

```json
{
  "title": "string", // обязательное
  "authorName": "string", // обязательное
  "description": "string", // обязательное
  "publishedYear": 0, // обязательное, 1000-9999
  "genre": "Genre", // обязательное
  "price": 0.0, // обязательное, цена в USD
  "discountPercent": 0.0 // обязательное, процент скидки 0-100
}
```

#### CreateReviewRequest

```json
{
  "text": "string" // обязательное
}
```

#### UpdateReviewRequest

```json
{
  "text": "string" // обязательное
}
```

#### CreateRatingRequest

```json
{
  "value": 0 // обязательное, 1-10
}
```

#### UpdateRatingRequest

```json
{
  "value": 0 // обязательное, 1-10
}
```

#### MessageRequest

```json
{
  "message": "string" // обязательное, максимум 256 символов
}
```

#### ForgotPasswordRequest

```json
{
  "email": "string" // обязательное, валидный email
}
```

#### ResetPasswordRequest

```json
{
  "token": "string",        // обязательное, UUID токен из письма
  "newPassword": "string"   // обязательное, минимум 8 символов, должна содержать: цифру, строчную, заглавную, спецсимвол, без пробелов
}
```

#### ResendVerificationRequest

```json
{
  "email": "string" // обязательное, валидный email
}
```

#### SupportRequest

```json
{
  "message": "string", // обязательное для /support (проверяется в контроллере), опциональное для /support/file, максимум 1000 символов
  "telegram": "string" // опциональное, максимум 100 символов
}
```

#### ForgotPasswordRequest

```json
{
  "email": "string" // обязательное, валидный email
}
```

#### ResetPasswordRequest

```json
{
  "token": "string",        // обязательное, UUID токен из письма
  "newPassword": "string"   // обязательное, минимум 8 символов, должна содержать: цифру, строчную, заглавную, спецсимвол, без пробелов
}
```

#### ResendVerificationRequest

```json
{
  "email": "string" // обязательное, валидный email
}
```

---

### Response DTOs

#### UserInfoResponse

```json
{
  "id": 0,
  "nickname": "string",
  "email": "string",
  "role": "USER" | "ADMIN"
}
```

#### RegisterResponse

```json
{
  "userId": 0,
  "email": "string",
  "role": "USER" | "ADMIN",
  "message": "string"  // Сообщение с инструкцией о необходимости подтвердить email
}
```

#### LoginResponse

```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "tokenType": "Bearer"
}
```

#### BookResponse

```json
{
  "id": 0,
  "title": "string",
  "author": {
    "id": 0,
    "fullName": "string"
  },
  "description": "string",
  "publishedYear": 0,
  "genre": "Genre",
  "ratingAvg": 0.0, // BigDecimal, 0.00-10.00
  "ratingCount": 0,
  "hasFile": true, // boolean, наличие PDF файла
  "imagePath": "string",
  "price": 0.0, // BigDecimal, цена в USD
  "discountPercent": 0.0, // BigDecimal, процент скидки 0.00-100.00
  "finalPrice": 0.0, // BigDecimal, финальная цена с учетом скидки
  "createdAt": "2025-12-17T13:20:00", // ISO 8601
  "updatedAt": "2025-12-17T13:20:00", // ISO 8601
  "reviews": [] // массив ReviewResponse (может быть пустым)
}
```

#### AuthorResponse

```json
{
  "id": 0,
  "fullName": "string"
}
```

#### ReviewResponse

```json
{
  "id": 0,
  "bookId": 0,
  "user": {
    "id": 0,
    "nickname": "string",
    "email": "string"
  },
  "text": "string",
  "createdAt": "2025-12-17T13:20:00", // ISO 8601
  "updatedAt": "2025-12-17T13:20:00" // ISO 8601
}
```

#### RatingResponse

```json
{
  "id": 0,
  "bookId": 0,
  "userId": 0,
  "value": 0, // 1-10
  "createdAt": "2025-12-17T13:20:00", // ISO 8601
  "updatedAt": "2025-12-17T13:20:00" // ISO 8601
}
```

#### AdminUserResponse

```json
{
  "id": 0,
  "nickname": "string",
  "email": "string",
  "role": "USER" | "ADMIN",
  "createdAt": "2025-12-17T13:20:00"  // ISO 8601
}
```

#### MessageResponse

```json
{
  "message": "string"
}
```

#### SupportResponse

```json
{
  "message": "string",          // Сообщение об успешной отправке
  "contentType": "text" | "photo" | "video" | "document"  // Тип отправленного контента
}
```

#### ErrorResponse

```json
{
  "status": 0, // HTTP статус код
  "error": "string", // Код типа ошибки
  "message": "string", // Сообщение об ошибке
  "fieldErrors": {
    // Только для ошибок валидации
    "fieldName": "error message"
  },
  "timestamp": "2025-12-17T13:20:00Z", // ISO 8601
  "path": "string" // Путь запроса
}
```

#### HealthResponse

```json
{
  "status": "UP" | "DOWN",
  "uptime": 0,                   // long, время работы в миллисекундах
  "timestamp": "2025-12-17T13:20:00Z"  // ISO 8601
}
```

#### KuberInfoResponse

```json
{
  "pod": "string",
  "node": "string",
  "os": "string"
}
```

#### BookAnalyticsResponse

```json
{
  "bookId": 0,
  "bookTitle": "string",
  "bookGenre": "string",
  "viewCount": 0,
  "downloadCount": 0,
  "purchaseCount": 0,
  "reviewCount": 0,
  "ratingCount": 0,
  "averageRating": 0.0,  // BigDecimal
  "totalRevenue": 0.0,   // BigDecimal
  "uniqueViewers": 0,
  "uniqueDownloaders": 0,
  "uniquePurchasers": 0,
  "aggregatedAt": "2025-12-17T13:20:00",  // ISO 8601
  "createdAt": "2025-12-17T13:20:00"      // ISO 8601
}
```

#### BookAnalyticsHistoryResponse

```json
{
  "bookId": 0,
  "bookTitle": "string",
  "history": [
    {
      // BookAnalyticsResponse объекты
    }
  ]
}
```

#### SystemAnalyticsResponse

```json
{
  "totalBooks": 0,
  "totalUsers": 0,
  "totalPurchases": 0,
  "totalRevenue": 0.0,        // BigDecimal
  "totalReviews": 0,
  "totalRatings": 0,
  "averageBookRating": 0.0,    // BigDecimal
  "aggregatedAt": "2025-12-17T13:20:00",  // ISO 8601
  "createdAt": "2025-12-17T13:20:00"      // ISO 8601
}
```

#### SystemAnalyticsHistoryResponse

```json
{
  "history": [
    {
      // SystemAnalyticsResponse объекты
    }
  ]
}
```

#### PopularBooksResponse

```json
{
  "sortBy": "string",  // "views", "downloads", "purchases", "revenue"
  "books": [
    {
      "bookId": 0,
      "bookTitle": "string",
      "bookGenre": "string",
      "viewCount": 0,
      "downloadCount": 0,
      "purchaseCount": 0,
      "totalRevenue": 0.0  // BigDecimal
    }
  ]
}
```

---

## Обработка ошибок

### Структура ErrorResponse

Все ошибки возвращаются в стандартизированном формате:

```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "fieldErrors": {
    "title": "Title is required",
    "authorName": "Author name is required"
  },
  "timestamp": "2025-12-17T13:20:00Z",
  "path": "/api/v1/admin/books"
}
```

### Коды ошибок (error field)

| Код                         | HTTP Status | Описание                               |
| --------------------------- | ----------- | -------------------------------------- |
| `VALIDATION_ERROR`          | 400         | Ошибка валидации                       |
| `UNAUTHORIZED`              | 401         | Не авторизован                         |
| `ACCESS_DENIED`             | 403         | Недостаточно прав                      |
| `NOT_FOUND`                 | 404         | Ресурс не найден                       |
| `BOOK_NOT_FOUND`            | 404         | Книга не найдена                       |
| `USER_NOT_FOUND`            | 404         | Пользователь не найден                 |
| `AUTHOR_NOT_FOUND`          | 404         | Автор не найден                        |
| `REVIEW_NOT_FOUND`          | 404         | Отзыв не найден                        |
| `RATING_NOT_FOUND`          | 404         | Рейтинг не найден                      |
| `EMAIL_ALREADY_EXISTS`      | 409         | Email уже существует                   |
| `BOOK_ALREADY_EXISTS`       | 409         | Книга уже существует                   |
| `REVIEW_ALREADY_EXISTS`     | 409         | Отзыв уже существует                   |
| `RATING_ALREADY_EXISTS`     | 409         | Рейтинг уже существует                 |
| `BOOK_HAS_REVIEWS`          | 409         | Книга имеет отзывы, удаление запрещено |
| `CANNOT_DELETE_ADMIN`       | 403         | Нельзя удалить администратора          |
| `INVALID_GENRE`             | 400         | Несуществующий жанр                    |
| `AUTHOR_CHANGE_NOT_ALLOWED` | 400         | Изменение автора запрещено             |
| `PAYMENT_REQUIRED`          | 402         | Требуется оплата для скачивания        |
| `BOOK_ALREADY_PURCHASED`    | 400         | Книга уже оплачена                     |
| `INTERNAL_SERVER_ERROR`     | 500         | Внутренняя ошибка сервера              |
| `GONE`                      | 410         | Токен истек или уже использован        |
| `BAD_REQUEST`               | 400         | Неверный запрос (например, email уже верифицирован) |

### Обработка исключений

Система использует `GlobalExceptionHandler` для централизованной обработки ошибок:

- `MethodArgumentNotValidException` → `400 VALIDATION_ERROR` (с fieldErrors)
- `HttpMessageNotReadableException` → `400 VALIDATION_ERROR` или `400 INVALID_GENRE`
- `ResponseStatusException` → соответствующий HTTP статус с определенным кодом ошибки
- `RuntimeException` → `500 INTERNAL_SERVER_ERROR`

---

## Интеграция с внешними сервисами

### 1. Stripe (Платежи)

#### Настройка

**Переменные окружения:**

```properties
stripe.secret-key=${STRIPE_SECRET_KEY:sk_test_...}
stripe.webhook-secret=${STRIPE_WEBHOOK_SECRET:whsec_...}
stripe.success-url=${STRIPE_SUCCESS_URL:http://localhost:8080/api/v1/payment/success}
stripe.cancel-url=${STRIPE_CANCEL_URL:http://localhost:8080/api/v1/payment/cancel}
```

**Получение ключей:**

1. Зарегистрируйтесь на [Stripe](https://stripe.com)
2. Получите Secret Key из Dashboard → Developers → API keys
3. Создайте Webhook endpoint в Dashboard → Developers → Webhooks
4. Получите Webhook Secret (начинается с `whsec_...`)

#### Механика работы

1. **Создание checkout session:**

   - Клиент вызывает `POST /api/v1/payment/checkout/{bookId}`
   - Сервер создает Stripe Checkout Session
   - Возвращает `checkoutUrl` для редиректа

2. **Обработка оплаты:**

   - Пользователь оплачивает на странице Stripe
   - Stripe отправляет webhook на `POST /api/v1/payment/webhook`
   - Сервер обрабатывает событие `checkout.session.completed`
   - Создается/обновляется запись Purchase со статусом COMPLETED

3. **События webhook:**

   - `checkout.session.completed` - успешная оплата
   - `payment_intent.succeeded` - успешная оплата (резервный вариант)
   - `payment_intent.payment_failed` - неудачная оплата

4. **Проверка подписи:**
   - Webhook проверяет подпись Stripe через `Stripe-Signature` header
   - Используется `stripe.webhook-secret` для проверки

#### Настройка webhook для локальной разработки

Используйте [Stripe CLI](https://stripe.com/docs/stripe-cli):

```bash
stripe listen --forward-to localhost:8080/api/v1/payment/webhook
```

Это даст вам webhook secret, который нужно добавить в `stripe.webhook-secret`.

---

### 2. Telegram Bot API (Служба поддержки)

#### Настройка

**Переменные окружения:**

```properties
telegram.bot.token=${TELEGRAM_BOT_TOKEN:...}
telegram.bot.chat-id=${TELEGRAM_CHAT_ID:...}
```

**Создание бота:**

1. Напишите [@BotFather](https://t.me/botfather) в Telegram
2. Отправьте команду `/newbot`
3. Следуйте инструкциям для создания бота
4. Получите токен (формат: `123456789:ABCdefGHIjklMNOpqrsTUVwxyz`)

**Получение Chat ID:**

1. Напишите боту [@userinfobot](https://t.me/userinfobot) - он покажет ваш Chat ID
2. Или отправьте сообщение вашему боту и вызовите `getUpdates` API:
   ```bash
   curl https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates
   ```
3. Найдите `chat.id` в ответе

#### Механика работы

1. **Отправка текстового сообщения:**

   - Клиент вызывает `POST /api/v1/support`
   - Сервер отправляет сообщение в Telegram через Bot API
   - Email пользователя автоматически добавляется к сообщению

2. **Отправка файла:**

   - Клиент вызывает `POST /api/v1/support/file`
   - Сервер определяет тип файла (изображение/видео/другое)
   - Отправляет файл в Telegram через соответствующий метод API
   - Если указан `message`, используется как caption

3. **API методы:**
   - `sendMessage` - для текстовых сообщений
   - `sendPhoto` - для изображений
   - `sendVideo` - для видео
   - `sendMessage` - для других файлов (отправляется информация о файле)

---

### 3. OpenAI API (Генерация ответов читателей)

#### Настройка

**Переменные окружения:**

```properties
openai.api.key=${OPENAI_API_KEY:...}
```

**Получение API ключа:**

1. Зарегистрируйтесь на [OpenAI](https://platform.openai.com)
2. Перейдите в API Keys
3. Создайте новый ключ
4. Скопируйте ключ (начинается с `sk-...`)

#### Механика работы

1. **Генерация ответа:**

   - Клиент вызывает `POST /api/v1/books/{bookId}/message/censored` или `/uncensored`
   - Сервер получает информацию о книге из БД
   - Формирует промпт для OpenAI с информацией о книге и вопросом пользователя
   - Отправляет запрос в OpenAI API
   - Возвращает сгенерированный ответ

2. **Параметры запроса:**

   - Модель: `gpt-3.5-turbo` или `gpt-4`
   - Temperature: настраивается в коде
   - Max tokens: настраивается в коде

3. **Обработка ошибок:**
   - При ошибке API возвращается `500 INTERNAL_SERVER_ERROR`
   - Логируется детальная информация об ошибке

---

### 4. Google Gemini API (Альтернатива OpenAI)

#### Настройка

**Переменные окружения:**

```properties
gemini.api.key=${GEMINI_API_KEY:...}
```

**Получение API ключа:**

1. Зарегистрируйтесь на [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Создайте новый API ключ
3. Скопируйте ключ

#### Механика работы

Аналогично OpenAI API, используется как альтернатива для генерации ответов читателей.

---

## Конфигурация

### Переменные окружения

#### База данных

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/spring_digital_bookstore
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password
```

#### JWT

```properties
JWT_SECRET=your_secret_key
JWT_EXPIRATION=300000  # 5 минут в миллисекундах
JWT_REFRESH_EXPIRATION=86400000  # 24 часа в миллисекундах
```

#### Stripe

```properties
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_SUCCESS_URL=http://localhost:8080/api/v1/payment/success
STRIPE_CANCEL_URL=http://localhost:8080/api/v1/payment/cancel
```

#### Telegram

```properties
TELEGRAM_BOT_TOKEN=123456789:ABCdefGHIjklMNOpqrsTUVwxyz
TELEGRAM_CHAT_ID=123456789
```

#### OpenAI

```properties
OPENAI_API_KEY=sk-...
```

#### Google Gemini

```properties
GEMINI_API_KEY=...
```

#### Email Verification

```properties
EMAIL_VERIFICATION_TOKEN_EXPIRATION_HOURS=24
EMAIL_VERIFICATION_BASE_URL=http://localhost:8080
```

#### Password Reset

```properties
PASSWORD_RESET_TOKEN_EXPIRATION_HOURS=1
```

#### Хранилище файлов

```properties
APP_IMAGES_STORAGE_PATH=/opt/spring-digital-bookstore/pictures
APP_PDF_STORAGE_PATH=/opt/spring-digital-bookstore/pdf
```

#### Frontend URL

```properties
FRONTEND_URL=http://localhost:3000
```

### application.properties

Основные настройки находятся в `src/main/resources/application.properties`:

```properties
# Server
server.forward-headers-strategy=framework

# Database
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/spring_digital_bookstore}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}

# JPA
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=create  # или update/validate/none

# JWT
jwt.secret=${JWT_SECRET}
jwt.expiration=300000
jwt.refresh-expiration=86400000

# File upload
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB

# Storage paths
app.images.storage-path=${APP_IMAGES_STORAGE_PATH:G:\\opt\\spring-digital-bookstore\\pictures}
app.pdf.storage-path=${APP_PDF_STORAGE_PATH:G:\\opt\\spring-digital-bookstore\\pdf}

# Stripe
stripe.secret-key=${STRIPE_SECRET_KEY}
stripe.webhook-secret=${STRIPE_WEBHOOK_SECRET}
stripe.success-url=${STRIPE_SUCCESS_URL:http://localhost:8080/api/v1/payment/success}
stripe.cancel-url=${STRIPE_CANCEL_URL:http://localhost:8080/api/v1/payment/cancel}

# Telegram
telegram.bot.token=${TELEGRAM_BOT_TOKEN}
telegram.bot.chat-id=${TELEGRAM_CHAT_ID}

# OpenAI
openai.api.key=${OPENAI_API_KEY}

# Gemini
gemini.api.key=${GEMINI_API_KEY}

# Frontend
app.frontend-url=${FRONTEND_URL:http://localhost:3000}

# Email Verification
app.email.verification.token-expiration-hours=${EMAIL_VERIFICATION_TOKEN_EXPIRATION_HOURS:24}
app.email.verification.base-url=${EMAIL_VERIFICATION_BASE_URL:http://localhost:8080}

# Password Reset
app.password.reset.token-expiration-hours=${PASSWORD_RESET_TOKEN_EXPIRATION_HOURS:1}
```

---

## Безопасность

### JWT Authentication

1. **Регистрация/Вход:**

   - Пользователь регистрируется или входит в систему
   - Сервер генерирует пару токенов (access + refresh)
   - Клиент сохраняет токены

2. **Использование токена:**

   - Клиент отправляет access token в заголовке: `Authorization: Bearer <token>`
   - Сервер проверяет токен через `JwtAuthenticationFilter`
   - При истечении токена клиент использует refresh token для получения новой пары

3. **Обновление токена:**
   - Клиент вызывает `POST /api/v1/auth/refresh` с refresh token
   - Сервер проверяет refresh token и выдает новую пару токенов

### Security Configuration

**Публичные эндпоинты:**

- `/api/v1/auth/**` - аутентификация
- `/api/v1/health` - проверка здоровья
- `/api/v1/kuberinfo` - информация о Kubernetes
- `/api/v1/payment/webhook` - Stripe webhook
- `/api/v1/payment/success` - страница успешной оплаты
- `/api/v1/payment/cancel` - страница отмены оплаты
- `/api/v1/books` - список книг (GET)
- `/api/v1/books/{id}` - детали книги (GET)
- `/api/v1/books/{id}/image` - изображение книги (GET)
- `/api/v1/books/images/all` - все изображения (GET)
- `/api/v1/books/{id}/reviews` - отзывы на книгу (GET)
- Swagger UI и документация

**Защищенные эндпоинты:**

- `/api/v1/users/**` - информация о пользователе (требуется авторизация)
- `/api/v1/books/{id}/download` - скачивание PDF (требуется авторизация)
- `/api/v1/books/{id}/ratings/**` - рейтинги (требуется авторизация)
- `/api/v1/books/{id}/reviews/**` - отзывы (POST/PUT требуют авторизации)
- `/api/v1/books/{id}/message/**` - сообщения читателям (требуется авторизация)
- `/api/v1/support/**` - служба поддержки (требуется авторизация)
- `/api/v1/payment/**` - платежи (кроме webhook, success, cancel)

**Административные эндпоинты:**

- `/api/v1/admin/**` - все административные функции (требуется роль ADMIN)
  - `/api/v1/admin/books/**` - управление книгами
  - `/api/v1/admin/users/**` - управление пользователями
  - `/api/v1/admin/analytics/**` - аналитика системы и книг
  - `/api/v1/admin/books/**` - управление книгами
  - `/api/v1/admin/users/**` - управление пользователями
  - `/api/v1/admin/analytics/**` - аналитика системы и книг

---

## Примеры использования

### Регистрация и вход

```bash
# Регистрация
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nickname": "john_doe",
    "email": "john@example.com",
    "password": "Password123!"
  }'

# Вход
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "Password123!"
  }'
```

### Получение списка книг

```bash
# Базовый запрос
curl http://localhost:8080/api/v1/books

# С пагинацией и сортировкой
curl "http://localhost:8080/api/v1/books?page=0&size=20&sort=ratingAvg,desc"

# С фильтрацией по жанру
curl "http://localhost:8080/api/v1/books?genre=TECHNOLOGY&page=0&size=10"
```

### Создание отзыва (требуется авторизация)

```bash
curl -X POST http://localhost:8080/api/v1/books/1/reviews \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access_token>" \
  -d '{
    "text": "Отличная книга!"
  }'
```

### Создание checkout session для оплаты

```bash
curl -X POST http://localhost:8080/api/v1/payment/checkout/1 \
  -H "Authorization: Bearer <access_token>"
```

### Отправка сообщения в службу поддержки

```bash
curl -X POST http://localhost:8080/api/v1/support \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access_token>" \
  -d '{
    "message": "Обнаружен баг: при нажатии на кнопку приложение крашится",
    "telegram": "@username"
  }'
```

### Загрузка PDF файла (требуется роль ADMIN)

```bash
curl -X POST http://localhost:8080/api/v1/admin/books/1/pdf \
  -H "Authorization: Bearer <admin_access_token>" \
  -F "file=@/path/to/book.pdf"
```

---

## Подключение сторонних микросервисов

### Общие принципы

1. **Аутентификация:** Используйте JWT Bearer Token для всех защищенных эндпоинтов
2. **Base URL:** Все эндпоинты начинаются с `/api/v1`
3. **Content-Type:**
   - JSON запросы: `application/json`
   - Загрузка файлов: `multipart/form-data`
4. **Обработка ошибок:** Всегда проверяйте статус код и структуру ErrorResponse

### Пример интеграции (Java)

```java
@Service
public class DigitalLibraryClient {

    private final RestTemplate restTemplate;
    private final String baseUrl = "http://localhost:8080/api/v1";

    public BookResponse getBook(Long bookId) {
        String url = baseUrl + "/books/" + bookId;
        return restTemplate.getForObject(url, BookResponse.class);
    }

    public LoginResponse login(String email, String password) {
        String url = baseUrl + "/auth/login";
        LoginRequest request = new LoginRequest(email, password);
        return restTemplate.postForObject(url, request, LoginResponse.class);
    }

    public BookResponse createBook(String accessToken, CreateBookRequest request) {
        String url = baseUrl + "/admin/books";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateBookRequest> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForObject(url, entity, BookResponse.class);
    }
}
```

### Пример интеграции (Python)

```python
import requests

class DigitalLibraryClient:
    def __init__(self, base_url="http://localhost:8080/api/v1"):
        self.base_url = base_url
        self.access_token = None

    def login(self, email, password):
        url = f"{self.base_url}/auth/login"
        response = requests.post(url, json={"email": email, "password": password})
        response.raise_for_status()
        data = response.json()
        self.access_token = data["accessToken"]
        return data

    def get_book(self, book_id):
        url = f"{self.base_url}/books/{book_id}"
        response = requests.get(url)
        response.raise_for_status()
        return response.json()

    def create_review(self, book_id, text):
        url = f"{self.base_url}/books/{book_id}/reviews"
        headers = {"Authorization": f"Bearer {self.access_token}"}
        response = requests.post(url, json={"text": text}, headers=headers)
        response.raise_for_status()
        return response.json()
```

### Пример интеграции (Node.js/TypeScript)

```typescript
class DigitalLibraryClient {
  private baseUrl: string;
  private accessToken: string | null = null;

  constructor(baseUrl: string = "http://localhost:8080/api/v1") {
    this.baseUrl = baseUrl;
  }

  async login(email: string, password: string): Promise<LoginResponse> {
    const response = await fetch(`${this.baseUrl}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    });
    if (!response.ok) throw new Error(`Login failed: ${response.statusText}`);
    const data = await response.json();
    this.accessToken = data.accessToken;
    return data;
  }

  async getBook(bookId: number): Promise<BookResponse> {
    const response = await fetch(`${this.baseUrl}/books/${bookId}`);
    if (!response.ok)
      throw new Error(`Failed to get book: ${response.statusText}`);
    return response.json();
  }

  async createReview(bookId: number, text: string): Promise<ReviewResponse> {
    if (!this.accessToken) throw new Error("Not authenticated");
    const response = await fetch(`${this.baseUrl}/books/${bookId}/reviews`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${this.accessToken}`,
      },
      body: JSON.stringify({ text }),
    });
    if (!response.ok)
      throw new Error(`Failed to create review: ${response.statusText}`);
    return response.json();
  }
}
```

---

## Swagger/OpenAPI документация

Документация API доступна по адресу:

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

Все эндпоинты документированы с помощью аннотаций OpenAPI 3.0.

---

## Заключение

Этот документ содержит полное описание API Spring Digital Bookstore, включая:

- Все эндпоинты с детальным описанием
- Структуры данных (DTO и Entity)
- Механики взаимодействия с внешними сервисами
- Обработку ошибок
- Примеры интеграции

Для дополнительной информации обращайтесь к исходному коду или Swagger документации.
