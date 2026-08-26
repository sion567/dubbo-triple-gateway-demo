package com.example.dubbo.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@RequestMapping("/user")
public interface UserService {
    @GetMapping("/getUser/{id}")
    Map<String, Object> getUserInfo(@PathVariable("id") Long userId);
    @GetMapping("/getOrder/{id:\\d+}")
    Map<String, Object> getUserWithOrders(@PathVariable("id") Long userId);
}
