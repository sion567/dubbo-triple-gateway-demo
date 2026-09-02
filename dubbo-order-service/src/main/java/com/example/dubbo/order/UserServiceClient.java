package com.example.dubbo.order;

import com.example.dubbo.user.api.UserServiceApi;
import com.example.proto.UserProtoService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

@Component
public class UserServiceClient {

    @DubboReference(check = true) //check = true 是高危配置，如果 Provider 启动慢于 Consumer，会直接导致 Consumer 启动失败，进而订阅流程中断。
    private UserServiceApi userServiceApi;

    @DubboReference(check = false)
    private UserProtoService userProtoService;

    public UserServiceApi getUserServiceApi() {
        return userServiceApi;
    }

    public UserProtoService getUserProtoService() {
        return userProtoService;
    }
}
