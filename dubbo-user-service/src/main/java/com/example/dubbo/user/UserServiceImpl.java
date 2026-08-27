package com.example.dubbo.user;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.dubbo.api.OrderService;
import com.example.dubbo.api.UserService;
import com.example.dubbo.api.vo.LoginRequest;
import com.example.dubbo.security.JwtUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DubboService
public class UserServiceImpl implements UserService {

    // 模拟用户数据
    private final Map<Long, String> userDb = new HashMap<>();

    // 登录账号：username -> (bcrypt 密码哈希, 角色)
    // user/123456 -> ROLE_USER；admin/admin123 -> ROLE_ADMIN
    private final Map<String, Map<String, Object>> accountDb = new HashMap<>();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final com.example.dubbo.user.mapper.AccountMapper accountMapper;

    public UserServiceImpl(com.example.dubbo.user.mapper.AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
        userDb.put(1L, "张三");
        userDb.put(2L, "李四");
        userDb.put(3L, "王五");
        userDb.put(4L, "赵六");
        userDb.put(5L, "孙七");

        accountDb.put("user", Map.of(
                "passwordHash", passwordEncoder.encode("123456"),
                "userId", 1L,
                "roles", List.of("ROLE_USER"),
                "perms", List.of("order:query", "order:create")));
        accountDb.put("admin", Map.of(
                "passwordHash", passwordEncoder.encode("admin123"),
                "userId", 2L,
                "roles", List.of("ROLE_ADMIN", "ROLE_USER"),
                "perms", List.of("order:query", "order:create", "system:dept:add", "system:dept:query")));
    }

    // 🔑 关键：注入订单服务
    @DubboReference(protocol = "dubbo")
    private OrderService orderService;

    @Override
    @SentinelResource(value = "getUserInfo")
    public Map<String, Object> getUserInfo(Long userId) {
        System.out.println("📨 [UserService] getUserInfo(" + userId + ")");
        String name = userDb.getOrDefault(userId, "未知用户");
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("name", name);
        return result;
    }

    @Override
    @SentinelResource(value = "getUserWithOrders", blockHandler = "handleGetUserWithOrdersBlock")
    public Map<String, Object> getUserWithOrders(Long userId) {
        System.out.println("📨 [UserService] getUserWithOrders(" + userId + ")");

        // 1. 查询用户基本信息（自己处理）
        String name = userDb.getOrDefault(userId, "未知用户");
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("name", name);

        // 2. 🔑 调用订单服务获取订单列表（服务间链式调用）
//    消费端 @DubboReference(protocol = "dubbo")
//    ↓
//    走 Dubbo TCP 协议直接调用接口方法
//    ↓
//    Spring Web 注解被完全忽略
//    ↓
//    直接调用 getOrderById(Long orderId) 方法
        try {
            List<Map<String, Object>> orders = orderService.getOrdersByUserId(userId);
            result.put("orders", orders);
            result.put("orderCount", orders.size());
            System.out.println("✅ [UserService] 成功调用订单服务，获取 " + orders.size() + " 条订单");
        } catch (Exception e) {
            System.err.println("❌ [UserService] 调用订单服务失败: " + e.getMessage());
            result.put("orders", List.of());
            result.put("orderCount", 0);
            result.put("error", "订单服务暂时不可用");
        }

        return result;
    }

    // 限流降级方法
    public Map<String, Object> handleGetUserWithOrdersBlock(Long userId, BlockException ex) {
        System.out.println("⚠️ [UserService] getUserWithOrders 被限流! userId=" + userId);
        return Map.of("userId", userId, "error", "服务繁忙，请稍后重试");
    }

    /**
     * 登录：BCrypt 校验密码，成功签发 JWT（sub=用户名, roles=角色, 2h 过期）。
     */
    @Override
    public Map<String, Object> login(LoginRequest request) {
        String username = request.getUsername();
        Map<String, Object> account = accountDb.get(username);

        // 用户不存在或密码错误统一返回 401，不区分提示（避免用户名枚举）
        if (account == null || !passwordEncoder.matches(request.getPassword(), (String) account.get("passwordHash"))) {
            System.out.println("🚫 [UserService] 登录失败: " + username);
            return Map.of("code", 401, "message", "用户名或密码错误");
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) account.get("roles");
        @SuppressWarnings("unchecked")
        List<String> perms = (List<String>) account.getOrDefault("perms", List.of());
        String token = JwtUtil.createToken(username, roles, perms);
        System.out.println("🔓 [UserService] 登录成功: " + username + ", roles=" + roles + ", perms=" + perms);

        return Map.of(
                "code", 0,
                "username", username,
                "roles", roles,
                "perms", perms,
                "token", token,
                "tokenType", "Bearer",
                "expiresAt", Instant.now().plusSeconds(2 * 3600).toString());
    }

    /**
     * 前端用户列表：mock 用户名 + 登录账号的角色 + DB 余额。
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> list() {
        List<Map<String, Object>> users = new java.util.ArrayList<>();
        userDb.forEach((id, name) -> {
            Map<String, Object> u = new HashMap<>();
            u.put("userId", id);
            u.put("name", name);
            users.add(u);
        });
        // 附上账号信息（username/roles/余额）
        accountDb.forEach((username, acc) -> {
            Long userId = (Long) acc.get("userId");
            Double money = accountMapper.selectMoney(userId);
            users.stream().filter(u -> userId.equals(u.get("userId"))).findFirst().ifPresent(u -> {
                u.put("username", username);
                u.put("roles", acc.get("roles"));
                u.put("money", money);
            });
        });
        return Map.of("code", 0, "users", users);
    }
}