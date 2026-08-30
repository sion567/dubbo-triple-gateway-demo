package com.example.dubbo.user.api;

import com.example.dubbo.user.api.vo.LoginRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@RequestMapping("/auth")
public interface AccountService {
    /**
     * 登录：校验用户名密码，签发 JWT（含角色）。成功返回 token，失败返回 code=401。
     */
    @PostMapping("/login")
    Map<String, Object> login(@RequestBody LoginRequest request);
}