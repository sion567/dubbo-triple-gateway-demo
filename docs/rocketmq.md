# RocketMQ 削峰填谷示例

## 场景

秒杀/大促时下单请求洪峰直接打到 DB 和下游服务会把系统压垮。引入 RocketMQ 后：

```
前端/客户端
  │ POST /order/quickOrder（鉴权同普通下单）
  ▼
order-service QuickOrderProducer ──syncSend──> RocketMQ Broker（topic: order-quick-topic）
  │                                            ↑ 流量在这里排队堆积
  立即返回 {code:0, message:"排队中", queueKey}
  ▼
QuickOrderConsumer（按自身消费能力拉取）
  │ 逐条消费
  ▼
OrderTxService#doCreate ──Seata 全局事务──> 写订单 + 扣余额 + 扣库存
```

- 峰值流量堆积在 Broker 上，DB 压力 = 消费速率（可配消费者并发/限流）
- 消费失败（余额不足/库存不足/事务回滚）抛异常 → RocketMQ 自动重试，
  超过重试次数进入死信队列 `%DLQ%order-quick-group`，可人工介入

## 基础设施

```bash
cd docker && docker compose up -d
```

新增 `rocketmq-namesrv`（9876）与 `rocketmq-broker`（10911，`autoCreateTopicEnable=true`，
sample 免建 topic，生产应预建）。配置在 [docker/rocketmq/broker.conf](../docker/rocketmq/broker.conf)，
关键项 `brokerIP1`：默认 `127.0.0.1` 适配"docker 跑基础设施 + 应用跑宿主机"，
应用跨机器时改为宿主机 IP。

## 代码位置

| 类 | 职责 |
|---|---|
| `QuickOrderProducer` | syncSend 进 `order-quick-topic`，key=userId+时间戳 |
| `QuickOrderConsumer` | `@RocketMQMessageListener` 消费，调 `OrderTxService#doCreate` |
| `OrderTxService` | `@GlobalTransactional` 下单事务（REST 与 MQ 共用） |
| `OrderServiceImpl#quickOrder` | REST 入口（`@PreAuthorize("hasRole('USER')")`） |

注意：MQ 消费线程里没有 HTTP 上下文，SecurityContext 不存在，
所以鉴权留在 REST 入口层、事务逻辑抽到独立 Bean —— 这也是 `@GlobalTransactional`
必须经过 Spring 代理而不能自调用的原因。

## 验证

1. `docker compose up -d`（等 broker 起来约 10s）
2. 启动 order-service（application.yml 已配 `rocketmq.name-server`）
3. 前端订单页点"⚡ 秒杀下单"——立即提示"排队中"，约 1~2 秒后刷新列表出现新订单；
   或 test.http 的 quickOrder 请求
4. 并发压测对比：普通 createOrder 同步扛全部压力；quickOrder 只受消费速率约束

## 生产化提醒

- 幂等：消费端可能重试导致重复下单，需按 queueKey/业务单号做幂等（DB 唯一键或 Redis setnx）
- 顺序消息：同一用户的下单/取消需要顺序时用 MessageQueueSelector 按 userId 哈希到同一队列
- 事务消息：若需要"DB 操作和发消息原子一致"，用 RocketMQ 事务消息（半消息 + 本地事务回查），
  替代"先落库再发消息"的补偿方案
- 监控：RocketMQ Exporter + Prometheus（消息堆积、消费延迟告警）；
  SkyWalking Agent 自带 rocketmq-client 埋点，链路里能看到消息收发 span
