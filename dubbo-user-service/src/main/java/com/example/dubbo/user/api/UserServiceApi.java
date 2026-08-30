package com.example.dubbo.user.api;

/**
 * 精简版用户服务接口，仅暴露 order-service 需要的 getUserIdByUsername 方法。
 * 避免 order-service 依赖整个 user-service 模块。
 */
public interface UserServiceApi {
    /** 根据用户名查 userId，供 order-service RPC 调用 */
    Long getUserIdByUsername(String username);
}