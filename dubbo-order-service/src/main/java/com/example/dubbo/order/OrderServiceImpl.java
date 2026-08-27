package com.example.dubbo.order;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.dubbo.api.AccountService;
import com.example.dubbo.api.OrderService;
import com.example.dubbo.api.StorageService;
import com.example.dubbo.api.vo.OrderDTO;
import com.example.dubbo.order.mapper.OrderMapper;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import java.util.*;

@DubboService
public class OrderServiceImpl implements OrderService {

    // 模拟订单数据（查询演示用）
    private final Map<Long, List<Map<String, Object>>> orderDb = new HashMap<>();

    private final OrderMapper orderMapper;

    @DubboReference(protocol = "dubbo")
    private AccountService accountService;

    @DubboReference(protocol = "dubbo")
    private StorageService storageService;

    public OrderServiceImpl(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
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

    /**
     * 下单：跨 3 个库的分布式事务（Seata AT 模式）。
     * 1. 本服务：orders 表插入订单（分支事务，seata_order 库）
     * 2. user-service：account 表扣余额（分支事务，seata_account 库）
     * 3. storage-service：storage 表扣库存（分支事务，seata_storage 库）
     *
     * 任一环节抛异常（余额不足/库存不足/模拟失败），三个库全部自动回滚。
     * 验证回滚：请求体 status 传 "FAIL"。
     */
    @Override
    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    public String createOrder(OrderDTO order) {
        System.out.println("🚀 [OrderService] 全局事务开始, XID = " + RootContext.getXID());

        int count = order.getCount() == null ? 1 : order.getCount();
        double money = order.getPrice() == null ? 0.0 : order.getPrice() * count;

        // 1. 写订单（本地分支事务，Seata 代理数据源自动注册分支 + 记录 undo_log）
        orderMapper.insert(order.getUserId(), order.getProductCode(),
                order.getProduct(), count, money, "INIT");
        System.out.println("📦 [OrderService] 订单已写入 seata_order.orders");

        // 2. RPC 扣余额（XID 由 SeataXidConsumerFilter 自动透传）
        accountService.debit(order.getUserId(), money);
        System.out.println("💰 [OrderService] 已调用账户扣款 " + money + " 元");

        // 3. RPC 扣库存
        storageService.deduct(order.getProductCode(), count);
        System.out.println("📉 [OrderService] 已调用库存扣减 " + count + " 件");

        // 模拟下单失败，验证全局回滚
        if ("FAIL".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("模拟下单失败，全局事务回滚");
        }

        orderMapper.updateStatus(order.getUserId(), "SUCCESS");
        System.out.println("✅ [OrderService] 全局事务提交前业务完成");
        return "下单成功, XID=" + RootContext.getXID();
    }
}