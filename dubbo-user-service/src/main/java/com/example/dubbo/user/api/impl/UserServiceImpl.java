package com.example.dubbo.user.api.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.dubbo.user.api.UserService;
import com.example.dubbo.user.mapper.AccountMapper;
import com.example.dubbo.user.mapper.UserMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;

@DubboService
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final AccountMapper accountMapper;

    public UserServiceImpl(UserMapper userMapper, AccountMapper accountMapper) {
        this.userMapper = userMapper;
        this.accountMapper = accountMapper;
    }

    @Override
    @SentinelResource(value = "getUserInfo")
    public Map<String, Object> getUserInfo(Long userId) {
        System.out.println("📨 [UserService] getUserInfo(" + userId + ")");
        Map<String, Object> user = userMapper.selectById(userId);
        if (user == null) {
            return Map.of("userId", userId, "name", "未知用户");
        }
        return user;
    }

    @Override
    @SentinelResource(value = "getUserWithOrders", blockHandler = "handleGetUserWithOrdersBlock")
    public Map<String, Object> getUserWithOrders(Long userId) {
        System.out.println("📨 [UserService] getUserWithOrders(" + userId + ")");
        Map<String, Object> user = userMapper.selectById(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        if (user == null) {
            result.put("name", "未知用户");
        } else {
            result.put("name", user.get("name"));
        }
        return result;
    }

    public Map<String, Object> handleGetUserWithOrdersBlock(Long userId, BlockException ex) {
        System.out.println("⚠️ [UserService] getUserWithOrders 被限流! userId=" + userId);
        return Map.of("userId", userId, "error", "服务繁忙，请稍后重试");
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> list() {
        List<Map<String, Object>> users = userMapper.selectAll();
        List<Map<String, Object>> accounts = accountMapper.findAll();
        for (Map<String, Object> user : users) {
            Long userId = (Long) user.get("userid");
            if (userId == null) {
                continue;
            }
            for (Map<String, Object> acc : accounts) {
                if (userId.equals(acc.get("user_id"))) {
                    user.put("username", acc.get("username"));
                    user.put("roles", Arrays.asList(((String) acc.get("roles")).split(",")));
                    String permsStr = (String) acc.getOrDefault("perms", "");
                    user.put("perms", permsStr.isBlank() ? List.of() : Arrays.asList(permsStr.split(",")));
                    break;
                }
            }
        }
        return Map.of("code", 0, "users", users);
    }

    @Override
    public Long getUserIdByUsername(String username) {
        Map<String, Object> account = accountMapper.findByUsername(username);
        if (account == null) return null;
        return (Long) account.get("user_id");
    }
}