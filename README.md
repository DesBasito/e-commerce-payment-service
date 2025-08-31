# Payment Service

## 📋 Описание

Payment Service - это микросервис для обработки платежей в e-commerce системе. Сервис обрабатывает платежные запросы от Order Service и отправляет уведомления о статусе платежей через Kafka.

## 🎯 Основные функции

- Обработка платежных запросов
- Сохранение информации о платежах
- Отправка уведомлений о статусе платежа
- Поддержка различных методов оплаты
- Аудит платежных операций

## ⚙️ Технический стек

- **Java 17**
- **Spring Boot 3.5.3**
- **Spring Data JPA**
- **Spring Cloud Config Client**
- **Spring Cloud Netflix Eureka Client**
- **Spring Kafka** (для уведомлений)
- **PostgreSQL**
- **Lombok**
- **Maven**

## 🗄️ Модель данных

### Payment Entity
```java
@Entity
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue
    private Integer id;
    
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    
    private Integer orderId;
    
    @CreatedDate
    private LocalDateTime createdDate;
    
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
}
```

### PaymentMethod Enum
```java
public enum PaymentMethod {
    PAYPAL,
    CREDIT_CARD,
    VISA,
    MASTER_CARD,
    BITCOIN
}
```

## 🚀 Запуск сервиса

