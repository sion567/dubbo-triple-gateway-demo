# 前端（Vite + Vue3 + Element Plus）

登录 + 用户/订单/库存管理的演示前端，与后端（gateway:7788）联动，
完整链路：浏览器 → gateway(Spring Security 认证) → Dubbo tri rest → @PreAuthorize 鉴权。

## 启动

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173，/api 自动代理到 http://localhost:7788
```

账号：`user/123456`（普通用户，只能看订单+下单）；`admin/admin123`
（管理员，额外可见用户管理、库存管理，可改单/删单/补货）。

## 功能对照

| 页面 | 后端端点 | 权限 |
|---|---|---|
| 登录 | POST /user/login | 白名单 |
| 订单列表 | GET /order/list | 登录（ROLE_USER） |
| 下单 | POST /order/createOrder | hasRole('USER')，联动 Seata（开关可触发全局回滚） |
| 完成/取消/删除订单 | POST /order/updateStatus、DELETE /order/delete/{id} | hasRole('ADMIN') |
| 用户列表 | GET /user/list | hasRole('ADMIN') |
| 库存列表/补货/删除 | GET /storage/list、POST /storage/save、DELETE /storage/delete/{code} | save/delete 需 ROLE_ADMIN |

## 技术要点

- token 存 localStorage（Pinia auth store），axios 请求拦截器统一加 `Authorization: Bearer`
- 401 → 自动跳登录页；403 → Element Plus 提示（403 语义由后端 AccessDeniedStatusFilter 保证）
- 路由守卫：未登录跳 /login；`meta.admin` 页面非管理员不可进
- 管理员菜单动态隐藏（v-if="auth.isAdmin"）
- 开发用 Vite proxy 免跨域；生产可 `npm run build` 后把 dist 交给 nginx/网关静态资源

## 生产构建

```bash
npm run build      # 产物在 frontend/dist
```
