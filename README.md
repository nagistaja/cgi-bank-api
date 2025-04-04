# Bank REST API

## Overview

This project provides a RESTful API for managing simple bank accounts with multi-currency support. It allows users to:

* Create new accounts.
* Deposit funds into specific currency balances.
* Withdraw funds from specific currency balances (no automatic exchange).
* View all currency balances for an account.
* Perform currency exchanges between supported currencies (EUR, USD, SEK, RUB) using fixed, configured exchange rates.

The initial implementation is a modular monolith (`account-service`) built using Java 17 and Spring Boot 3.x, designed with microservice principles in mind. It includes features like optimistic locking for concurrency control and asynchronous external notifications.

## Key Features

* **Multi-Currency Accounts**: Hold balances in EUR, USD, SEK, RUB simultaneously.
* **Core Banking Operations**: Deposit, Withdraw, Get Balance.
* **Currency Exchange**: Convert between supported currencies at fixed rates.
* **Transaction History**: Records all operations (persisted but retrieval endpoint needs review).
* **Persistence**: Uses PostgreSQL database with Flyway migrations.
* **External Call Simulation**: Notifies an external endpoint (configurable, defaults to `https://httpstat.us/`) on deposits.
* **Containerized**: Easily run using Docker Compose.

## Technology Stack

* **Language:** Java 17
* **Framework:** Spring Boot 3.x (Web, Data JPA, Validation, Actuator, Webflux, Security)
* **Database:** PostgreSQL
* **Migration:** Flyway
* **Build Tool:** Gradle 8.x
* **Testing:** JUnit 5, Mockito, AssertJ, Testcontainers
* **Containerization:** Docker, Docker Compose
* **API Documentation:** SpringDoc OpenAPI (Swagger UI)
* **Utilities:** Lombok, MapStruct, Resilience4j, Logstash Logback Encoder

## Running the Application (Docker Compose)

This is the recommended way to run the application and its database dependency.

**Prerequisites:**
* Docker Desktop (or Docker Engine + Docker Compose) installed.

**Steps:**

1.  **Build the Application Image:**
    Open a terminal in the project root directory (`bank-rest-api`) and run:
    ```bash
    docker-compose build
    ```
    This command builds the Docker image for the `account-service`.

2.  **Start the Services:**
    Once the build is complete, start the containers:
    ```bash
    docker-compose up -d
    ```
    * This starts the `bank_db` (PostgreSQL) and `bank_account_service` containers in detached mode.
    * The application service waits for the database to be healthy before starting.
    * Flyway migrations will run automatically on application startup.
    * The API will be available at `http://localhost:8080`.

3.  **View Logs (Optional):**
    ```bash
    docker-compose logs -f account-service
    # or
    docker-compose logs -f db
    ```

4.  **Stop the Services:**
    When finished, stop and remove the containers, network, and the database data volume:
    ```bash
    docker-compose down -v
    ```
    *(Use `docker-compose down` without `-v` to keep the database data for the next run).*

## Interacting with the API

### API Documentation (Swagger UI)

The most convenient way to explore and interact with the API is via the integrated Swagger UI:

* **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **OpenAPI Spec:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Curl Examples

You can also use `curl` to interact with the API. The `| jq .` part formats the JSON response; remove it if you don't have `jq` installed.

```bash
# Example: Create a new account
echo "--- Creating a new account ---"
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json")
echo $RESPONSE | jq .
echo "\n"

# Extract the account ID from the response
NEW_ACCOUNT_ID=$(echo $RESPONSE | jq -r '.accountId')
echo "New Account ID: $NEW_ACCOUNT_ID"
echo "\n"

# Example: Deposit 500 EUR to the new account
echo "--- Depositing 500 EUR to new account ---"
curl -s -X POST "http://localhost:8080/api/v1/accounts/$NEW_ACCOUNT_ID/deposits" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500.00,
    "currency": "EUR"
  }' | jq .
echo "\n"

# Example: Check balances
echo "--- Checking balances ---"
curl -s -X GET "http://localhost:8080/api/v1/accounts/$NEW_ACCOUNT_ID/balances" | jq .
echo "\n"

# Example: Exchange 100 EUR to USD (Rate EUR_USD: 1.08)
echo "--- Exchanging 100 EUR to USD ---"
curl -s -X POST "http://localhost:8080/api/v1/accounts/$NEW_ACCOUNT_ID/exchanges" \
  -H "Content-Type: application/json" \
  -d '{
    "fromCurrency": "EUR",
    "toCurrency": "USD",
    "amount": 100.00
  }' | jq .
echo "\n"

# Example: Check final balances
echo "--- Checking final balances ---"
curl -s -X GET "http://localhost:8080/api/v1/accounts/$NEW_ACCOUNT_ID/balances" | jq .
echo "\n"

# Example: Check application health
echo "--- Checking application health ---"
curl -s http://localhost:8080/actuator/health | jq .
echo "\n"
```

