package com.example.dubbo.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关统一认证（标准 Spring Security WebFlux）：
 * - 白名单精确到路径（/user/login），不做 /user/** 前缀放行
 * - 其余请求必须认证（JwtAuthenticationWebFilter 验签后注入 Authentication）
 * - 认证失败统一 401 JSON；授权判断留给下游 @PreAuthorize 做细粒度控制
 * - 原始 Authorization 头原样透传下游（网关不做任何改写）
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtAuthenticationWebFilter jwtAuthenticationWebFilter;

    public SecurityConfig(JwtAuthenticationWebFilter jwtAuthenticationWebFilter) {
        this.jwtAuthenticationWebFilter = jwtAuthenticationWebFilter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)          // 无状态 API
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchange -> exchange
                        // 精确白名单：只放登录和探活
                        .pathMatchers("/auth/login", "/actuator/**").permitAll()
                        .anyExchange().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, e) -> unauthorized(exchange.getResponse())))
                // 关键：JwtAuthenticationWebFilter 必须在 AuthorizationWebFilter 之前执行，
                // 否则 authorizeExchange 检查时 SecurityContext 还是空的，直接返回 401
                .addFilterBefore(jwtAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHORIZATION)
                .build();
    }

    /**
     * CORS：sample 放开所有来源（不允许凭据）。生产按实际前端域名收窄 allowedOriginPatterns。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private Mono<Void> unauthorized(org.springframework.http.server.reactive.ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"Unauthorized: Missing or invalid token\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
