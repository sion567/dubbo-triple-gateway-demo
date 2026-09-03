package com.example.dubbo.user.api.vo;

import org.apache.dubbo.remoting.http12.rest.Schema;

import java.io.Serializable;

@Schema(title = "登录请求", description = "用户登录请求参数，包含用户名和密码")
public class LoginRequest implements Serializable {
    @Schema(title = "用户名", example = "Tom")
    private String username;
    @Schema(title = "密碼", example = "123456")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
