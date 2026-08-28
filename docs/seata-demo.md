# Seata AT 模式分布式事务示例

## 架构

```
gateway (7788, HTTP)
   │
order-service (TM + RM)  ── H2 内存库 seata_order（orders + undo_log）
   │ @GlobalTransactional 开启全局事务
   ├──IDL/Protobuf RPC(Triple)──> user-service (RM)    ── H2 内存库 seata_account（account + undo_log）扣余额
   └──IDL/Protobuf RPC(Triple)──> storage-service (RM) ── H2 内存库 seata_storage（storage + undo_log）扣库存

Seata Server (TC) 8091：注册到 Nacos（SEATA_GROUP / seata-server）
```

**业务库使用 H2 内存库**（MySQL 兼容模式 `MODE=MySQL`）：每个服务一个独立实例，
启动时通过各自 `src/main/resources/schema.sql` / `data.sql` 自动建表和灌初始数据，
重启即重置，sample 无需安装 MySQL。要查数据可给 yml 临时加 H2 的 web console：

```yaml
spring.h2.console.enabled: true   # web 服务可用 /h2-console 访问，JDBC url 见 datasource
```

换回 MySQL：pom 里 h2 换回 mysql-connector-j，yml 数据源改回 `jdbc:mysql://...`，
并执行 `docs/sql/seata/*.sql` 建库（内含等价的 MySQL DDL）。

- **TM**：order-service 的 `createOrder`，`@GlobalTransactional` 开启/提交/回滚全局事务
- **RM**：三个服务各自的数据库操作，由 Seata 自动代理数据源，分支事务 + undo_log
- **XID 透传**：Seata 官方无 Dubbo3 适配器，`dubbo-common` 里自定义了两个 Filter
  （现合并进 `ContextPropagationConsumerFilter` / `ContextPropagationProviderFilter`，与身份 token 一起透传），通过 RPC attachment 传递 XID，
  provider 端绑定到 `RootContext`，使下游分支事务挂在同一全局事务下

## 启动步骤

1. **基础设施**（一个 compose 全含：Nacos + Seata Server）：

   ```bash
   cd docker
   docker compose up -d
   ```

   - Nacos v3.2.4 slim（standalone，控制台 http://localhost:8080/nacos ，账号 nacos/nacos）
   - Seata Server 2.0（TC，8091；控制台 7091），注册到 Nacos 的 `SEATA_GROUP`

   业务库用各服务内嵌的 H2 内存库，无需 MySQL。应用默认连 `localhost` 的
   Nacos / Seata，即「docker 跑基础设施 + 应用跑宿主机」的标准玩法。

   > 换环境时两个口子：
   > - 基础设施在别的机器：`SEATA_IP=<那台机器IP> docker compose up -d`（Seata 注册真实 IP），
   >   应用启动时加 `-Dnacos.address=<那台机器IP>` 或环境变量覆盖（seata 地址用 `-Dseata.server=...`）
   > - 应用在其他机器跑：同理反向覆盖即可，所有地址都参数化了

2. **启动服务**（顺序无所谓）：dubbo-user-service、dubbo-storage-service、dubbo-order-service、dubbo-gateway

   > 监控说明：服务已移除 Actuator（纯 Dubbo 无 Web 容器），观测走
   > SkyWalking（http://localhost:8090）、Dubbo Admin（http://localhost:8095，root/root）、
   > Dubbo QoS（user-service 22222 端口 `telnet` 后执行 `status`）

3. **验证**：

   下单成功（正常提交，三个库同时落库）：

   ```bash
   curl -X POST http://localhost:7788/order/createOrder \
     -H "Content-Type: application/json" \
     -d '{"userId":1,"productCode":"iPhone15","product":"iPhone 15","count":2,"price":6999.0}'
   ```

   下单失败（status=FAIL，验证全局回滚：订单/余额/库存全部还原）：

   ```bash
   curl -X POST http://localhost:7788/order/createOrder \
     -H "Content-Type: application/json" \
     -d '{"userId":1,"productCode":"iPhone15","product":"iPhone 15","count":2,"price":6999.0,"status":"FAIL"}'
   ```

   库存不足回滚（iPhone15 只有 100 台）：

   ```bash
   curl -X POST http://localhost:7788/order/createOrder \
     -H "Content-Type: application/json" \
     -d '{"userId":1,"productCode":"iPhone15","count":999,"price":6999.0}'
   ```

   回滚验证（H2 数据在内存里，重启服务即重置；可在 user/storage 服务加
   `spring.h2.console.enabled=true` 后通过 `/h2-console` 查表）：

   ```sql
   SELECT * FROM seata_order.orders;      -- order 服务库
   SELECT * FROM seata_account.account;   -- user 服务库
   SELECT * FROM seata_storage.storage;   -- storage 服务库
   ```

## 回滚原理（AT 模式）

1. 一阶段：RM 执行业务 SQL 前后解析数据快照，生成 undo_log 与业务 SQL 在**同一个本地事务**提交；
   并向 TC 注册分支、申请行锁
2. 二阶段提交：TC 异步删除 undo_log（几乎零开销）
3. 二阶段回滚：TC 通知各 RM 根据 undo_log 反补数据，释放行锁

## 客户端配置说明（每个 RM 服务）

```yaml
seata:
  tx-service-group: demo_tx_group        # 事务组
  service:
    vgroup-mapping:
      demo_tx_group: default             # 组 -> TC 集群名，与服务端 file.conf 一致
    grouplist:
      default: 192.168.40.237:8091       # 直连兜底，生产可去掉
  registry:
    type: nacos                          # 通过 Nacos 找 TC
  enable-auto-data-source-proxy: true    # AT 核心：自动代理数据源
```

## 注意事项

- 每个参与全局事务的库都必须有 `undo_log` 表
- 业务表建议有主键 + 唯一键（回滚与全局锁依赖）
- Seata Server 的 store 这里用 file 模式（sample 用途），生产建议 db/redis + nacos 配置中心
