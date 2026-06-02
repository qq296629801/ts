# 表现层：顶栏毛玻璃与全站背景

**归属**：`001-ai-image-platform` | **实现**：`frontend/web/src/App.vue`

> 无持久化数据变更；供实现与验收对照。

## 顶栏 Token（`app-header--glass`）

| Token | 含义 | 默认值 |
|-------|------|--------|
| `header-bg-glass` | 半透明底色 | `rgba(26, 26, 46, 0.72)` |
| `header-bg-fallback` | 无 `backdrop-filter` 降级 | `rgba(26, 26, 46, 0.92)` |
| `header-blur` | 模糊半径 | `14px` |
| `header-border` | 底部分隔 | `1px solid rgba(255,255,255,0.08)` |
| `header-z` | 层级 | `1000` |
| `nav-text` / `nav-text-active` | 导航字色 | `rgba(255,255,255,0.75)` / `#fff` |

**结构**：品牌区 + 主导航（登录后；游客见广场/作品展示/登录注册）+ `QuotaBadge`（登录后）；窄屏导航可横向滚动。

## 全站背景 Token（`app-page-bg`）

| Token | 含义 | 默认值 |
|-------|------|--------|
| `page-bg-color` | 占位底色 | `#0f0f1a` |
| `page-bg-image` | 装饰图 | `assets/images/app-bg.webp` |
| `page-bg-overlay` | `::after` 渐变遮罩 | 顶 `rgba(15,15,26,0.45)` → 底 `rgba(26,26,46,0.88)` |
| `page-bg-z` / `shell-z` / `header-z` | 层级 | `0` / `1` / `1000` |

**结构**：固定全屏背景 → `app-shell`（透明 `el-main`）→ 业务卡片半透明白底；移动端 `background-attachment: scroll`。

## 验收要点

| ID | 描述 |
|----|------|
| VR-H1 | 顶栏可见毛玻璃或 fallback，滚动仍 sticky |
| VR-H2 | 导航选中态可区分 |
| VR-B1 | 顶栏后方可见背景层次，正文可读 |
| VR-B2 | 主路由切换背景风格一致 |
