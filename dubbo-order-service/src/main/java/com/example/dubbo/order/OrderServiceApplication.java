package com.example.dubbo.order;

import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.context.ConfigManager;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestExtension;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.util.Collections;

@SpringBootApplication
@EnableDiscoveryClient
@EnableMethodSecurity  // 开启 @PreAuthorize 方法级权限控制（身份由 AuthContextProviderFilter 注入）
@MapperScan("com.example.dubbo.order.mapper")
public class OrderServiceApplication {

    @Value("${dubbo.protocol.port}")
    private String port;


    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
        System.out.println("OrderService Start, Dubbo Port: 20880");

        // 验证 SPI 是否加载
        ExtensionLoader<RestExtension> loader =
                FrameworkModel.defaultModel()
                        .getExtensionLoader(RestExtension.class);
        System.out.println("RestExtension extensions: " +
                loader.getSupportedExtensions());
    }

    @PostConstruct
    public void initFlowRules() {
        // 限流规则
        FlowRule rule = new FlowRule();
        // 资源名：接口全限定名 + ":" + 方法签名
        rule.setResource("com.example.dubbo.api.OrderService:getOrdersByUserId(java.lang.Long)");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS); // QPS 模式
        rule.setCount(3); // 阈值 = 3
        FlowRuleManager.loadRules(Collections.singletonList(rule));
    }
}
