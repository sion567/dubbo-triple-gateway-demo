package com.example.dubbo.order;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
        System.out.println("OrderService Start, Dubbo Port: 20881");

        // 验证 SPI 是否加载
        ExtensionLoader<RestExtension> loader =
                FrameworkModel.defaultModel()
                        .getExtensionLoader(RestExtension.class);
        System.out.println("RestExtension extensions: " +
                loader.getSupportedExtensions());

    }
}
