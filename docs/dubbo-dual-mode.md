# Dubbo 双开发模式：接口+POJO（REST）与 IDL/Protobuf（内部 RPC）

本工程同时演示 Dubbo 3 的两种服务定义方式，各司其职：

| 模式 | 用途 | 模块 | 协议/序列化 |
|---|---|---|---|
| 接口+POJO + Spring Web 注解 | **对外 REST 端点**（网关/前端）：/user/** /order/** /storage/** | `dubbo-api` | Triple + JSON（tri rest） |
| IDL（Protobuf） | **服务间内部 RPC**（Seata 全局事务中的扣款/扣库存） | `dubbo-proto-api` | Triple + protobuf 二进制 |

选择依据：对外接口要给 HTTP 客户端消费，必须是 JSON REST；内部 RPC 没有这个约束，
用 IDL 换取强类型契约、跨语言能力和二进制性能。"REST 门面 + gRPC 内核"是常见生产形态。

## IDL 模式的组成（dubbo-proto-api 模块）

- `src/main/proto/account.proto` / `storage.proto` / `user.proto`：service + message 定义
- `dubbo-maven-plugin`（3.3.0+，版本与 dubbo 核心一致）：compile 阶段根据 proto 生成
  - 普通接口 `AccountProtoService`（返回值风格，不是回调）
  - 基类 `DubboAccountProtoServiceTriple.AccountProtoServiceImplBase`
  - message 类 `DebitRequest/DebitResponse/...`
- 依赖：`protobuf-java`（生成代码需要）；`dubbo`（ImplBase 引用 triple 类）
- `user.proto` 的 `GetUsernamesByUserIds` 是"内部查询走 IDL"的示例：order-service
  订单列表补用户名时批量调用（一次 RPC），names 与 user_ids 同序、空串表示用户不存在
  （proto3 无 null，缺失语义要在注释里写成契约）

## 服务端（user-service / storage-service）

```java
@DubboService
public class AccountProtoImpl extends DubboAccountProtoServiceTriple.AccountProtoServiceImplBase {
    @Override
    @PreAuthorize("hasRole('USER')")          // 鉴权照常可用（还是 Spring Bean）
    public DebitResponse debit(DebitRequest request) {
        return DebitResponse.newBuilder().setSuccess(true).build();
    }
}
```

## 消费端（order-service OrderTxService）

```java
@DubboReference
private AccountProtoService accountProtoService;   // 注入生成的普通接口

DebitResponse r = accountProtoService.debit(
        DebitRequest.newBuilder().setUserId(1L).setMoney(99.0).build());
```

## 与既有机制的关系（关键：全部无缝兼容）

- **Seata XID 透传**：走 Dubbo Filter 的 attachment，与序列化方式无关，IDL 调用照样挂同一全局事务
- **身份透传/鉴权**：ContextPropagation 过滤器同理；@PreAuthorize 在 ImplBase 实现上照常生效
- **SkyWalking**：Agent 对 triple+protobuf 有原生埋点
- **回滚语义**：IDL 方法失败用返回值表达（success=false），由调用方抛异常触发 Seata 回滚——
  因为 Triple IDL 方法抛业务异常的传播行为与接口模式不同，返回值语义更可控

## 改 proto 的流程

改 `.proto` → `mvn clean compile`（dubbo-proto-api 模块重新生成 stub）→ 服务端/消费端同步改代码。
CI 里建议把"proto 文件变更必须走 review"写进规范——它就是跨服务的契约。

## 环境注意（本机私服缺件）

dubbo-maven-plugin、dubbo-compiler、protoc exe 及若干传递依赖在公司私服上不存在，
已手动从 Maven Central 下载进本地仓库。换机器/清仓库后需要同样的补齐动作，
或推动私服代理刷新。protoc 版本必须与 protobuf-java 版本匹配（当前 3.25.0）。
