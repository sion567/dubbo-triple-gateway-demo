package com.example.dubbo.order;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.dubbo.api.OrderService;
import com.example.dubbo.api.vo.OrderDTO;
import org.apache.dubbo.config.annotation.DubboService;
import java.util.*;

@DubboService
public class OrderServiceImpl implements OrderService {

    // 模拟订单数据
    private final Map<Long, List<Map<String, Object>>> orderDb = new HashMap<>();

    public OrderServiceImpl() {
        // 用户1的订单
        orderDb.put(1L, Arrays.asList(
                Map.of("orderId", 101L, "product", "iPhone 15", "price", 6999.0, "status", "已发货"),
                Map.of("orderId", 102L, "product", "AirPods Pro", "price", 1899.0, "status", "已完成")
        ));
        // 用户2的订单
        orderDb.put(2L, Arrays.asList(
                Map.of("orderId", 201L, "product", "MacBook Pro", "price", 14999.0, "status", "待付款")
        ));
        // 用户3的订单
        orderDb.put(3L, Arrays.asList(
                Map.of("orderId", 301L, "product", "iPad Air", "price", 4799.0, "status", "已发货"),
                Map.of("orderId", 302L, "product", "Apple Watch", "price", 2999.0, "status", "已完成"),
                Map.of("orderId", 303L, "product", "AirTag", "price", 249.0, "status", "已取消")
        ));
    }

    @Override
    @SentinelResource(value = "getOrdersByUserId", blockHandler = "handleGetOrdersBlock")
    public List<Map<String, Object>> getOrdersByUserId(Long userId) {
        System.out.println("📨 [OrderService] getOrdersByUserId(" + userId + ")");
        // 模拟耗时
        try { Thread.sleep(50); } catch (InterruptedException e) {}
        return orderDb.getOrDefault(userId, Collections.emptyList());
    }

    // 限流降级方法
    public List<Map<String, Object>> handleGetOrdersBlock(Long userId, BlockException ex) {
        System.out.println("⚠️ [OrderService] getOrdersByUserId 被限流! userId=" + userId);
        return Collections.emptyList();
    }

    @Override
    @SentinelResource(value = "getOrderById", blockHandler = "handleGetOrderBlock")
    public Map<String, Object> getOrderById(Long orderId) {
        System.out.println("📨 [OrderService] getOrderById(" + orderId + ")");
        // 简单模拟：遍历查找
        for (List<Map<String, Object>> orders : orderDb.values()) {
            for (Map<String, Object> order : orders) {
                if (order.get("orderId").equals(orderId)) {
                    return order;
                }
            }
        }
        return Map.of("error", "订单不存在");
    }

    public Map<String, Object> handleGetOrderBlock(Long orderId, BlockException ex) {
        System.out.println("⚠️ [OrderService] getOrderById 被限流! orderId=" + orderId);
        return Map.of("error", "服务繁忙，请稍后重试");
    }

    @Override
    public String createOrder(OrderDTO order) {
        return "";
    }
}