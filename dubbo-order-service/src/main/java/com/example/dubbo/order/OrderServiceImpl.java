package com.example.dubbo.order;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.dubbo.api.OrderService;
import com.example.dubbo.api.vo.OrderDTO;
import com.example.dubbo.order.mapper.OrderMapper;
import org.apache.dubbo.config.annotation.DubboService;
import java.util.*;

@DubboService
public class OrderServiceImpl implements OrderService {

    // 模拟订单数据（查询演示用）
    private final Map<Long, List<Map<String, Object>>> orderDb = new HashMap<>();

    private final OrderMapper orderMapper;
    private final OrderTxService orderTxService;
    private final QuickOrderProducer quickOrderProducer;

    public OrderServiceImpl(OrderMapper orderMapper, OrderTxService orderTxService,
                            QuickOrderProducer quickOrderProducer) {
        this.orderMapper = orderMapper;
        this.orderTxService = orderTxService;
        this.quickOrderProducer = quickOrderProducer;
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
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public String createOrder(OrderDTO order) {
        // 事务逻辑在 OrderTxService（REST 与 MQ 消费者共用）
        return orderTxService.doCreate(order);
    }

    /**
     * 秒杀/削峰下单：请求进 RocketMQ 立即返回，消费者异步执行真正的下单。
     */
    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('USER')")
    public Map<String, Object> quickOrder(OrderDTO order) {
        String key = quickOrderProducer.send(order);
        System.out.println("⚡ [OrderService] 下单请求已入队: key=" + key);
        return Map.of("code", 0, "message", "排队中，请稍后刷新查看结果", "queueKey", key);
    }

    /**
     * 管理端接口：方法级权限演示。
     * 身份由 ContextPropagationProviderFilter 验签原始 JWT 后重建进 SecurityContext。
     *
     * 注：@RequiresPermissions 是 Shiro 的注解；Spring Security 的等价写法是
     * @PreAuthorize("hasAuthority('...')")（角色用 hasRole/hasAnyRole），本工程用后者。
     * 权限点与若依风格一致：system:dept:add
     */
    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('system:dept:add')")
    public List<Map<String, Object>> getAllOrders() {
        System.out.println("👮 [OrderService] getAllOrders 调用者="
                + org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication().getName());
        // 查询演示用 mock 全量订单 + DB 里的订单
        List<Map<String, Object>> all = new ArrayList<>();
        orderDb.values().forEach(all::addAll);
        all.addAll(orderMapper.selectAll());
        return all;
    }

    /** 前端订单列表：真实 DB 订单 */
    @Override
    public List<Map<String, Object>> list() {
        return orderMapper.selectAll();
    }

    /** 前端更新订单状态（管理员） */
    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> updateStatus(Map<String, Object> body) {
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        String status = String.valueOf(body.get("status"));
        int rows = orderMapper.updateStatusById(id, status);
        return rows > 0 ? Map.of("code", 0, "message", "ok") : Map.of("code", 404, "message", "订单不存在");
    }

    /** 前端删除订单（管理员） */
    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> delete(Long id) {
        int rows = orderMapper.deleteById(id);
        return rows > 0 ? Map.of("code", 0, "message", "ok") : Map.of("code", 404, "message", "订单不存在");
    }
}