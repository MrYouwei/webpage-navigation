# Webpage Navigation

The project is split into two independent projects:

- `frontend/`: static HTML/CSS/JavaScript frontend. No build step is required.
- `backend/`: Spring Boot REST API with Shiro session authentication, MyBatis-Plus, and MySQL.

## Run Backend

1. Create MySQL tables with `backend/src/main/resources/db/schema.sql`.
2. Update `backend/src/main/resources/application.yml` database credentials.
3. Start:

```bash
cd backend
mvn spring-boot:run
```

Backend API base URL: `http://localhost:8080`.

## Run Frontend

Serve `frontend/index.html` from a static server, for example:

```bash
cd frontend
node server.js
```

Then open `http://127.0.0.1:5500`.

If the backend address changes, define it before loading the page:

```html
<script>
  window.NAV_API_BASE_URL = 'http://localhost:8080';
</script>
```

The frontend sends requests with cookies enabled, so the frontend origin must be listed under `app.cors.allowed-origins` in `backend/src/main/resources/application.yml`.
