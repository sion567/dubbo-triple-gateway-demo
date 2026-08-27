package com.example.dubbo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("✅ Gateway 已启动，端口: 7788");
        System.out.println("   /user/** -> User Service (Triple)");
        System.out.println("   /order/** -> Order Service (Triple)");
        System.out.println("✅ Sentinel Gateway Adapter 已生效");
    }
}