# SkyWalking 链路追踪接入

采用 **Java Agent 无侵入方案**：业务代码零改动，Agent 通过字节码增强自动埋点
（覆盖 Dubbo 3、Spring Cloud Gateway/WebFlux、Spring MVC/rest、JDBC、H2 等），
跨服务 traceId 由 Agent 通过协议头（SW8）自动透传，不需要我们手动传递。

## 1. 启动基础设施

```bash
cd docker && docker compose up -d
```

新增两个服务：
- **skywalking-oap**（10.1.0）：数据收集与分析，gRPC 11800（Agent 上报）、REST 12800
- **skywalking-ui**：浏览器打开 **http://localhost:8090**（8080 被 Nacos 控制台占了）

> OAP 用 H2 内存存储，重启数据清空（sample 用途）。生产换 Elasticsearch/BanyanDB：
> `SW_STORAGE: elasticsearch` + 对应连接配置。

## 2. 下载 Java Agent（一次性）

```bash
bash scripts/get-skywalking-agent.sh
# Windows PowerShell 手动下载解压也可以：
# https://archive.apache.org/dist/skywalking/javaagent/9.6.0/apache-skywalking-java-agent-9.6.0.tgz
```

得到项目根目录 `skywalking-agent/skywalking-agent.jar`。

## 3. 给每个服务挂 Agent（IDEA VM options 示例）

每个服务的启动参数加上（service_name 各自不同）：

```
-javaagent:E:/sample/dubbo-triple-gateway-demo/skywalking-agent/skywalking-agent.jar
-Dskywalking.agent.service_name=dubbo-order-service
-Dskywalking.collector.backend_service=localhost:11800
```

| 服务 | service_name |
|---|---|
| dubbo-gateway | dubbo-gateway |
| dubbo-order-service | dubbo-order-service |
| dubbo-user-service | dubbo-user-service |
| dubbo-storage-service | dubbo-storage-service |

> IDEA 里每个 Application 的 Run Configuration → VM options 里加这三行即可。

## 4. 已内置的代码级增强（不依赖 Agent 也能编译运行）

- **日志带 traceId**：四个服务的 `logback-spring.xml` 用了 SkyWalking 的
  `TraceIdPatternLogbackLayout`，日志格式为 `HH:mm:ss [TID] LEVEL logger - msg`。
  未挂 Agent 时 TID 显示 `N/A`；挂上后输出真实 traceId，可与 UI 拓扑对账。
- **自定义 span**：`ContextPropagationConsumerFilter` / `ContextPropagationProviderFilter`
  标了 `@Trace`，在调用链上能看到这两个透传节点的耗时（XID+身份还原一目了然）。

## 5. 验证

1. 打开 http://localhost:8090 → General Service → Trace，能看到
   `gateway → order-service → user/storage` 的完整调用链
2. 前端下一单（或 test.http），Trace 里应包含：
   gateway(HTTP entry) → ContextPropagationConsumer → order createOrder → JDBC(insert orders)
   → ContextPropagationConsumer → user debit → JDBC(update account) → ...
3. 服务日志里 `[TID]` 与 UI 中 traceId 一致

## 原理说明：为什么不需要在 ContextPropagationFilter 里透传 traceId

Dubbo Consumer/Provider、Gateway 的 SW8 协议头注入/提取都是 Agent 插件自动完成的，
与我们的 XID/token 透传正交（那是业务上下文，这是追踪上下文）。我们的 Filter 里加
`@Trace` 只是让"还原身份"这个动作在链路图上可见，不承担传播职责。
