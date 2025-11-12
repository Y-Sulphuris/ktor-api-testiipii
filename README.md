# Ktor API TestIIPII
Сложная система высокой надёжности. При создании ни один воображаемый клиент не пострадал.
***
### Postman
Коллекция и окружение находится в [postman/](postman)<br>
Описание структуры коллекции:
```
Ktor-api-testiipii/
├── User/
│   ├── Login
│   ├── Refresh
│   ├── Logout
│   ├── Create
│   ├── Read
│   └── Update
├── Product/
│   ├── Create
│   ├── Read
│   ├── Update
│   └── Delete
└── Order/
    ├── Create
    ├── Read
    ├── Update
    └── Delete
```

Скриншот с выплонеными тестами:<br>
<img width="772" height="184" alt="image" src="https://github.com/user-attachments/assets/7805a084-7e61-4a5d-bb48-9211bb442141" />


Для тестов использовался тестовый пользователь, так же известный как собирательный персонаж admin admin:
```json
{
  "name": "Admin",
  "email": "admin@example.com",
  "password": "admin"
}
```
***

### Аутентификация (JWT)
Эндпоинт для получения токена: `/authenitification/login`.
***
### Swagger
Документация находится в [openapi/documentation.yaml](src/main/resources/openapi/documentation.yaml). _В общем там же где и обычно._

## Аутентификация

