package com.example.dubbo.user;

import com.example.dubbo.api.OrderService;
import com.example.dubbo.api.UserService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DubboService
public class UserServiceImpl implements UserService {

    // 模拟用户数据
    private final Map<Long, String> userDb = new HashMap<>();

    public UserServiceImpl() {
        userDb.put(1L, "张三");
        userDb.put(2L, "李四");
        userDb.put(3L, "王五");
        userDb.put(4L, "赵六");
        userDb.put(5L, "孙七");
    }

    // 🔑 关键：注入订单服务
    @DubboReference(protocol = "dubbo")
    private OrderService orderService;

    @Override
    public Map<String, Object> getUserInfo(Long userId) {
        System.out.println("📨 [UserService] getUserInfo(" + userId + ")");
        String name = userDb.getOrDefault(userId, "未知用户");
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("name", name);
        return result;
    }

    @Override
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
}