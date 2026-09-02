package com.example.dubbo.order.api.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.dubbo.order.OrderTxService;
import com.example.dubbo.order.QuickOrderProducer;
import com.example.dubbo.order.UserServiceClient;
import com.example.dubbo.order.api.OrderService;
import com.example.dubbo.order.api.vo.OrderDTO;
import com.example.dubbo.order.mapper.OrderMapper;
import com.example.proto.GetUsernamesByUserIdsRequest;
import org.apache.dubbo.config.annotation.DubboService;
import java.util.*;

@DubboService
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderTxService orderTxService;
    private final QuickOrderProducer quickOrderProducer;

    private final UserServiceClient userServiceClient;

    public OrderServiceImpl(OrderMapper orderMapper, OrderTxService orderTxService,
                            QuickOrderProducer quickOrderProducer,
                            UserServiceClient userServiceClient) {
        this.orderMapper = orderMapper;
        this.orderTxService = orderTxService;
        this.quickOrderProducer = quickOrderProducer;

        this.userServiceClient = userServiceClient;
    }



    @Override
    @SentinelResource(value = "getOrdersByUserId", blockHandler = "handleGetOrdersBlock")
    public List<Map<String, Object>> getOrdersByUserId(Long userId) {
        System.out.println("📨 [OrderService] getOrdersByUserId(" + userId + ")");
        try { Thread.sleep(50); } catch (InterruptedException e) {}
        return orderMapper.selectByUserId(userId);
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
        Map<String, Object> order = orderMapper.selectById(orderId);
        return order != null ? order : Map.of("error", "订单不存在");
    }

    public Map<String, Object> handleGetOrderBlock(Long orderId, BlockException ex) {
        System.out.println("⚠️ [OrderService] getOrderById 被限流! orderId=" + orderId);
        return Map.of("error", "服务繁忙，请稍后重试");
    }

    /**
     * 下单：跨 3 个库的分布式事务（Seata AT 模式）。
     * 1. 本服务：t_orders 表插入订单（分支事务，seata_order 库）
     * 2. user-service：t_user 表扣余额（分支事务，seata_account 库）
     * 3. storage-service：t_storage 表扣库存（分支事务，seata_storage 库）
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
     * 前端订单列表：根据角色返回，admin 返回全部，普通用户返回自己的。
     * 角色信息由 ContextPropagationProviderFilter 验签 JWT 后写入 SecurityContext。
     */
    @Override
    public List<Map<String, Object>> list() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Collections.emptyList();
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("order:manage:list"));
        if (isAdmin) {
            System.out.println("📋 [OrderService] list: 管理员，返回全部订单");
            return withUsernames(orderMapper.selectAll());
        }
        String username = auth.getName();
        Long userId = userServiceClient.getUserServiceApi().getUserIdByUsername(username);
        if (userId == null) {
            return Collections.emptyList();
        }
        System.out.println("📋 [OrderService] list: userId=" + userId + "，返回自己的订单");
        return withUsernames(orderMapper.selectByUserId(userId));
    }

    /**
     * 给订单列表补 username 字段（H2 小写化后订单行里的 key 是 "userid"）。
     * 内部 RPC 走 IDL 模式（UserProtoService，Triple/protobuf）：去重后一次批量调用，
     * 返回的 names 与入参 user_ids 同序，空串表示用户不存在。
     */
    private List<Map<String, Object>> withUsernames(List<Map<String, Object>> orders) {
        // 保持去重后的出现顺序，便于与 proto 返回的 names 按位置对齐
        Map<Long, Integer> indexById = new LinkedHashMap<>();
        for (Map<String, Object> order : orders) {
            Object uid = order.get("userid");
            if (uid != null) {
                indexById.putIfAbsent(Long.valueOf(String.valueOf(uid)), indexById.size());
            }
        }
        if (indexById.isEmpty()) {
            return orders;
        }
        GetUsernamesByUserIdsRequest request = GetUsernamesByUserIdsRequest.newBuilder()
                .addAllUserIds(indexById.keySet().stream().map(Long::longValue).toList())
                .build();
        List<String> names;
        try {
            names = userServiceClient.getUserProtoService().getUsernamesByUserIds(request).getNamesList();
        } catch (Exception e) {
            System.err.println("❌ [OrderService] 批量查询用户名失败: " + e.getMessage());
            return orders;
        }
        for (Map<String, Object> order : orders) {
            Object uid = order.get("userid");
            if (uid == null) continue;
            Integer idx = indexById.get(Long.valueOf(String.valueOf(uid)));
            String name = idx != null && idx < names.size() ? names.get(idx) : "";
            if (!name.isBlank()) {
                order.put("username", name);
            }
        }
        return orders;
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