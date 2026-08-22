---
name: "webpage-navigation-conventions"
description: "网页导航项目的通用规范与约定，包括页面样式、项目结构、导航模块规则等。当开发此项目或修改相关组件时调用。"
---

# 网页导航项目 - 通用规范

本技能记录了网页导航项目的核心规范，包括页面设计风格、技术栈、目录结构、导航模块规则等。开发时务必遵循。

---

## 一、技术栈

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 前端框架 | Vue 3 | 3.x | 组合式 API + `<script setup>` |
| 构建工具 | Vite | 5.x | 开发服务器 + 打包 |
| UI 组件库 | Element Plus | 2.x | 全局注册组件 |
| 状态管理 | Pinia | 2.x | 全局状态管理 |
| 路由 | Vue Router | 4.x | 支持 history 模式 |
| 后端框架 | Spring Boot | 3.x | Java 17 |
| 安全框架 | Apache Shiro | latest | Session 认证 |

---

## 二、页面设计规范

### 2.1 主题色

- **主色调**：蓝紫色渐变（`#667eea` → `#764ba2`）
- **辅助色**：粉紫色（`#f093fb`）
- **背景色**：深蓝紫（`#1a1a2e` / `#0f0f1a`）
- **文字颜色**：白色 / 浅灰色（`#ffffff` / `#b0b0b0`）

### 2.2 页面布局

- **首页**：全宽布局（无左侧栏和内容区分栏）
- **导航模块/我的/广场**：左右分栏布局
  - 左侧：导航树（二级分类树）
  - 右侧：内容区

### 2.3 动画效果

- **背景光晕**：3 个模糊光晕 blob，18 秒缓动漂浮
- **光点粒子**：28 个随机光点，漂浮 + 脉冲双动画
- **卡片 stagger 入场**：每张卡片 60ms 延迟，弹性 scale 动画
- **打字机效果**：逐字显示 + 光标闪烁
- **自动轮播**：7 秒切换，支持手动点击

### 2.4 响应式断点

| 断点 | 宽度 | 布局变化 |
|------|------|----------|
| 大屏 | ≥1200px | Hero 左右分栏 |
| 中屏 | 960-1200px | 部分堆叠 |
| 小屏 | <960px | 全部堆叠 |

---

## 三、项目目录结构

```
webpage-navigation/
├── backend/                    # Spring Boot 后端
│   ├── src/
│   │   └── main/
│   │       ├── java/           # Java 源码
│   │       └── resources/
│   │           └── application.yml
│   └── pom.xml
├── frontend/                   # Vue 前端
│   ├── src/
│   │   ├── api/                # API 请求封装
│   │   ├── components/         # Vue 组件
│   │   ├── stores/             # Pinia 状态管理
│   │   ├── App.vue             # 根组件
│   │   └── main.js
│   ├── backup/                 # 旧版本备份
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js
│   └── .env.production
└── doc/                        # 项目文档
```

### 关键文件说明

- [vite.config.js](file:///E:/TanwbWorkSpace/Project/webpage-navigation/frontend/vite.config.js)：Vite 配置（base 路径、代理、端口）
- [application.yml](file:///E:/TanwbWorkSpace/Project/webpage-navigation/backend/src/main/resources/application.yml)：后端配置（端口、CORS、数据源）

---

## 四、导航模块规则

### 4.1 导航树结构

```
二级分类树（左侧栏）
├── AI
│   ├── AI 绘画
│   └── AI 写作
├── 生活
│   ├── 美食
│   └── 旅游
├── 影视
│   ├── 电影
│   └── 电视剧
└── 搜索
    ├── 综合搜索
    └── 学术搜索
```

### 4.2 数据规则

- **导航数据**：使用公共推荐数据（非用户书签）
- **滚动锚点**：点击左侧树节点，右侧内容区平滑滚动
- **分块显示**：每个分类显示为文字链接块，而非卡片

### 4.3 状态管理

- **activeModule**：当前选中的主模块（首页/我的/导航/广场）
- **navScrollAnchor**：导航模块的滚动锚点
- **isLoggedIn**：登录状态

状态存储在 Pinia store 中，在 [stores/nav.js](file:///E:/TanwbWorkSpace/Project/webpage-navigation/frontend/src/stores/nav.js) 定义。

---

## 五、首页设计规范

### 5.1 区块结构

```
首页
├── Hero 区（左右分栏）
│   ├── 左侧：标题 + 渐变字 + 信任数据
│   └── 右侧：AI 搜索面板 Mockup
├── 核心能力卡片（4 张）
├── 精选推荐站点（12 个卡片）
├── Showcase 场景预览
└── 底部 CTA 卡片
```

### 5.2 Hero 标题规范

```html
<section class="home-hero ai-hero">
  <!-- 背景装饰：光晕 + 光点 + 扫光带 -->
  <div class="hero-bg">
    <div class="glow-blob glow-blob-1"></div>
    <span v-for="p in heroParticles" class="hero-dot" :style="..."></span>
    <div class="hero-shimmer"></div>
  </div>
  
  <!-- 主内容 -->
  <div class="hero-grid">
    <div class="hero-copy">...</div>
    <div class="hero-visual">...</div>
  </div>
</section>
```

### 5.3 卡片通用样式

```css
.card {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  transition: transform 0.3s ease;
}

.card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 40px rgba(102, 126, 234, 0.3);
}
```

---

## 六、环境变量配置

### 开发环境 (.env.development)

```
VITE_API_BASE_URL=http://localhost:8381
```

Vite 代理会将 `/api` 请求转发到 `http://localhost:8080`（后端本地）。

### 生产环境 (.env.production)

```
VITE_API_BASE_URL=https://api-proxy-xxx.workers.dev
```

通过 Cloudflare Worker 代理到 AlwaysData 后端（HTTP）。

---

## 七、编码约定

### 组件命名

- **大驼峰**：`NavTreeNode.vue`, `ContentArea.vue`, `Sidebar.vue`
- **文件位置**：`src/components/`

### 组件结构

```vue
<script setup>
// 1. 导入依赖
// 2. 定义 props / emits
// 3. 定义响应式数据
// 4. 定义计算属性
// 5. 定义方法
// 6. 生命周期钩子
</script>

<template>
  <!-- 模板内容 -->
</template>

<style scoped>
/* 组件样式 */
</style>
```

### API 请求封装

所有 API 请求通过 `src/api/` 目录下的封装函数调用，使用 axios。

---

## 八、部署流程

### 本地开发

```bash
# 终端 1：后端
cd backend && mvn spring-boot:run

# 终端 2：前端
cd frontend && npm run dev
```

### 生产部署

```bash
# 1. 前端 build
cd frontend && npm run build

# 2. 推到 GitHub Pages (gh-pages 分支)
# 参考 doc/常见问题与解决方案.md 第二部分

# 3. Cloudflare Worker 代理 HTTPS
# 参考 doc/常见问题与解决方案.md 第三部分
```

---

## 九、常见问题索引

详细问题及解决方案请参考 [doc/常见问题与解决方案.md](file:///E:/TanwbWorkSpace/Project/webpage-navigation/doc/常见问题与解决方案.md)。
