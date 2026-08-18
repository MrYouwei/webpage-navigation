# Webpage Navigation Backend

Spring Boot API for the separated webpage-navigation frontend.

## Requirements

- JDK 8+
- Maven 3.9+
- MySQL 8+

## Setup

1. Create the database and tables:

```sql
source src/main/resources/db/schema.sql;
```

2. Update `src/main/resources/application.yml` with your MySQL username and password.

3. Start the API:

```bash
mvn spring-boot:run
```

The API listens on `http://localhost:8080`.

## API

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /api/nav/data`
- `POST /api/nav/data`
- `PUT /api/nav/data`

启动命令
JAVA_VERSION=8 java -jar /home/tanyouwei/webpage-navigation/webpage-navigation-backend-0.0.1-SNAPSHOT.jar --server.address=:: --server.port=$PORT