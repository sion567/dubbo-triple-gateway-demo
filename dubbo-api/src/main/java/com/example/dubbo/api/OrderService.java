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
     * 管理端查询全部订单：需要 ROLE_ADMIN（由 ContextPropagationProviderFilter 重建的 SecurityContext 校验）。
     */
    @GetMapping("/admin/allOrders")
    List<Map<String, Object>> getAllOrders();

    /** 订单列表（登录即可看） */
    @GetMapping("/list")
    List<Map<String, Object>> list();

    /** 更新订单状态（管理员） */
    @PostMapping("/updateStatus")
    Map<String, Object> updateStatus(@RequestBody Map<String, Object> body);

    /** 删除订单（管理员） */
    @DeleteMapping("/delete/{id}")
    Map<String, Object> delete(@PathVariable("id") Long id);

}
