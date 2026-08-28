# 生产就绪差距清单（Production Readiness）

> 定位：本工程作为**技术选型验证 sample** 已完成使命——无 Tomcat 等外置容器（内嵌 Netty/Triple 直出）、
> 网关统一认证、JWT 信任边界、方法级权限、服务发现/配置/事务/消息/链路追踪全家桶齐备，
> 与主流微服务架构形态一致。本文档记录**如果作为正式公司项目上线**还欠的账，供真实项目立项时对照。
> 安全类欠账见 `security-production-checklist.md`，本文不重复。

## P0 — 不改就上不了线

### 1. 数据层：H2 内存库换 MySQL + schema 版本管理
现状：所有服务 `jdbc:h2:mem:...`，**重启即全部数据清零**；`schema.sql`/`data.sql` 裸 SQL 无版本管理。
H2 的 `DATABASE_TO_LOWER` 小写化行为已在代码里留坑（订单页 `userid` NPE、前端列名全是小写 key）。
方案：MySQL + Flyway/Liquibase，迁移时还掉小写 key 补丁代码。

### 2. 注册中心/TC 高可用
现状：Nacos 单机内嵌 Derby（compose 注释已自认），挂了服务发现全灭且配置丢失；
Seata TC file 模式直连单实例，TC 挂全局事务全卡。
方案：Nacos 集群 + MySQL 存储；Seata TC 集群（或明确放弃 AT，见 P2-11）。

### 3. 配置治理：去硬编码 IP 与凭证
现状：`172.26.205.13` 散布在各服务 yml 默认值；`nacos/nacos` 账号、`demo-jwt-secret-...` 默认密钥。
方案：配置中心下发 + 环境变量注入；**删掉 demo 默认值**——配置缺失应启动失败，
而不是静默降级到 demo 密钥（安全清单 P0-7 同源问题）。

### 4. 自动化测试
现状：零测试。至少覆盖三条命脉：
- 下单事务回滚（Seata FAIL 分支）
- 鉴权链路（401/403/正常，含 token 透传）
- 金额计算（BigDecimal 边界）

### 5. 日志规范化
现状：全链路 `System.out.println` + emoji。生产需结构化日志、级别可控、平台可采集；
logback 骨架已有，业务日志接入即可。

## P1 — 上线第一个月必须补

### 6. CI/CD 与镜像化
无 pipeline、无服务 Dockerfile、无部署描述。至少：Maven 流水线（编译→测试→镜像）
+ K8s Deployment/Service；**proto 模块代码生成必须在 CI 做**（契约变更可视化，
dual-mode 文档自定的规矩）。

### 7. 多实例与探活
每个服务都是单点。K8s 多副本 + liveness/readiness（actuator health 现成）
+ 优雅停机（Dubbo QoS shutdown、MQ 消费者 rebalance）。

### 8. quickOrder（MQ 削峰）可靠性语义
消息丢失策略（重试/DLQ）、**消费幂等**——`doCreate` 无幂等键，MQ 重投会重复扣款扣库存。
最便宜的幂等：业务单号唯一约束。

### 9. 接口健壮性与资金正确性
- 订单 `selectAll()` 无分页（批量查用户名的 N+1 根源在此）
- `getOrdersByUserId` 硬编码 `Thread.sleep(50)`（限流演示用，上线删）
- **金额裸 double**（`DebitRequest.money` proto 同样）——资金字段必须 BigDecimal 或分单位整数，
  这是正确性问题不是风格问题；proto 契约变更要走 review（同 dual-mode 流程）

### 10. 前端交付
静态文件手 build；token 存 localStorage（XSS 可窃取）。生产需 HTTPS、托管方案、token 存储策略评估。

## P2 — 择机演进

### 11. Seata 当前 `enabled: false`
「三库分布式事务」卖点实际处于关闭状态。上线前二选一并写入文档：
真开（TC 高可用先行）或降级最终一致性（本地消息表/TCC）。

### 12. Sentinel 规则持久化
dashboard `localhost:8080`、规则在内存重启即丢。接 Nacos 数据源。

### 13. 依赖安全扫描
OWASP dependency-check / Trivy 纳入 CI。

### 14. 指标与告警
`web-application-type: none` 的服务无 Prometheus 暴露端口；SkyWalking 有链路无告警。
补 micrometer + 告警规则。

## 落地建议

架构骨架（网关统一认证 / JWT 信任边界 / 上下文透传 / 双模式 RPC / 全链路可观测雏形）合格，
差的是工程化外围。顺序：**先做 1-3 成"生产基线"**（数据层、注册中心集群、配置治理）——
这三项改完，其余各项可独立渐进，无需大重构。真实项目立项时，本文与
`security-production-checklist.md` 合并成 checklist 逐项验收。
