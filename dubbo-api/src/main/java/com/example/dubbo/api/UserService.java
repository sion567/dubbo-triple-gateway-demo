package com.example.dubbo.api;

import com.example.dubbo.api.vo.LoginRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@RequestMapping("/user")
public interface UserService {
    @GetMapping("/getUser/{id}")
    Map<String, Object> getUserInfo(@PathVariable("id") Long userId);
    @GetMapping("/getOrder/{id:\\d+}")
    Map<String, Object> getUserWithOrders(@PathVariable("id") Long userId);

    /**
     * 登录：校验用户名密码，签发 JWT（含角色）。成功返回 token，失败返回 code=401。
     */
    @PostMapping("/login")
    Map<String, Object> login(@RequestBody LoginRequest request);

    /** 用户列表（含角色与余额，管理员） */
    @GetMapping("/list")
    Map<String, Object> list();
}
