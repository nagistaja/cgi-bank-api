# Bank Account Service

## Project Overview

The Bank Account Service is a RESTful API providing functionality for managing bank accounts with multiple currency balances (EUR, USD, SEK, RUB). It allows users to create accounts, deposit, withdraw, view balances, and exchange money between currencies using fixed, configured rates. The service uses optimistic locking for concurrency control and notifies an external system on deposits.

This service is designed as a standalone Spring Boot application.

## Technology Stack

* **Language:** Java 17
* **Framework:** Spring Boot 3.4.4
    * Spring Web (MVC)
    * Spring Data JPA
    * Spring Boot Starter Validation
    * Spring Security (Basic setup)
    * Spring Boot Starter Actuator
    * Spring Webflux (for WebClient)
    * Spring Boot Starter Test
* **Database:** PostgreSQL (Default/Prod/Testcontainers), H2 (Dev profile)
* **Migration:** Flyway
* **Build Tool:** Gradle 8.13+
* **Testing:** JUnit 5, Mockito, AssertJ, Testcontainers
* **Containerization:** Docker
* **API Documentation:** SpringDoc OpenAPI (Swagger UI)
* **Utilities:** Lombok, MapStruct, Resilience4j, Logstash Logback Encoder, Checkstyle, SpotBugs

## Setup and Running Locally

### Prerequisites

* JDK 17+ installed.
* Gradle 8.13+ installed (or use the included Gradle wrapper `./gradlew`).
* PostgreSQL server running and accessible (required for default run).
* Docker installed (optional, required for running integration tests).

### Setup

