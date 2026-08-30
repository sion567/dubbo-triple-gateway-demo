# API 模块重构设计方案

**日期：** 2026-08-30

## 背景

当前项目结构中：
- `dubbo-api`：包含 Spring REST 接口（UserService、OrderService、AccountService、StorageService），带 `@RequestMapping`，这些是 web-facing HTTP 接口
- `dubbo-proto-api`：包含 proto 文件，生成 Triple stub，用于**内部服务间 RPC**
- 各 service 同时实现 REST 接口，也暴露 proto-based RPC 端点

问题：`dubbo-api` 里的接口本质是对外 HTTP 接口，应该放在各自服务内部；`dubbo-proto-api` 才是真正的服务间 RPC 接口，应该改名为 `dubbo-api`。

## 重构目标

| 模块 | 重构前 | 重构后 |
|------|--------|--------|
| `dubbo-api` | Spring REST 接口 + VO | **删除**（内容迁出） |
| `dubbo-proto-api` | proto 文件 + 生成的 Triple stub | **改名为 `dubbo-api`**（仅放 proto 文件） |
| 各 service | 实现类在根包 | 新增 `api` + `api.impl` 包 |

## 重构后目标结构

```
dubbo-triple-gateway-demo/
├── dubbo-api/                      # 重命名自 dubbo-proto-api，仅放 proto 文件
│   └── src/main/proto/
│       ├── user.proto
│       ├── account.proto
│       └── storage.proto
│
├── dubbo-user-service/
│   └── src/main/java/com/example/dubbo/user/
│       ├── api/                    # 新增：web-facing HTTP 接口定义
│       │   ├── UserService.java   # @RequestMapping("/user")
│       │   └── AccountService.java # @RequestMapping("/auth")
│       └── api/impl/              # 新增：接口实现
│           ├── UserServiceImpl.java
│           └── AccountServiceImpl.java
│
├── dubbo-order-service/
│   └── src/main/java/com/example/dubbo/order/
│       ├── api/                   # 新增
│       │   └── OrderService.java  # @RequestMapping("/order")
│       └── api/impl/              # 新增
│           └── OrderServiceImpl.java
│
├── dubbo-storage-service/
│   └── src/main/java/com/example/dubbo/storage/
│       ├── api/                   # 新增
│       │   └── StorageService.java # @RequestMapping("/storage")
│       └── api/impl/              # 新增
│           └── StorageServiceImpl.java
│
├── dubbo-gateway/                  # 不依赖任何 API 模块
└── dubbo-common/                   # 保持不变
```

## 接口归属

| 接口 | 实现方 | 所在包 |
|------|--------|--------|
| UserService | user-service | `com.example.dubbo.user.api.UserService` |
| AccountService | user-service | `com.example.dubbo.user.api.AccountService` |
| OrderService | order-service | `com.example.dubbo.order.api.OrderService` |
| StorageService | storage-service | `com.example.dubbo.storage.api.StorageService` |

## VO 归属

| VO | 迁移至 |
|----|--------|
| LoginRequest | user-service 的 `api.vo.LoginRequest` |
| OrderDTO | order-service 的 `api.vo.OrderDTO` |

## 实施步骤

1. **模块改名**：`dubbo-proto-api` → `dubbo-api`，清空 src/main/java，pom.xml 去掉非 proto 依赖
2. **各服务新增 api + api.impl 包**：将 `dubbo-api` 中的接口和实现迁入
3. **VO 迁移**：LoginRequest → user-service，OrderDTO → order-service
4. **更新依赖**：
   - 各 service 的 pom.xml 去掉 `dubbo-api` 依赖（如果不再需要 REST 接口共享）
   - 或保留 `dubbo-api`（proto）依赖用于服务间 RPC
5. **网关 pom.xml**：去掉 `dubbo-api` 依赖（路由只依赖 lb:// 转发）
6. **根 pom.xml**：更新 modules 列表，将 `dubbo-proto-api` 改为 `dubbo-api`
7. **验证**：各服务启动正常，网关路由正常

## 关键约束

- REST 接口路径（`@RequestMapping`）保持现状不变
- proto 接口实现（AccountProtoImpl、UserProtoImpl、StorageProtoImpl）留在原 service，不移动
- account.proto 留在 proto 模块（shared across services）
- 网关不依赖任何 API 模块
