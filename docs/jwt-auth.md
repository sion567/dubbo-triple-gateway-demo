# JWT 认证完整流程

## 流程

```
1. POST /user/login (user-service, 开放接口)
      │ BCrypt 校验密码 → 签发 JWT（HS256, sub=用户名, roles=角色, 2h 过期）
      ▼
2. 客户端携带 Authorization: Bearer <token> 访问 /order/**
      │
3. SecurityFilterV1 (dubbo-common, Dubbo tri rest RestExtension, 只拦 /order/*)
      │ 验签 + 过期校验（jjwt）→ 填充 SecurityContext（用户名 + 角色）
      │ 失败 → 401；成功 → 放行
      ▼
4. 业务方法鉴权（如 getAllOrders 检查 ROLE_ADMIN）→ 无权限返回 403
```

- 账号：`user/123456`（ROLE_USER）、`admin/admin123`（ROLE_ADMIN）
- 密钥：默认内置 sample 密钥，环境变量 `JWT_SECRET` 覆盖（>= 32 字节）
- 登录接口在 `/user/**` 下，过滤器 `getPatterns()` 只拦 `/order/*`，天然开放

## 关键代码

| 位置 | 职责 |
|---|---|
| `dubbo-common .../security/JwtUtil.java` | 签发/验签（jjwt 0.12，HS256） |
| `dubbo-common .../filter/SecurityFilterV1.java` | tri rest 过滤器：认证 + 401 |
| `dubbo-user-service UserServiceImpl#login` | 登录 + BCrypt 校验 + 发 token |
| `dubbo-order-service OrderServiceImpl#getAllOrders` | 鉴权示例（ROLE_ADMIN → 403） |

## 验证

IDEA 里打开根目录 `test.http` 依次执行：登录（自动把 token 存进变量）→ 无 token 401 →
伪造 token 401 → 普通用户 200 → 普通用户访问管理接口 403 → 管理员 200 → 带 token 下单（联动 Seata 示例）。