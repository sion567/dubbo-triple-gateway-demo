package com.example.dubbo.order;

import com.example.dubbo.api.AccountService;
import com.example.dubbo.api.StorageService;
import com.example.dubbo.api.vo.OrderDTO;
import com.example.dubbo.order.mapper.OrderMapper;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.dubbo.config.annotation.DubboReference;

/**
 * 下单事务逻辑（TM）：REST 入口和 MQ 消费者共用。
 * 独立成 Bean 是因为 @GlobalTransactional 需要经过 Spring 代理生效，
 * 且 MQ 消费线程里没有 SecurityContext，鉴权必须留在各自的入口层。
 */
@org.springframework.stereotype.Service
public class OrderTxService {

    private final OrderMapper orderMapper;

    @DubboReference(protocol = "dubbo")
    private AccountService accountService;

    @DubboReference(protocol = "dubbo")
    private StorageService storageService;

    public OrderTxService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    /**
     * 跨 3 个库的分布式事务（Seata AT）：写订单 → RPC 扣余额 → RPC 扣库存。
     * status=FAIL 时抛异常触发全局回滚。
     */
    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    public String doCreate(OrderDTO order) {
        System.out.println("🚀 [OrderTxService] 全局事务开始, XID = " + RootContext.getXID());

        int count = order.getCount() == null ? 1 : order.getCount();
        double money = order.getPrice() == null ? 0.0 : order.getPrice() * count;

        orderMapper.insert(order.getUserId(), order.getProductCode(),
                order.getProduct(), count, money, "INIT");
        System.out.println("📦 [OrderTxService] 订单已写入 seata_order.orders");

        accountService.debit(order.getUserId(), money);
        System.out.println("💰 [OrderTxService] 已调用账户扣款 " + money + " 元");

        storageService.deduct(order.getProductCode(), count);
        System.out.println("📉 [OrderTxService] 已调用库存扣减 " + count + " 件");

        if ("FAIL".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("模拟下单失败，全局事务回滚");
        }

        orderMapper.updateStatus(order.getUserId(), "SUCCESS");
        System.out.println("✅ [OrderTxService] 全局事务提交前业务完成");
        return "下单成功, XID=" + RootContext.getXID();
    }
}
