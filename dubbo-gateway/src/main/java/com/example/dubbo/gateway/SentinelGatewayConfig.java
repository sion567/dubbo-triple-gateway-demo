package com.example.dubbo.gateway;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class SentinelGatewayConfig {

    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;

    public SentinelGatewayConfig(ObjectProvider<List<ViewResolver>> viewResolversProvider,
                                 ServerCodecConfigurer serverCodecConfigurer) {
        this.viewResolvers = viewResolversProvider.getIfAvailable(Collections::emptyList);
        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    // 自定义限流异常处理器
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
        return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }

    // 初始化限流规则
    @PostConstruct
    public void initGatewayRules() {
        // 1. 定义 API 分组 (把 /user/** 和 /order/** 分别定义为一个资源)
        Set<ApiDefinition> apiDefinitions = new HashSet<>();

        // User API 分组
        ApiDefinition userApi = new ApiDefinition("user_api")
                .setPredicateItems(new HashSet<ApiPredicateItem>() {{
                    add(new ApiPathPredicateItem().setPattern("/user/**"));
                }});
        apiDefinitions.add(userApi);

        // Order API 分组
        ApiDefinition orderApi = new ApiDefinition("order_api")
                .setPredicateItems(new HashSet<ApiPredicateItem>() {{
                    add(new ApiPathPredicateItem().setPattern("/order/**"));
                }});
        apiDefinitions.add(orderApi);

        GatewayApiDefinitionManager.loadApiDefinitions(apiDefinitions);

        // 2. 设置限流规则
        Set<GatewayFlowRule> rules = new HashSet<>();

        // User API: QPS = 10
        GatewayFlowRule userRule = new GatewayFlowRule("user_api")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setGrade(1)  // 1 = QPS, 0 = 线程数
                .setCount(10)
                .setIntervalSec(1);
        rules.add(userRule);

        // Order API: QPS = 5
        GatewayFlowRule orderRule = new GatewayFlowRule("order_api")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setGrade(1)
                .setCount(5)
                .setIntervalSec(1);
        rules.add(orderRule);

        GatewayRuleManager.loadRules(rules);

        System.out.println("✅ Sentinel Gateway 限流规则已加载:");
        System.out.println("   /user/** -> QPS=10");
        System.out.println("   /order/** -> QPS=5");
    }
}
