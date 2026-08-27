package com.example.dubbo.api;

import com.example.dubbo.api.vo.OrderDTO;
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

    /**
     * 管理端查询全部订单：需要 ROLE_ADMIN（由 SecurityFilterV1 填充的 SecurityContext 校验）。
     */
    @GetMapping("/admin/allOrders")
    List<Map<String, Object>> getAllOrders();

}