### Вход в систему
```bash
curl -X POST "http://localhost:8080/authentication/login" \
-H "Content-Type: application/json" \
-d '{
  "email": "user@example.com",
  "password": "p@ssw0rd!"
}'
```
#### Возможные ответы:
- Успех (`200`)
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
  "token_type": "Bearer"
}
```
- отсутствуют поля email или password (`400`)
- неверные учётные данные (`401`)

---

### Обновление токена
```bash
curl -X POST "http://localhost:8080/authentication/refresh-token" \
-H "Content-Type: application/json" \
-d '{
  "refresh_token": "your_refresh_token"
}'
```
#### Возможные ответы:
- Успех (`200`)
```json
{
  "access_token": "new_access_token",
  "refresh_token": "new_refresh_token",
  "token_type": "Bearer"
}
```
- отсутствует refresh_token (`400`)
- недействительный refresh_token (`401`)

---

### Выход из системы
```bash
curl -X POST "http://localhost:8080/authentication/logout" \
-H "Authorization: Bearer <access_token>"
```
#### Возможные ответы:
- Успех (`200`)
```json
{ "message": "Logged out successfully" }
```
- отсутствует токен (`400`)

---

## Users (Пользователи)

### Создание пользователя
```bash
curl -X POST "http://localhost:8080/users" \
-H "Content-Type: application/json" \
-d '{
  "name": "John Smith",
  "email": "john@example.com",
  "password": "p@ssw0rd!"
}'
```
#### Возможные ответы:
- Успех (`201`)
```json
{
  "id": 1,
  "name": "John Smith",
  "email": "john@example.com"
}
```
- не все поля заполнены (`400`)
- пользователь с таким email уже существует (`409`)

---

### Получение списка пользователей
```bash
curl -X GET "http://localhost:8080/users?page=1&limit=10"
```
#### Возможные ответы:
- Успех (`200`)
```json
[
  { "id": 1, "name": "John Smith", "email": "john@example.com" }
]
```

---

### Получение пользователя по ID
```bash
curl -X GET "http://localhost:8080/users/1"
```
#### Возможные ответы:
- Успех (`200`)
```json
{
  "id": 1,
  "name": "John Smith",
  "email": "john@example.com"
}
```
- пользователь не найден (`404`)

---

### Обновление пользователя
```bash
curl -X PUT "http://localhost:8080/users/1" \
-H "Authorization: Bearer <access_token>" \
-H "Content-Type: application/json" \
-d '{
  "name": "John Smith",
  "email": "johnsmith@example.com"
}'
```
#### Возможные ответы:
- Успех (`200`)
```json
{ "message": "User updated" }
```
- пользователь не найден (`404`)
- email уже занят другим пользователем (`409`)

---

### Удаление пользователя
```bash
curl -X DELETE "http://localhost:8080/users/1" \
-H "Authorization: Bearer <access_token>"
```
#### Возможные ответы:
- Успех (`200`)
```json
{ "message": "User deleted" }
```
- пользователь не найден (`404`)

---

## Products (Продукты, Товары, и т.п.)

### Создание продукта
```bash
curl -X POST "http://localhost:8080/products" \
-H "Authorization: Bearer <access_token>" \
-H "Content-Type: application/json" \
-d '{
  "name": "Car",
  "description": "Sportcar",
  "price": 1399.99
}'
```
#### Возможные ответы:
- Успех (`201`)
```json
{
  "id": 1,
  "name": "Car",
  "description": "Sportcar",
  "price": 1399.99
}
```
- отсутствует название или некорректная цена (`400`)

---

### Получение списка продуктов
```bash
curl -X GET "http://localhost:8080/products?name=Car"
```
#### Возможные ответы:
- Успех (`200`)
```json
[
  {
    "id": 1,
    "name": "Car",
    "description": "Sportcar",
    "price": 1399.99
  }
]
```

---

### Получение продукта по ID
```bash
curl -X GET "http://localhost:8080/products/1"
```
#### Возможные ответы:
- Успех (`200`)
```json
{
  "id": 1,
  "name": "Car",
  "description": "Sportcar",
  "price": 1399.99
}
```
- продукт не найден (`404`)

---

### Обновление продукта
```bash
curl -X PUT "http://localhost:8080/products/1" \
-H "Authorization: Bearer <access_token>" \
-H "Content-Type: application/json" \
-d '{
  "name": "Sportcar",
  "price": 1099.99
}'
```
#### Возможные ответы:
- Успех (`200`)
```json
{ "message": "Product updated" }
```
- продукт не найден (`404`)

---

### Удаление продукта
```bash
curl -X DELETE "http://localhost:8080/products/1" \
-H "Authorization: Bearer <access_token>"
```
#### Возможные ответы:
- Успех (`200`)
```json
{ "message": "Product deleted" }
```
- продукт не найден (`404`)

---

## Orders (Заказы)

### Создание заказа
```bash
curl -X POST "http://localhost:8080/orders" \
-H "Authorization: Bearer <access_token>" \
-H "Content-Type: application/json" \
-d '{
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 2, "quantity": 1 }
  ]
}'
```
#### Возможные ответы:
- Успех (`201`)
```json
{
  "id": 1,
  "userId": 1,
  "items": [
    {
      "productId": 1,
      "productName": "Car",
      "quantity": 2,
      "price": 1399.99,
      "total": 1999.98
    }
  ],
  "totalAmount": 1999.98,
  "createdAt": "2023-10-01T12:00:00Z"
}
```
- заказ без товаров (`400`)

---

### Получение заказов пользователя
```bash
curl -X GET "http://localhost:8080/orders" \
-H "Authorization: Bearer <access_token>"
```
#### Возможные ответы:
- Успех (`200`)
```json
[
  {
    "id": 1,
    "userId": 1,
    "items": [...],
    "totalAmount": 1999.98,
    "createdAt": "2023-10-01T12:00:00Z"
  }
]
```

---

### Получение заказа по ID
```bash
curl -X GET "http://localhost:8080/orders/1" \
-H "Authorization: Bearer <access_token>"
```
#### Возможные ответы:
- Успех (`200`)
```json
{
  "id": 1,
  "userId": 1,
  "items": [...],
  "totalAmount": 1999.98,
  "createdAt": "2023-10-01T12:00:00Z"
}
```
- заказ не найден (`404`)

---

### Обновление заказа
```bash
curl -X PUT "http://localhost:8080/orders/1" \
-H "Authorization: Bearer <access_token>" \
-H "Content-Type: application/json" \
-d '{
  "items": [{ "productId": 1, "quantity": 3 }]
}'
```
#### Возможные ответы:
- Успех (`200`)
```json
{
  "id": 1,
  "userId": 1,
  "items": [...],
  "totalAmount": 2999.97,
  "updatedAt": "2023-10-01T14:30:00Z"
}
```
- заказ не найден (`404`)

---

### Удаление заказа
```bash
curl -X DELETE "http://localhost:8080/orders/1" \
-H "Authorization: Bearer <access_token>"
```
#### Возможные ответы:
- Успех (`200`)
```json
{ "message": "Order deleted" }
```
- заказ не найден (`404`)
