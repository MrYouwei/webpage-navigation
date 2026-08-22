# Webpage Navigation Frontend

Static frontend for the webpage navigation app.

## Configuration

Default API base URL:

```js
http://localhost:8080
```

Override it before the main script runs:

```html
<script>
  window.NAV_API_BASE_URL = 'http://localhost:8080';
</script>
```

## Local Run

```bash
node server.js
```

Open `http://127.0.0.1:5500`.

## 杀死所有之前开启未关闭的进程

```bash
taskkill /f /im node.exe
or
Get-Process node -ErrorAction SilentlyContinue | Stop-Process -Force
```

##  项目启动 dev

```bash
npm run dev
```
##  项目启动 preview

```bash
npm run preview -- --port 8382 --host
```