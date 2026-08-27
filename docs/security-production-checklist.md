# 安全生产化欠账清单

当前 sample 已具备：网关统一认证（标准 Spring Security WebFlux）、JWT(HS256) 签发/验签、
BCrypt 密码、roles+perms、`@PreAuthorize` 方法级鉴权（全服务）、403 异常映射、CORS、
XID+身份统一 RPC 透传。以下是从 sample 到生产还欠的账，按优先级排列。

## P0 上线前必须补

### 1. 密钥管理：HS256 → RS256/ES256
现状：网关与所有下游共享同一个 HMAC secret（`JWT_SECRET`），泄露面大、无法做到"下游只验不签"。
方案：签发方持私钥（KMS/配置中心下发），网关与下游只持公钥；换 `JwtUtil` 为非对称算法即可，
架构不用动（因为下游本来就各自验签，只换密钥形态）。

### 2. 登出 / token 吊销
现状：token 签发后 2 小时内无法作废，改密、踢人、员工离职都失效。
方案（按成本递增）：
- 缩短 access token TTL（如 15 分钟）+ refresh token 轮换
- Redis 黑名单（登出/改密时写入 jti，验签时查）
- 网关侧对高频接口做黑名单缓存

### 3. 传输层加密（TLS）
现状：客户端→网关、网关→tri、服务间 RPC 全部明文，token 在链路上裸奔。
方案：网关对外上 HTTPS（证书/LB 终结）；内部流量按合规要求决定是否上 Triple TLS/mTLS
（本架构下 JWT 验签已保证身份可信，mTLS 是锦上添花不是必需）。

### 4. 账号安全策略
现状：BCrypt 哈希正确，但无锁定/限速，可无限爆破。
方案：登录失败 N 次锁定（Redis 计数）、验证码、IP 维度限流（网关 Sentinel 已有基础）。

## P1 上线后尽快补

### 5. 审计与可观测
- 登录成功/失败、401/403 拒绝事件落日志/上报（现在只有 stdout）
- 引入 traceId 并在 `ContextPropagation*Filter` 里一并透传（和 XID/token 同通道），
  打通跨服务日志串联
- 认证/鉴权指标接入 Prometheus（/actuator/prometheus + micrometer）

### 6. 认证服务独立
现状：登录逻辑内嵌在 user-service（还兼账户扣款）。拆独立 auth 服务（或网关 BFF 层），
user-service 回归纯业务。

### 7. 密码与凭证治理
密码强度校验、定期轮换 JWT_SECRET 的运维流程（滚动双密钥期）、
`JWT_SECRET` 从环境变量改为配置中心+加密。

## P2 择机演进

### 8. OAuth2/OIDC 接入
当前 SecurityWebFilterChain 是标准结构，接 `oauth2ResourceServer()` 即可换成 OIDC
（Keycloak/Authing 等），JWT 逻辑替换为标准资源服务器校验，下游透传架构不变。

### 9. 权限模型精细化
roles+perms 平铺在 JWT claim 里，权限多后 token 膨胀。演进：token 只放 userId/roles，
细粒度权限由下游查缓存（Redis 本地缓存）；或引入 Casbin 风格的策略引擎。

### 10. 防重放/接口签名（面向开放 API 时）
时间戳 + nonce + 签名，网关校验；内部微服务间不需要。

## 已知技术债（记录在案）

- `RpcAuthHolder`/`SecurityContextHolder` 是 ThreadLocal：业务用 CompletableFuture/
  自建线程池会丢身份，需要 `DelegatingSecurityContextExecutor` 包装（Seata XID 同理）
- 异步场景（Dubbo async 调用）下 ContextPropagationConsumerFilter 取不到 ThreadLocal
  里的 token，链式透传会断——本 sample 全同步调用，未触发
- actuator 管理端口（9000-9003）无认证，内网可达即可读健康与指标；上生产应收紧
  （`management.endpoint.health.show-details` 降级 + 网络策略）
