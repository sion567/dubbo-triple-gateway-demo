# 安全认证架构：JWT 即信任边界 + 网关标准 Spring Security + 统一上下文透传

## 设计原则（与"网关解密明文下发"方案的差异）

1. **信任边界是 JWT 本身，不是网关**：网关验签后把原始 `Authorization` 头原样透传，
   下游 Provider 各自验签。绕过网关直连下游也打不穿，不需要明文 X-User-Info，
   也不需要为防伪造补内部签名/mTLS（HMAC 验签是微秒级，重复验签的开销可忽略）。
2. **网关用标准 Spring Security WebFlux**（SecurityWebFilterChain），不手写 GlobalFilter：
   白名单、401 语义、未来接 OAuth2/OIDC 都在框架轨道上。
3. **身份和 Seata XID 合并为一对 Dubbo Filter 透传**：所有 RPC 上下文同走一条通道，
   服务间链式调用（order→user/storage）身份也不丢。

## 流程

```
客户端
  │ ① POST /user/login（精确白名单）
  ▼
gateway SecurityWebFilterChain
  │ ② JwtAuthenticationWebFilter 验签(HS256) → 失败 401 JSON
  │ ③ 通过 → Authentication 进 Reactor Context；Authorization 头原样放行
  ▼  HTTP/2（Triple RPC：Header 自动转 attachment；Triple REST：不转，
  │    Provider 从 invocation 的 tri.http.request 直接读 HTTP Header）
Dubbo Provider: ContextPropagationProviderFilter（同时处理多个来源）
  │ ④ attachment 里的原始 JWT 再验签 → SecurityContext(用户+角色+权限)
  │    token 存 RpcAuthHolder（供本服务调下游时继续透传）
  │ ⑤ XID 绑定 RootContext（Seata 分支事务）
  ▼
业务方法 @PreAuthorize("hasAuthority('system:dept:add')") 细粒度鉴权
  │
  ▼（order 调 user/storage 时）
ContextPropagationConsumerFilter：把 XID + token 塞进下一次 RPC 的 attachment
```

- 账号：`user/123456`（ROLE_USER，order:query/create）；`admin/admin123`（ROLE_ADMIN，
  含 `system:dept:add`、`system:dept:query`）
- 密钥：`JWT_SECRET` 环境变量覆盖（>= 32 字节），网关与下游共用同一密钥

## 关键代码

| 位置 | 职责 |
|---|---|
| `dubbo-common .../security/JwtUtil.java` | 签发/验签（jjwt 0.12，HS256，roles+perms） |
| `dubbo-gateway .../SecurityConfig.java` | 标准 SecurityWebFilterChain：白名单+401 |
| `dubbo-gateway .../JwtAuthenticationWebFilter.java` | 响应式验签，Authentication 进 Reactor Context |
| `dubbo-common .../security/ContextPropagationConsumerFilter.java` | 出站 RPC 透传 XID+token |
| `dubbo-common .../security/ContextPropagationProviderFilter.java` | 入站还原 XID+验签重建身份（finally 全量清理） |
| `dubbo-user-service UserServiceImpl#login` | 登录 + BCrypt + 发 token |
| `dubbo-order-service OrderServiceImpl` | `@PreAuthorize` 方法级权限（`@EnableMethodSecurity`） |

## @RequiresPermissions 说明

`@RequiresPermissions` 是 Shiro 注解（若依系）。Spring Security 等价写法：

| Shiro | Spring Security |
|---|---|
| `@RequiresPermissions("x")` | `@PreAuthorize("hasAuthority('x')")` |
| `@RequiresRoles("admin")` | `@PreAuthorize("hasRole('ADMIN')")` |

本工程在 `getAllOrders` 用 `@PreAuthorize("hasAuthority('system:dept:add')")`，
权限字符串保持若依风格。

## 验证

IDEA 打开根目录 `test.http` 依次执行（登录自动保存 token 变量）。
观察下游日志中 `🔑 [ContextPropagation] 认证通过: user=...` 确认透传链路生效。

## 生产化提醒

- 换 RS256（公钥下发、私钥只在签发方）后可做到"下游只验不签"，密钥管理更干净
- 异步线程（CompletableFuture/自建线程池）会丢 SecurityContext 和 RpcAuthHolder，
  需 `DelegatingSecurityContextExecutor` 包装
- 登录可拆独立 auth 服务；加 refresh token 与登出黑名单