1.  **Clone the Repository:** (If you haven't already)
    ```bash
    git clone [https://github.com/your-repository/bank-rest-api.git](https://github.com/your-repository/bank-rest-api.git)
    cd bank-rest-api/account-service
    ```

2.  **Configure Database:**
    * Edit `src/main/resources/application.yml` to point to your local PostgreSQL instance:
        ```yaml
        spring:
          datasource:
            url: jdbc:postgresql://localhost:5432/accounts_db # Adjust host/port/db if needed
            username: your_postgres_user
            password: your_postgres_password
        ```
    * Alternatively, set environment variables: `DB_USERNAME=your_username`, `DB_PASSWORD=your_password`.

### Running

* **With External PostgreSQL (Default Profile):**
    Ensure your PostgreSQL server is running and configured as above.
    ```bash
    ./gradlew bootRun
    ```
    The service will start, run Flyway migrations, and connect to your PostgreSQL database.

* **With In-Memory H2 (Dev Profile):**
    This avoids the need for an external PostgreSQL database, ideal for quick development or testing.
    ```bash
    ./gradlew bootRun --args='--spring.profiles.active=dev'
    ```
    The H2 console will be available at `http://localhost:8080/h2-console` (use JDBC URL `jdbc:h2:mem:testdb`, user `sa`, password `password`).

The service will be available at `http://localhost:8080` by default.

## API Documentation

### Interactive Documentation (Swagger UI)

The best way to explore the API is using the integrated Swagger UI:

* **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **OpenAPI Spec:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Endpoints

#### 1. Create Account

* **Method**: `POST`
* **Path**: `/api/v1/accounts`
* **Description**: Creates a new empty bank account.
* **Request Body**: None
* **Success Response**:
    * Code: `201 Created`
    * Headers: `Location: /api/v1/accounts/{newAccountId}`
    * Body: `AccountBalanceResponseDTO` (with new ID and empty balances list)
        ```json
        {
          "accountId": "f7e9a1b2-c3d4-5e6f-7a8b-9c0d1e2f3a4b",
          "balances": []
        }
        ```
* **Curl Example**:
    ```bash
    curl -i -X POST http://localhost:8080/api/v1/accounts
    ```

#### 2. Get Account Balances

* **Method**: `GET`
* **Path**: `/api/v1/accounts/{accountId}/balances`
* **Description**: Retrieves all currency balances for a specific account.
* **Success Response**:
    * Code: `200 OK`
    * Body: `AccountBalanceResponseDTO`
        ```json
        {
          "accountId": "acc123",
          "balances": [
            { "currency": "EUR", "amount": 100.00 },
            { "currency": "USD", "amount": 150.00 }
          ]
        }
        ```
* **Error Response**:
    * Code: `404 Not Found` (if `accountId` does not exist)
        ```json
        {
          "timestamp": "...", "status": 404, "error": "Account not found",
          "message": "Account not found with ID: acc123", "path": "..."
        }
        ```
* **Curl Example**:
    ```bash
    curl -X GET http://localhost:8080/api/v1/accounts/acc123/balances | jq .
    ```

#### 3. Deposit Funds

* **Method**: `POST`
* **Path**: `/api/v1/accounts/{accountId}/deposits`
* **Description**: Adds money to an account in a specific currency. Creates the balance if it doesn't exist.
* **Request Body**: `DepositRequestDTO`
    ```json
    {
      "amount": 50.00,
      "currency": "EUR"
    }
    ```
* **Success Response**:
    * Code: `200 OK`
    * Body: `AccountBalanceResponseDTO` (with updated balances)
* **Error Responses**:
    * `400 Bad Request`: Invalid currency, non-positive amount, validation errors.
    * `404 Not Found`: Account not found.
* **Curl Example**:
    ```bash
    curl -X POST http://localhost:8080/api/v1/accounts/acc123/deposits \
      -H "Content-Type: application/json" \
      -d '{"amount": 50.00, "currency": "EUR"}' | jq .
    ```

#### 4. Withdraw Funds

* **Method**: `POST`
* **Path**: `/api/v1/accounts/{accountId}/withdrawals`
* **Description**: Withdraws money from an account in a specific currency. No automatic currency exchange occurs.
* **Request Body**: `WithdrawRequestDTO`
    ```json
    {
      "amount": 30.00,
      "currency": "EUR"
    }
    ```
* **Success Response**:
    * Code: `200 OK`
    * Body: `AccountBalanceResponseDTO` (with updated balances)
* **Error Responses**:
    * `400 Bad Request`: Invalid currency, non-positive amount, validation errors, balance for currency not found.
    * `404 Not Found`: Account not found.
    * `422 Unprocessable Entity`: Insufficient funds.
    * `409 Conflict`: Optimistic locking failure (concurrent modification).
* **Curl Example**:
    ```bash
    curl -X POST http://localhost:8080/api/v1/accounts/acc123/withdrawals \
      -H "Content-Type: application/json" \
      -d '{"amount": 30.00, "currency": "EUR"}' | jq .
    ```

#### 5. Exchange Currency

* **Method**: `POST`
* **Path**: `/api/v1/accounts/{accountId}/exchanges`
* **Description**: Converts an amount from one currency balance to another within the same account, using configured fixed rates.
* **Request Body**: `ExchangeRequestDTO`
    ```json
    {
      "fromCurrency": "EUR",
      "toCurrency": "USD",
      "amount": 50.00
    }
    ```
* **Success Response**:
    * Code: `200 OK`
    * Body: `AccountBalanceResponseDTO` (with updated balances)
* **Error Responses**:
    * `400 Bad Request`: Invalid/unsupported currency pair, non-positive amount, same from/to currency, validation errors, balance for `fromCurrency` not found.
    * `404 Not Found`: Account not found.
    * `422 Unprocessable Entity`: Insufficient funds in `fromCurrency`.
    * `409 Conflict`: Optimistic locking failure (concurrent modification).
* **Curl Example**:
    ```bash
    curl -X POST http://localhost:8080/api/v1/accounts/acc123/exchanges \
      -H "Content-Type: application/json" \
      -d '{"fromCurrency": "EUR", "toCurrency": "USD", "amount": 50.00}' | jq .
    ```

#### 6. Get Transaction History

* **Method**: `GET`
* **Path**: `/api/v1/accounts/{accountId}/transactions`
* **Description**: Retrieves a paginated list of transaction records for an account, ordered by timestamp descending.
* **Query Parameters**:
    * `page` (optional, default: `0`): Page number (0-indexed).
    * `size` (optional, default: `20`): Number of transactions per page.
* **Success Response**:
    * Code: `200 OK`
    * Body: Spring Data `Page<TransactionResponseDTO>`
        ```json
        {
          "content": [
            { "id": 2, "accountId": "acc123", "type": "DEPOSIT", "amount": 50.00, "currency": "USD", "relatedCurrency": "", "timestamp": "...", "description": "Deposit of 50.00 USD" },
            { "id": 1, "accountId": "acc123", "type": "DEPOSIT", "amount": 100.00, "currency": "EUR", "relatedCurrency": "", "timestamp": "...", "description": "Deposit of 100.00 EUR" }
          ],
          "pageable": { "sort": { ... }, "offset": 0, "pageNumber": 0, "pageSize": 20, "paged": true, "unpaged": false },
          "totalPages": 1, "totalElements": 2, "last": true, "size": 20, "number": 0,
          "sort": { ... }, "numberOfElements": 2, "first": true, "empty": false
        }
        ```
* **Error Responses**:
    * `404 Not Found`: Account not found.
* **Curl Example**:
    ```bash
    curl -X GET "http://localhost:8080/api/v1/accounts/acc123/transactions?page=0&size=10" | jq .
    ```

## Configuration Details

Configuration is managed via `application.yml`, `application-dev.yml`, and `application-prod.yml`.

* **Profiles**:
    * `default`: Uses PostgreSQL (requires external DB), Flyway enabled, basic security.
    * `dev`: Uses H2 in-memory DB, Flyway disabled, H2 console enabled, Swagger UI enabled.
    * `prod`: Uses PostgreSQL (via env vars), Flyway enabled, more restrictive logging, actuator endpoints exposed for monitoring.
    * `test`, `test-unit`: Used during testing.
* **Key Properties (`application.yml`)**:
    * `spring.datasource.*`: Database connection details.
    * `spring.jpa.*`: JPA/Hibernate settings.
    * `spring.flyway.*`: Flyway migration settings.
    * `app.notification.url`: Endpoint for external deposit notifications.
    * `app.currency.supported`: List of valid currencies.
    * `app.exchange-rates.rates.*`: Fixed exchange rates (e.g., `EUR_USD: 1.08`).
    * `resilience4j.*`: Configuration for Circuit Breaker, Retry, TimeLimiter for the `notificationService`.
    * `logging.*`, `logback-spring.xml`: Logging configuration (structured JSON for prod/default).
* **Environment Variables (primarily for Docker/Prod)**:
    * `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: Database connection (override defaults in `application-prod.yml` or `docker-compose.yml`).
    * `SERVER_PORT`: Application port (default: `8080`).
    * `NOTIFICATION_URL`: Notification service endpoint.
    * `SPRING_PROFILES_ACTIVE`: Set active Spring profiles (e.g., `prod`).

## Testing

The project includes unit and integration tests. Integration tests require Docker to run Testcontainers (PostgreSQL).

* **Run Unit Tests Only (Fast):**
    ```bash
    ./gradlew test
    # or (explicitly excludes integration tests, suitable for CI)
    ./gradlew ciTests
    ```

* **Run Integration Tests Only:**
    ```bash
    ./gradlew integrationTest
    ```
    *(Note: These tests start a PostgreSQL container)*

* **Run All Tests (Unit + Integration):**
    ```bash
    ./gradlew check
    ```

* **Run Specific Test Class:**
    ```bash
    ./gradlew test --tests "com.cgi.bank.account.service.AccountServiceTest"
    # or for integration tests
    ./gradlew integrationTest --tests "com.cgi.bank.account.service.AccountServiceIntegrationTest"
    ```

* **Test Coverage Report:**
    Run `./gradlew check jacocoTestReport combinedJacocoReport`. Reports are generated in `build/reports/jacoco/`. Coverage thresholds are defined in `gradle/scripts/testing.gradle`.

### Test Structure & Practices

* Unit tests (`*Test`) use Mockito for isolation.
* Integration tests (`*IntegrationTest`) use Testcontainers (`AbstractIntegrationTest`) and `@SpringBootTest`.
* Tests are organized using JUnit 5 `@Nested` and `@DisplayName`.
* Separate Spring profiles (`test-unit`, `test`) are used.
* `@Transactional` is used for integration test isolation.
* `@MockitoBean` is used in integration tests to mock external dependencies like `NotificationClient`.

## Design Decisions, Assumptions, and Limitations

* **Single Deployable Unit**: The service is currently a single module but structured for potential future decomposition.
* **Fixed Exchange Rates**: Exchange rates are read from configuration (`application.yml` or environment variables) at startup and are not dynamic.
* **Optimistic Locking**: JPA's `@Version` is used on `Account` and `Balance` entities to prevent lost updates during concurrent operations. Conflicting updates result in an HTTP `409 Conflict`.
* **Asynchronous Notifications**: Deposit notifications (`NotificationClient`) are sent asynchronously using Spring's `@Async` and a dedicated thread pool to avoid blocking the main request thread.
* **Notification Resilience**: Resilience4j patterns (Circuit Breaker, Retry, Timeout) are applied to the asynchronous notification call for robustness.
* **Simulated Notification Endpoint**: The default notification URL (`https://httpstat.us/`) simulates an external call but doesn't represent a real notification system. This URL should be configured appropriately for different environments.
* **Basic Security**: Spring Security is enabled, but endpoints are largely permitted for simplicity in this exercise. A real application would require proper authentication and authorization.
* **Transaction Logging**: All deposit, withdrawal, and exchange operations create immutable `Transaction` records for auditing purposes.
* **No Automatic Currency Conversion on Debit**: Withdrawals (`Debit Money`) strictly operate on the specified currency balance, as required.

## Docker Build and Run (Standalone Service)

While Docker Compose (see root README) is recommended, you can build and run the service container standalone if you manage the database separately.

1.  **Build the Image:**
    From the `account-service` directory:
    ```bash
    docker build -t your-repo/account-service:latest .
    ```

2.  **Run the Container:**
    You **must** provide the database connection details via environment variables.
    ```bash
    docker run -p 8080:8080 --name bank-app \
      -e SPRING_PROFILES_ACTIVE=prod \
      -e DB_URL=jdbc:postgresql://your-external-db-host:5432/your_db_name \
      -e DB_USERNAME=your_db_user \
      -e DB_PASSWORD=your_db_password \
      -e NOTIFICATION_URL=https://your-real-notification-service/notify \
      your-repo/account-service:latest
    ```
    *(Ensure the container can reach your database host).*
