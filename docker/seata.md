# Seata 2.x 部署指南

> 基于 Spring Cloud Alibaba + Dubbo + Nacos 环境，使用 DB 存储模式

---

## 一、关键准备工作

### 1.1 导入数据库表

确保已执行 Seata 2.x 的内置脚本：

| 角色 | 所需表 | 脚本位置 |
|------|--------|----------|
| **服务端** | `global_table`、`branch_table`、`lock_table` | `script/server/db/` 目录下 |
| **业务数据库** | `undo_log` | `script/client/at/db/` 目录下 |

### 1.2 包名变更提示

进入 Seata 2.x 后，核心 Maven 坐标及配置前缀可能由 `io.seata` 变更为 `org.apache.seata`。

> 本指南以 Spring Cloud Alibaba 兼容的标准前缀为例，请根据你引入的具体 Starter 依赖调整前缀。

---

## 二、Nacos 配置中心设置

在 Nacos 中统一托管 Seata 服务端和客户端配置。

### 2.1 操作步骤

1. 进入 Nacos 控制台
2. 新建配置，**Data ID** 设为 `seataServer.properties`（Seata 2.x 默认读取的外部文件名）
3. **Group** 设为 `SEATA_GROUP`

### 2.2 配置内容

以 MySQL 存储模式为例：

```properties
# 事务分组映射关系（客户端的事务分组 -> 服务端集群名）
seata.service.vgroupMapping.my_tx_group=default

# 2.x 存储模式配置
seata.store.mode=db
seata.store.db.datasource=druid
seata.store.db.dbType=mysql
seata.store.db.driverClassName=com.mysql.cj.jdbc.Driver
seata.store.db.url=jdbc:mysql://127.0.0.1:3306/seata?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
seata.store.db.user=root
seata.store.db.password=root
```

---

## 三、Seata Server 端配置

### 3.1 使用 Nacos 托管

解压 Seata 2.x Server 安装包后，修改其自带的 `conf/application.yml`，让 Server 从 Nacos 读取配置并完成服务注册。

```yaml
seata:
  config:
    # 指定配置中心为 Nacos
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
      data-id: seataServer.properties
      namespace: ""   # 如有命名空间请填写 ID

  registry:
    # 指定注册中心为 Nacos，方便微服务客户端发现自己
    type: nacos
    nacos:
      application: seata-server
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
      namespace: ""
```

### 3.2 不使用 Nacos（直连模式）

不使用 Nacos 后，Seata Server 直接读取本地配置文件 `conf/application.yml`，只需把 `store.mode`、数据库连接信息及 `vgroup-mapping` 直接写死即可。启动时直接运行 `./bin/seata-server.sh` 即可生效。

---

## 四、客户端（微服务）配置

在整合了 Dubbo 的 Spring Cloud 微服务项目中，客户端会自动从 Nacos 中加载所需的 Seata 规则。

### 4.1 使用 Nacos 托管

在整合了 Dubbo 的 Spring Cloud 微服务项目中，通过 application.yml 直接指定 Nacos 配置中心，客户端会自动从 Nacos 中加载所需的 Seata 规则。

### 4.2 不使用 Nacos（直连模式）

不使用 Nacos 托管时，所有配置退化为纯本地管理。只需在各个微服务的 `application.yml` 中进行本地配置：

```yaml
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: my_tx_group

  service:
    vgroup-mapping:
      my_tx_group: default   # 本地建立分组与集群的映射

  registry:
    type: file               # 注册中心改为 file（通过 grouplist 直连服务端）
  config:
    type: file               # 配置中心改为 file

  client:
    service:
      grouplist:
        default: 127.0.0.1:8091   # 直连 Seata Server 的真实 IP 和端口
```

---

## 五、两种模式对比

| 对比项 | Nacos 托管 | 直连（file） |
|--------|-----------|-------------|
| **动态刷新** | 支持配置热更新，无需重启 | 修改需重启所有微服务和 Seata Server |
| **高可用（HA）** | 自动注册/发现，故障自动切换 | 需手动维护每个微服务的 `grouplist` |
| **前期配置** | 稍复杂（需配置 Nacos） | 更简单 |
| **适用场景** | 生产环境、微服务架构 | 本地开发调试 / Demo |

### 5.1 推荐选择

- **生产环境**：强烈建议使用 Nacos 托管，实现配置热更新和高可用自动切换
- **本地调试 / Demo**：可直接使用 `type: file` 直连模式，简单省心

> 虽然不用 Nacos 让前期配置变简单了，但在生产环境或微服务架构中会面临配置僵化和高可用复杂度上升的问题。

---

## 六、快速验证

1. 启动 Nacos（确保 `SEATA_GROUP` 配置已下发）
2. 启动 Seata Server（检查日志确认成功注册到 Nacos）
3. 启动微服务（观察日志确认成功连接到 Seata Server）
4. 执行一笔分布式事务，验证 TCC/AT 模式是否正常工作
