package com.example.dubbo.order.api;

import com.example.dubbo.order.api.vo.OrderDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping("/order")
public interface OrderService {

    List<Map<String, Object>> getOrdersByUserId(Long userId);

    @GetMapping("/getOrder")
    Map<String, Object> getOrderById(@RequestParam("id") Long orderId);

    @PostMapping("/createOrder")
    String createOrder(@RequestBody OrderDTO order);

    /** 订单列表（角色不同返回不同：管理员返回全部，普通用户返回自己的） */
    @GetMapping("/list")
    List<Map<String, Object>> list();

    /**
     * 秒杀/削峰下单：请求写入 RocketMQ 后立即返回"排队中"，
     * 由消费者异步执行真正的下单（Seata 全局事务）。
     */
    @PostMapping("/quickOrder")
    Map<String, Object> quickOrder(@RequestBody OrderDTO order);

    /** 更新订单状态（管理员） */
    @PostMapping("/updateStatus")
    Map<String, Object> updateStatus(@RequestBody Map<String, Object> body);

    /** 删除订单（管理员） */
    @DeleteMapping("/delete/{id}")
    Map<String, Object> delete(@PathVariable("id") Long id);
}