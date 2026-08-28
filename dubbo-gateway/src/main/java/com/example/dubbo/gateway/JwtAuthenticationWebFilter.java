package com.example.dubbo.gateway;

import com.example.dubbo.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * 响应式 JWT 认证过滤器：网关作为第一道关卡验签，通过后把 Authentication 写入
 * Reactor Context（供 SecurityWebFilterChain 的授权判断使用）。
 *
 * 注意：不做任何 Header 改写——原始 Authorization token 原样透传给下游，
 * 下游 Provider 自行验签（信任边界是 JWT 本身，网关只是提前拦截点）。
 */
@Component
public class JwtAuthenticationWebFilter implements WebFilter {
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HEADER_AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // 交给授权层按"未认证"处理 -> 401
            return chain.filter(exchange);
        }

        Claims claims;
        try {
            claims = JwtUtil.parseToken(authHeader.substring(BEARER_PREFIX.length()));
        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(exchange, "Invalid or expired token: " + e.getMessage());
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        List<String> roles = claims.get(JwtUtil.CLAIM_ROLES, List.class);
        if (roles != null) {
            roles.forEach(r -> authorities.add(new SimpleGrantedAuthority(r)));
        }
        List<String> perms = claims.get(JwtUtil.CLAIM_PERMS, List.class);
        if (perms != null) {
            perms.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);

        return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"Unauthorized: " + message + "\"}";
        return exchange.getResponse().writeWith(Mono.just(
                exchange.getResponse().bufferFactory().wrap(body.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    }
}