### Предварительные условия
- Config Server (http://localhost:8888)
- Discovery Service (http://localhost:8761)
- PostgreSQL (localhost:5555)
- Kafka (для отправки уведомлений)
- Java 17+
- Maven 3.6+

### Локальный запуск

```bash
# Клонирование репозитория
git clone <repository-url>
cd services/payment

# Сборка проекта
./mvnw clean install

# Запуск сервиса
./mvnw spring-boot:run
```

## 🔧 Конфигурация

### application.yml
```yaml
spring:
  application:
    name: payment-service
```

### payment-service.yml (в Config Server)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5555/payment
    username: qwe
    password: qwe
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    database: postgresql
    database-platform: org.hibernate.dialect.PostgreSQLDialect

server:
  port: 8060
```

## 🌐 API Endpoints

### Создание платежа
```http
POST /api/v1/payments
Content-Type: application/json

{
  "amount": 999.99,
  "paymentMethod": "CREDIT_CARD",
  "orderId": 15,
  "orderReference": "ORD-2024-001",
  "customer": {
    "id": "507f1f77bcf86cd799439011",
    "firstname": "Иван",
    "lastname": "Иванов",
    "email": "ivan@example.com"
  }
}
```

**Ответ:**
```json
25
```

## 🏗️ Архитектура

### Структура пакетов
```
kg.manurov.ecommerce/
├── configs/
│   └── KafkaPaymentTopicConfig.java
├── controllers/
│   └── PaymentController.java
├── dto/
│   ├── Customer.java
│   └── PaymentRequest.java
├── mapper/
│   └── PaymentMapper.java
├── model/
│   ├── Payment.java
│   └── PaymentMethod.java
├── notification/
│   ├── NotificationProducer.java
│   └── PaymentNotificationRequest.java
├── repositories/
│   └── PaymentRepository.java
├── services/
│   └── PaymentService.java
└── PaymentApplication.java
```

### Основные компоненты

#### PaymentController
Обрабатывает HTTP запросы для создания платежей.

#### PaymentService
Содержит бизнес-логику:
- Обработка платежных запросов
- Сохранение платежей в базе данных
- Отправка уведомлений через Kafka

#### PaymentRepository
JPA репозиторий для работы с PostgreSQL.

#### NotificationProducer
Kafka producer для отправки уведомлений о платежах.

## 💳 Бизнес-логика обработки платежей

### Алгоритм обработки платежа:

1. **Валидация запроса** - проверка корректности данных
2. **Создание платежа** - сохранение в базе данных
3. **Отправка уведомления** - отправка события в Kafka

### Код PaymentService.createPayment():
```java
public Integer createPayment(PaymentRequest request) {
    // 1. Создание и сохранение платежа
    Payment payment = this.repository.save(this.mapper.toPayment(request));
    
    // 2. Отправка уведомления
    this.notificationProducer.sendNotification(
            new PaymentNotificationRequest(
                    request.orderReference(),
                    request.amount(),
                    request.paymentMethod(),
                    request.customer().firstname(),
                    request.customer().lastname(),
                    request.customer().email()
            )
    );
    
    return payment.getId();
}
```

## 📨 Kafka Integration

### NotificationProducer
```java
@Service
public class NotificationProducer {
    
    private final KafkaTemplate<String, PaymentNotificationRequest> kafkaTemplate;
    
    public void sendNotification(PaymentNotificationRequest request) {
        Message<PaymentNotificationRequest> message = MessageBuilder
                .withPayload(request)
                .setHeader(TOPIC, "payment-topic")
                .build();
        
        kafkaTemplate.send(message);
    }
}
```

### PaymentNotificationRequest Event
```java
public record PaymentNotificationRequest(
    String orderReference,
    BigDecimal amount,
    PaymentMethod paymentMethod,
    String customerFirstname,
    String customerLastname,
    String customerEmail
) {}
```

### Kafka Topic Configuration
```java
@Configuration
public class KafkaPaymentTopicConfig {
    
    @Bean
    public NewTopic paymentTopic() {
        return TopicBuilder
                .name("payment-topic")
                .build();
    }
}
```

## 🔍 Валидация данных

### PaymentRequest
- `amount` - обязательное поле, должно быть положительным числом
- `paymentMethod` - обязательное поле
- `orderId` - обязательное поле
- `orderReference` - обязательное поле
- `customer` - обязательные данные клиента для уведомления

### Customer DTO
- `firstname` - обязательное поле
- `lastname` - обязательное поле
- `email` - обязательное поле, должно соответствовать формату email

### Примеры ошибок валидации
```json
{
  "title": "Ошибка валидации",
  "errors": {
    "amount": ["Amount is required"],
    "customer.email": ["The customer email is not correctly formatted"]
  }
}
```

## 📊 База данных

### Схема таблицы payment
```sql
CREATE TABLE payment (
    id SERIAL PRIMARY KEY,
    amount DECIMAL(10,2),
    payment_method VARCHAR(20),
    order_id INTEGER,
    created_date TIMESTAMP,
    last_modified_date TIMESTAMP
);

CREATE INDEX idx_payment_order_id ON payment(order_id);
CREATE INDEX idx_payment_created_date ON payment(created_date);
```

## 📊 Мониторинг

### Health Check
```http
GET /actuator/health
```

### JPA Health Check
Spring Boot автоматически проверяет подключение к PostgreSQL:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    }
  }
}
```

### Kafka Health Check
```json
{
  "status": "UP",
  "components": {
    "kafka": {
      "status": "UP"
    }
  }
}
```

## 🔗 Интеграция с другими сервисами

### Вызывается Order Service
Order Service создает платежи через HTTP запросы:

```java
// В Order Service
PaymentRequest paymentRequest = new PaymentRequest(
    request.amount(),
    request.paymentMethod(),
    order.getId(),
    order.getReference(),
    customer
);
paymentClient.requestOrderPayment(paymentRequest);
```

### Уведомления для Notification Service
Payment Service отправляет события в Kafka топик `payment-topic`, которые обрабатывает Notification Service для отправки email уведомлений.

## 🐛 Устранение неполадок

### Частые проблемы

1. **PostgreSQL недоступен**
    - Убедитесь, что PostgreSQL запущен на порту 5555
    - Проверьте существование базы данных 'payment'
    - Проверьте учетные данные (qwe/qwe)

2. **Kafka недоступен**
    - Убедитесь, что Kafka запущен
    - Проверьте конфигурацию топика 'payment-topic'

3. **Сервис не регистрируется в Eureka**
    - Убедитесь, что Discovery Service запущен
    - Проверьте конфигурацию eureka в Config Server

4. **Ошибки при отправке уведомлений**
    - Проверьте подключение к Kafka
    - Проверьте формат PaymentNotificationRequest

## 📁 Структура проекта

```
payment/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── kg/manurov/ecommerce/
│   │   │       ├── configs/
│   │   │       │   └── KafkaPaymentTopicConfig.java
│   │   │       ├── controllers/
│   │   │       │   └── PaymentController.java
│   │   │       ├── dto/
│   │   │       │   ├── Customer.java
│   │   │       │   └── PaymentRequest.java
│   │   │       ├── mapper/
│   │   │       │   └── PaymentMapper.java
│   │   │       ├── model/
│   │   │       │   ├── Payment.java
│   │   │       │   └── PaymentMethod.java
│   │   │       ├── notification/
│   │   │       │   ├── NotificationProducer.java
│   │   │       │   └── PaymentNotificationRequest.java
│   │   │       ├── repositories/
│   │   │       │   └── PaymentRepository.java
│   │   │       ├── services/
│   │   │       │   └── PaymentService.java
│   │   │       └── PaymentApplication.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
├── target/
├── pom.xml
└── README.md
```