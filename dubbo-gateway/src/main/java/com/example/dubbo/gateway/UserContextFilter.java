package com.example.dubbo.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class UserContextFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret:12345678901234567890123456789012}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        ServerHttpRequest.Builder mutated = exchange.getRequest().mutate();

        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                var claims = Jwts.parser()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(auth.substring(7))
                        .getBody();
                mutated.header("X-User-Id", claims.getSubject());
                System.out.println("X-User-Id" + claims.getSubject());
                Object roles = claims.get("roles");
                if (roles instanceof List<?> r) {
                    mutated.header("X-User-Roles", String.join(",", r.stream().map(Object::toString).toList()));
                }
            } catch (Exception e) {
                // 解析失败：不注入 Header，下游自己 401；或者在此直接 return 401
            }
        }
        return chain.filter(exchange.mutate().request(mutated.build()).build());
    }

    @Override
    public int getOrder() {
        return -1;   // 必须在 Routing 类 Filter 之前
    }
}