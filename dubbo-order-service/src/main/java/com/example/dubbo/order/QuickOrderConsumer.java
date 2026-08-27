package com.example.dubbo.order;

import com.example.dubbo.api.vo.OrderDTO;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 秒杀订单消费者：从 RocketMQ 拉取排队中的下单请求，异步执行真正的下单
 * （OrderTxService#doCreate 内含 Seata 全局事务）。
 * 流量高峰时请求只堆积在 Broker 上，DB 与下游服务的压力由消费速率决定 —— 削峰填谷。
 */
@Component
@RocketMQMessageListener(topic = QuickOrderProducer.TOPIC, consumerGroup = "order-quick-group")
public class QuickOrderConsumer implements RocketMQListener<OrderDTO> {

    private final OrderTxService orderTxService;

    public QuickOrderConsumer(OrderTxService orderTxService) {
        this.orderTxService = orderTxService;
    }

    @Override
    public void onMessage(OrderDTO order) {
        System.out.println("📬 [QuickOrderConsumer] 收到排队订单: user=" + order.getUserId()
                + ", product=" + order.getProductCode() + "x" + order.getCount());
        String result = orderTxService.doCreate(order);
        System.out.println("✅ [QuickOrderConsumer] 异步下单完成: " + result);
        // 消费失败（余额不足/库存不足/回滚）抛异常即可：RocketMQ 会重试，
        // 达到最大重试次数后进入死信队列（%DLQ%order-quick-group），可人工处理
    }
}
