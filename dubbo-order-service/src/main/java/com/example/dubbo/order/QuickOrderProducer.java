package com.example.dubbo.order;

import com.example.dubbo.api.vo.OrderDTO;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 秒杀订单生产者：把下单请求发进 RocketMQ，立即返回（排队中）。
 */
@Component
public class QuickOrderProducer {

    public static final String TOPIC = "order-quick-topic";

    private final RocketMQTemplate rocketMQTemplate;

    public QuickOrderProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    public String send(OrderDTO order) {
        // key 用 userId+时间戳，Broker 端可按 key 查询/去重
        String key = order.getUserId() + "-" + System.currentTimeMillis();
        rocketMQTemplate.syncSend(TOPIC,
                MessageBuilder.withPayload(order).setHeader("KEYS", key).build());
        return key;
    }
}
