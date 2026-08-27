package com.example.dubbo.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;


@SpringBootApplication
@EnableDiscoveryClient
@EnableMethodSecurity  // @PreAuthorize 生效（身份由 ContextPropagationProviderFilter 重建）
@MapperScan("com.example.dubbo.user.mapper")
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
        System.out.println("UserService Start, Dubbo Port: 20882");
    }
}