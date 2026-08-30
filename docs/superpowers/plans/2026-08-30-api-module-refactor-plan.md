# API 模块重构实现计划

**目标：** 将 `dubbo-proto-api` 重命名为 `dubbo-api`（仅放 proto 文件），各服务内部新增 `api` + `api.impl` 包存放各自的 web-facing HTTP 接口。

**架构：** 改动以目录重命名和文件迁移为主，无新增业务逻辑。各服务 REST 接口移入 `api` 包，VO 随接口归属迁入对应服务。

**技术栈：** Java 17, Maven, Dubbo 3.3.6, Spring Boot 3.5.16

---

## 文件变更总览

| 操作 | 路径 |
|------|------|
| 重命名 | `dubbo-proto-api/` → `dubbo-api/` |
| 清理 | 删除 `dubbo-api/src/main/java/` 下所有文件 |
| 修改 | `dubbo-api/pom.xml`：去掉非 proto 依赖 |
| 新增 | `dubbo-user-service/.../api/UserService.java` |
| 新增 | `dubbo-user-service/.../api/AccountService.java` |
| 新增 | `dubbo-user-service/.../api/impl/UserServiceImpl.java` |
| 新增 | `dubbo-user-service/.../api/impl/AccountServiceImpl.java` |
| 新增 | `dubbo-user-service/.../api/vo/LoginRequest.java` |
| 新增 | `dubbo-order-service/.../api/OrderService.java` |
| 新增 | `dubbo-order-service/.../api/impl/OrderServiceImpl.java` |
| 新增 | `dubbo-order-service/.../api/vo/OrderDTO.java` |
| 新增 | `dubbo-storage-service/.../api/StorageService.java` |
| 新增 | `dubbo-storage-service/.../api/impl/StorageServiceImpl.java` |
| 修改 | 各 service `pom.xml`：去掉 `dubbo-api` 依赖 |
| 修改 | 根 `pom.xml`：modules 列表 `dubbo-proto-api` → `dubbo-api` |
| 删除 | 原 `dubbo-api/src/main/java/com/example/dubbo/api/` |

---

### Task 1: 重命名目录并清理 dubbo-api

- [ ] **Step 1: 重命名模块目录**

```bash
cd E:/sample/dubbo-triple-gateway-demo
mv dubbo-proto-api dubbo-api-temp
mv dubbo-api-temp dubbo-api
```

- [ ] **Step 2: 删除旧的 Java 接口文件和 VO**

```bash
rm -rf dubbo-api/src/main/java
```

- [ ] **Step 3: 更新 dubbo-api/pom.xml，移除非 proto 依赖**

