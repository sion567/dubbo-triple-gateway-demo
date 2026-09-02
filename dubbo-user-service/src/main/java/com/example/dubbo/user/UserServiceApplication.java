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

//Invoke-WebRequest http://localhost:22222/ls -UseBasicParsing | Select-Object -ExpandProperty Content
//As Provider side:
//        +----------------------------------------------------------------------------------+---------------------+
//        |                               Provider Service Name                              |         PUB         |
//        +----------------------------------------------------------------------------------+---------------------+
//        |                     com.example.dubbo.user.api.AccountService                    |nacos-A(Y)/nacos-A(Y)|
//        +----------------------------------------------------------------------------------+---------------------+
//        |                      com.example.dubbo.user.api.UserService                      |nacos-A(Y)/nacos-A(Y)|
//        +----------------------------------------------------------------------------------+---------------------+
//        |                     com.example.dubbo.user.api.UserServiceApi                    |nacos-A(Y)/nacos-A(Y)|
//        +----------------------------------------------------------------------------------+---------------------+
//        |                       com.example.proto.AccountProtoService                      |nacos-A(Y)/nacos-A(Y)|
//        +----------------------------------------------------------------------------------+---------------------+
//        |                        com.example.proto.UserProtoService                        |nacos-A(Y)/nacos-A(Y)|
//        +----------------------------------------------------------------------------------+---------------------+
//        |DubboInternal - dubbo-user-service/org.apache.dubbo.metadata.MetadataService:1.0.0|                     |
//        +----------------------------------------------------------------------------------+---------------------+
//As Consumer side:
//        +---------------------+---+
//        |Consumer Service Name|NUM|
//        +---------------------+---+