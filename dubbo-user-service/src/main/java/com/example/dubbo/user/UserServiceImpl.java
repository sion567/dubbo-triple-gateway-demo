package com.example.dubbo.user;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.dubbo.api.OrderService;
import com.example.dubbo.api.UserService;
import com.example.dubbo.user.mapper.AccountMapper;
import com.example.dubbo.user.mapper.UserMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import java.util.*;

@DubboService
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final AccountMapper accountMapper;

    public UserServiceImpl(UserMapper userMapper, AccountMapper accountMapper) {
        this.userMapper = userMapper;
        this.accountMapper = accountMapper;
    }

    @DubboReference(protocol = "dubbo")
    private OrderService orderService;

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
            // H2 连接串开了 DATABASE_TO_LOWER=TRUE，未加引号的别名 userId 落到 Map 里是小写 "userid"
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