将 `dubbo-api/pom.xml` 内容替换为：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>dubbo-triple-gateway-demo</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>dubbo-api</artifactId>
    <description>IDL(Protobuf) 内部 RPC 接口：Triple + protobuf 二进制，仅供服务间调用</description>

    <dependencies>
        <dependency>
            <groupId>com.google.protobuf</groupId>
            <artifactId>protobuf-java</artifactId>
            <version>3.25.0</version>
        </dependency>
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.dubbo</groupId>
                <artifactId>dubbo-maven-plugin</artifactId>
                <version>${dubbo.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>compile</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <protoSourceDir>${basedir}/src/main/proto</protoSourceDir>
                    <protocVersion>3.25.0</protocVersion>
                    <dubboGenerateType>tri</dubboGenerateType>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4: 提交**

```bash
git add dubbo-api && git commit -m "refactor: rename dubbo-proto-api to dubbo-api, keep only proto files"
```

---

### Task 2: user-service 新增 api + api.impl 包

- [ ] **Step 1: 创建目录结构**

```bash
mkdir -p dubbo-user-service/src/main/java/com/example/dubbo/user/api/vo
mkdir -p dubbo-user-service/src/main/java/com/example/dubbo/user/api/impl
```

- [ ] **Step 2: 新增 UserService.java**

Create: `dubbo-user-service/src/main/java/com/example/dubbo/user/api/UserService.java`

```java
package com.example.dubbo.user.api;

import com.example.dubbo.user.api.vo.LoginRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@RequestMapping("/user")
public interface UserService {
    @GetMapping("/getUser/{id}")
    Map<String, Object> getUserInfo(@PathVariable("id") Long userId);
    @GetMapping("/getOrder/{id:\\d+}")
    Map<String, Object> getUserWithOrders(@PathVariable("id") Long userId);

    /** 用户列表（含角色与余额，管理员） */
    @GetMapping("/list")
    Map<String, Object> list();

    /** 根据用户名查 userId，供其他服务 RPC 调用 */
    @GetMapping("/userId/{username}")
    Long getUserIdByUsername(@PathVariable("username") String username);
}
```

- [ ] **Step 3: 新增 AccountService.java**

Create: `dubbo-user-service/src/main/java/com/example/dubbo/user/api/AccountService.java`

```java
package com.example.dubbo.user.api;

import com.example.dubbo.user.api.vo.LoginRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@RequestMapping("/auth")
public interface AccountService {
    /**
     * 登录：校验用户名密码，签发 JWT（含角色）。成功返回 token，失败返回 code=401。
     */
    @PostMapping("/login")
    Map<String, Object> login(@RequestBody LoginRequest request);
}
```

- [ ] **Step 4: 新增 LoginRequest.java**

Create: `dubbo-user-service/src/main/java/com/example/dubbo/user/api/vo/LoginRequest.java`

```java
package com.example.dubbo.user.api.vo;

public class LoginRequest {
    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
```

- [ ] **Step 5: 新增 UserServiceApi.java（精简版，供 order-service 依赖）**

Create: `dubbo-user-service/src/main/java/com/example/dubbo/user/api/UserServiceApi.java`

```java
package com.example.dubbo.user.api;

/**
 * 精简版用户服务接口，仅暴露 order-service 需要的 getUserIdByUsername 方法。
 * 避免 order-service 依赖整个 user-service 模块。
 */
public interface UserServiceApi {
    /** 根据用户名查 userId，供 order-service RPC 调用 */
    Long getUserIdByUsername(String username);
}
```

- [ ] **Step 6: 修改 UserServiceImpl.java，移除 getUserWithOrders 和 OrderService 依赖**

Create: `dubbo-user-service/src/main/java/com/example/dubbo/user/api/impl/UserServiceImpl.java`

```java
package com.example.dubbo.user.api.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.dubbo.user.api.UserService;
import com.example.dubbo.user.mapper.AccountMapper;
import com.example.dubbo.user.mapper.UserMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;

@DubboService
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final AccountMapper accountMapper;

    public UserServiceImpl(UserMapper userMapper, AccountMapper accountMapper) {
        this.userMapper = userMapper;
        this.accountMapper = accountMapper;
    }

    @Override
    @SentinelResource(value = "getUserInfo")
    public Map<String, Object> getUserInfo(Long userId) {
        System.out.println("📨 [UserService] getUserInfo(" + userId + ")");
        Map<String, Object> user = userMapper.selectById(userId);
        if (user == null) {
            return Map.of("userId", userId, "name", "未知用户");
        }
        return user;
    }

    @Override
    @SentinelResource(value = "getUserWithOrders", blockHandler = "handleGetUserWithOrdersBlock")
    public Map<String, Object> getUserWithOrders(Long userId) {
        System.out.println("📨 [UserService] getUserWithOrders(" + userId + ")");
        Map<String, Object> user = userMapper.selectById(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        if (user == null) {
            result.put("name", "未知用户");
        } else {
            result.put("name", user.get("name"));
        }
        return result;
    }

    public Map<String, Object> handleGetUserWithOrdersBlock(Long userId, BlockException ex) {
        System.out.println("⚠️ [UserService] getUserWithOrders 被限流! userId=" + userId);
        return Map.of("userId", userId, "error", "服务繁忙，请稍后重试");
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> list() {
        List<Map<String, Object>> users = userMapper.selectAll();
        List<Map<String, Object>> accounts = accountMapper.findAll();
        for (Map<String, Object> user : users) {
            Long userId = (Long) user.get("userid");
            if (userId == null) {
                continue;
            }
            for (Map<String, Object> acc : accounts) {
                if (userId.equals(acc.get("user_id"))) {
                    user.put("username", acc.get("username"));
                    user.put("roles", Arrays.asList(((String) acc.get("roles")).split(",")));
                    String permsStr = (String) acc.getOrDefault("perms", "");
                    user.put("perms", permsStr.isBlank() ? List.of() : Arrays.asList(permsStr.split(",")));
                    break;
                }
            }
        }
        return Map.of("code", 0, "users", users);
    }

    @Override
    public Long getUserIdByUsername(String username) {
        Map<String, Object> account = accountMapper.findByUsername(username);
        if (account == null) return null;
        return (Long) account.get("user_id");
    }
}
```

> 注：`getUserWithOrders` 虽然不在 UserService 接口定义中，但已在 UserServiceImpl 中实现（SentinelResource 注解），保留该方法实现。getUserInfo 中原有对 orderService 的调用也已移除。

- [ ] **Step 7: 新增 UserServiceApiImpl.java**

Create: `dubbo-user-service/src/main/java/com/example/dubbo/user/api/impl/UserServiceApiImpl.java`

```java
package com.example.dubbo.user.api.impl;

import com.example.dubbo.user.api.UserServiceApi;
import com.example.dubbo.user.mapper.AccountMapper;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class UserServiceApiImpl implements UserServiceApi {

    private final AccountMapper accountMapper;

    public UserServiceApiImpl(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @Override
    public Long getUserIdByUsername(String username) {
        var account = accountMapper.findByUsername(username);
        if (account == null) return null;
        return (Long) account.get("user_id");
    }
}
```

- [ ] **Step 8: 更新 user-service/pom.xml**

在 `dubbo-user-service/pom.xml` 中添加对 order-service 的 api 模块依赖（用于调用 OrderService）。由于 order-service 的 api 包在 order-service 模块内部，user-service 无法直接依赖其 api 包。

> **解决方案**：order-service 的 OrderService 接口也需要被其他服务调用时，应该放在 `dubbo-api`（proto）或另一个共享 API 模块中。这里 order-service 本身不需要调用任何其他服务的 REST 接口，只有 user-service 需要调用 order-service 的 OrderService。

**最简方案（无循环依赖）**：
- order-service 的 OrderServiceImpl 调用 `UserServiceApi.getUserIdByUsername()` → 依赖 user-service（pom 类型）
- user-service 的 UserServiceImpl 不再调用 OrderService → 无需依赖 order-service

在 `dubbo-order-service/pom.xml` 中添加：
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>dubbo-user-service</artifactId>
    <version>1.0.0</version>
    <type>pom</type>
</dependency>
```

- [ ] **Step 6: 更新 user-service/pom.xml**

在 `dubbo-user-service/pom.xml` 中添加 proto 依赖（`dubbo-api` 重命名后）：
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>dubbo-api</artifactId>  <!-- 重命名后的 proto 模块 -->
    <version>1.0.0</version>
</dependency>
```
（确保 user-service 依赖新的 dubbo-api proto 模块）

- [ ] **Step 7: 删除原 user-service 中的旧实现文件**

```bash
rm dubbo-user-service/src/main/java/com/example/dubbo/user/UserServiceImpl.java
```

- [ ] **Step 8: 提交**

```bash
git add dubbo-user-service && git commit -m "refactor: add api + api.impl packages to user-service"
```

---

### Task 3: order-service 新增 api + api.impl 包

- [ ] **Step 1: 创建目录结构**

```bash
mkdir -p dubbo-order-service/src/main/java/com/example/dubbo/order/api/vo
mkdir -p dubbo-order-service/src/main/java/com/example/dubbo/order/api/impl
```

- [ ] **Step 2: 新增 OrderService.java**

Create: `dubbo-order-service/src/main/java/com/example/dubbo/order/api/OrderService.java`

```java
package com.example.dubbo.order.api;

import com.example.dubbo.order.api.vo.OrderDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping("/order")
public interface OrderService {

    List<Map<String, Object>> getOrdersByUserId(Long userId);

    @GetMapping("/getOrder")
    Map<String, Object> getOrderById(@RequestParam("id") Long orderId);

    @PostMapping("/createOrder")
    String createOrder(@RequestBody OrderDTO order);

    /** 订单列表（角色不同返回不同：管理员返回全部，普通用户返回自己的） */
    @GetMapping("/list")
    List<Map<String, Object>> list();

    /**
     * 秒杀/削峰下单：请求写入 RocketMQ 后立即返回"排队中"，
     * 由消费者异步执行真正的下单（Seata 全局事务）。
     */
    @PostMapping("/quickOrder")
    Map<String, Object> quickOrder(@RequestBody OrderDTO order);

    /** 更新订单状态（管理员） */
    @PostMapping("/updateStatus")
    Map<String, Object> updateStatus(@RequestBody Map<String, Object> body);

    /** 删除订单（管理员） */
    @DeleteMapping("/delete/{id}")
    Map<String, Object> delete(@PathVariable("id") Long id);
}
```

- [ ] **Step 3: 新增 OrderDTO.java**

Create: `dubbo-order-service/src/main/java/com/example/dubbo/order/api/vo/OrderDTO.java`

```java
package com.example.dubbo.order.api.vo;

import java.math.BigDecimal;

public class OrderDTO {
    private Long orderId;
    private Long userId;
    private String product;
    private String productCode;
    private Integer count;
    private BigDecimal price;
    private String status;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
```

- [ ] **Step 4: 新增 OrderServiceImpl.java**

Create: `dubbo-order-service/src/main/java/com/example/dubbo/order/api/impl/OrderServiceImpl.java`

内容从原 `dubbo-order-service/src/main/java/com/example/dubbo/order/OrderServiceImpl.java` 复制，关键改动：
- package 改为 `com.example.dubbo.order.api.impl`
- `com.example.dubbo.api.OrderService` → `com.example.dubbo.order.api.OrderService`
- `com.example.dubbo.api.UserService` → `com.example.dubbo.user.api.UserServiceApi`
- `com.example.dubbo.api.vo.OrderDTO` → `com.example.dubbo.order.api.vo.OrderDTO`
- `@DubboReference private UserService userService` → `@DubboReference private UserServiceApi userServiceApi`
- 所有 `userService.xxx()` 调用 → `userServiceApi.xxx()`

> 注：list() 方法中调用了 `userService.getUserIdByUsername(username)`，改为调用 `userServiceApi.getUserIdByUsername(username)`。

- [ ] **Step 6: 更新 order-service/pom.xml**

在 `dubbo-order-service/pom.xml` 中添加对 user-service 的 pom 类型依赖（用于访问 UserServiceApi）：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>dubbo-user-service</artifactId>
    <version>1.0.0</version>
    <type>pom</type>
</dependency>
```

- [ ] **Step 7: 删除原 order-service 中的旧实现文件**

```bash
rm dubbo-order-service/src/main/java/com/example/dubbo/order/OrderServiceImpl.java
```

- [ ] **Step 8: 提交**

```bash
git add dubbo-order-service && git commit -m "refactor: add api + api.impl packages to order-service"
```

---

### Task 4: storage-service 新增 api + api.impl 包

- [ ] **Step 1: 创建目录结构**

```bash
mkdir -p dubbo-storage-service/src/main/java/com/example/dubbo/storage/api/impl
```

- [ ] **Step 2: 新增 StorageService.java**

Create: `dubbo-storage-service/src/main/java/com/example/dubbo/storage/api/StorageService.java`

```java
package com.example.dubbo.storage.api;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@RequestMapping("/storage")
public interface StorageService {

    /** 全量库存列表 */
    @GetMapping("/list")
    List<Map<String, Object>> list();

    /** 新增/补货（存在则累加数量） */
    @PostMapping("/save")
    Map<String, Object> save(@RequestBody Map<String, Object> storage);

    /** 删除 */
    @DeleteMapping("/delete/{productCode}")
    Map<String, Object> delete(@PathVariable("productCode") String productCode);
}
```

- [ ] **Step 3: 新增 StorageServiceImpl.java**

Create: `dubbo-storage-service/src/main/java/com/example/dubbo/storage/api/impl/StorageServiceImpl.java`

内容从原 `dubbo-storage-service/src/main/java/com/example/dubbo/storage/StorageServiceImpl.java` 复制，package 改为 `com.example.dubbo.storage.api.impl`，import 中的 `com.example.dubbo.api.StorageService` → `com.example.dubbo.storage.api.StorageService`。

- [ ] **Step 4: 删除原 storage-service 中的旧实现文件**

```bash
rm dubbo-storage-service/src/main/java/com/example/dubbo/storage/StorageServiceImpl.java
```

- [ ] **Step 5: 提交**

```bash
git add dubbo-storage-service && git commit -m "refactor: add api + api.impl packages to storage-service"
```

---

### Task 5: 更新根 pom.xml 并清理旧 dubbo-api 依赖

- [ ] **Step 1: 更新根 pom.xml modules 列表**

将根 `pom.xml` 中 `<modules>` 下的 `<module>dubbo-proto-api</module>` 改为 `<module>dubbo-api</module>`，删除 `<module>dubbo-api</module>` 行。

Before:
```xml
<modules>
    <module>dubbo-api</module>
    <module>dubbo-proto-api</module>
    <module>dubbo-common</module>
    ...
</modules>
```

After:
```xml
<modules>
    <module>dubbo-api</module>
    <module>dubbo-common</module>
    ...
</modules>
```

- [ ] **Step 2: 检查并更新各服务 pom.xml 中的 dubbo-api 依赖**

确认以下依赖存在于对应 pom.xml 中：

**dubbo-user-service/pom.xml** 应有：
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>dubbo-api</artifactId>  <!-- proto，服务间 RPC -->
    <version>1.0.0</version>
</dependency>
```

**dubbo-order-service/pom.xml** 应有：
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>dubbo-api</artifactId>  <!-- proto -->
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.example</groupId>
    <artifactId>dubbo-user-service</artifactId>  <!-- pom 类型，用于访问 UserServiceApi -->
    <version>1.0.0</version>
    <type>pom</type>
</dependency>
```

**dubbo-storage-service/pom.xml** 应有：
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>dubbo-api</artifactId>  <!-- proto -->
    <version>1.0.0</version>
</dependency>
```

**dubbo-gateway/pom.xml**：确认没有 `dubbo-api` 依赖（网关不依赖任何 API 模块）。

- [ ] **Step 3: 提交**

```bash
git add pom.xml dubbo-user-service/pom.xml dubbo-order-service/pom.xml dubbo-storage-service/pom.xml dubbo-gateway/pom.xml && git commit -m "refactor: update module dependencies after API split"
```

---

### Task 6: 最终验证

- [ ] **Step 1: Maven 编译验证**

```bash
cd E:/sample/dubbo-triple-gateway-demo
mvn clean compile -DskipTests
```

预期：所有模块编译通过，无 `com.example.dubbo.api.*` 找不到的报错。

- [ ] **Step 2: 检查各服务 pom.xml 中是否还有对旧 dubbo-api 的引用**

搜索所有 `pom.xml` 中是否有 `<artifactId>dubbo-api</artifactId>` 的旧依赖引用（指当前内容为 REST 接口的那个），确认已全部替换为各服务内部的 api 包。

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "chore: finalize API module refactor"
```