## Testing

To run the tests for the `account-service`:

1.  Navigate to the service directory: `cd account-service`
2.  Run the desired test task (requires Docker for integration tests):
    * Run unit tests only (fast, no external dependencies): `./gradlew test` or `./gradlew ciTests`
    * Run unit and integration tests: `./gradlew check`

See the `account-service/README.md` for more detailed testing instructions.

## Project Structure

* `./docker-compose.yml`: Defines the multi-container Docker environment.
* `./account-service/`: Contains the source code, build scripts, tests, and Dockerfile for the main application.
    * `./account-service/README.md`: Detailed documentation for the account service module.

## Further Information

For more detailed technical documentation, API endpoint specifics, local development setup, and design decisions, please refer to the [Account Service README](./account-service/README.md).

## Additional Notes

### Assumptions

* **Deployment Environment:** While containerized, the basic security configuration assumes deployment within a trusted network or behind an API gateway/load balancer responsible for handling authentication and potentially TLS termination.
* **Database Reliability:** Relies on the ACID properties and reliability of the configured PostgreSQL database for data consistency within transactions.

### Design Decisions

* **Architecture:** Implemented as a single, deployable Spring Boot application (`account-service`), following a modular monolith pattern. Designed with separation of concerns (controller, service, repository, domain) to align with microservice principles for potential future decomposition.
* **Persistence:** Uses Spring Data JPA with Hibernate as the ORM. Flyway manages database schema migrations. PostgreSQL is the primary target database, with H2 supported for the `dev` profile.
* **Concurrency Control:** Optimistic Locking (`@Version` field on `Account` and `Balance` entities) is used to handle concurrent updates. This approach was chosen over pessimistic locking to generally favor higher throughput, assuming conflicts are relatively infrequent. Conflicts result in an HTTP `409 Conflict` response, requiring the client to retry the operation.
* **Multi-Currency Handling:** Each account holds balances in a `Map<Currency, Balance>`, mapping directly to rows in the `balances` table via a one-to-many relationship, ensuring a unique balance record per currency per account.
* **API Design:**
    * RESTful endpoints with clear resource paths (`/accounts/{id}/balances`, etc.).
    * Data Transfer Objects (DTOs) used for API request/response contracts, separating API representation from domain models. MapStruct is used for mapping.
    * Centralized exception handling (`GlobalExceptionHandler`) translates service-layer exceptions into standardized `ErrorResponseDTO` responses with appropriate HTTP status codes.
    * API documentation provided via SpringDoc OpenAPI and Swagger UI.
* **External Calls (Notifications):**
    * Calls to the notification service are executed asynchronously (`@Async`) on a separate thread pool (`taskExecutor`) to avoid blocking API request threads.
    * Resilience4j patterns (Circuit Breaker, Retry, Timeout) are applied to the `NotificationClient` to improve robustness against external service unreliability or latency. Fallbacks currently log failures.
* **Transaction Auditing:** A `Transaction` entity is created and persisted for every significant operation (Deposit, Withdraw, Exchange From/To), providing an audit trail.
* **Code Quality & Maintainability:**
    * Gradle build includes Checkstyle and SpotBugs for static analysis.
    * Lombok reduces boilerplate code.
    * Structured JSON logging (via Logback/Logstash encoder) is configured for easier log aggregation and analysis.
    * Request tracing (`RequestTracingFilter`) adds unique IDs (`traceId`, `requestId`) to logs via MDC for correlating messages belonging to a single request.
* **Testing Strategy:** Comprehensive approach including:
    * Unit tests with Mockito for component isolation.
    * Integration tests using Testcontainers (`@SpringBootTest`) for realistic testing against a PostgreSQL database.
    * Jacoco for code coverage reporting and threshold enforcement.

### Limitations

* **Fixed Exchange Rates:** Exchange rates are loaded from configuration at startup and are not dynamic. Updating rates requires a configuration change and application restart/refresh.
* **Basic Security:** The current Spring Security configuration is minimal, primarily allowing access to API endpoints and securing actuator endpoints partially. It lacks robust authentication (e.g., JWT, OAuth2) and authorization mechanisms suitable for production exposure.
* **Notification Simulation:** The default notification mechanism calls `https://httpstat.us/` for demonstration/testing only. The fallback for notification failures only logs the error.
* **No Real-time Updates:** The API is purely request-response based. Clients needing real-time balance updates would need to poll the API; no event streaming or WebSocket support is implemented.
* **Transaction History Filtering:** The `GET /transactions` endpoint provides basic pagination but currently lacks filtering capabilities (e.g., by date range, transaction type).
* **Error Reporting:** While standard HTTP status codes are used, the system does not employ more granular application-specific error codes within the response body, which might be beneficial in complex integrations.