package com.example.dubbo.filter;

import com.example.dubbo.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

/**
 * Dubbo tri rest 安全过滤器（RestExtension SPI 自动激活）：
 * 1. 从 Authorization: Bearer <jwt> 取 token
 * 2. 验签 + 校验过期（JwtUtil，HS256）
 * 3. 用 claims 里的用户名和角色填充 SecurityContext
 * 4. 缺失/无效/过期一律 401，不放行
 */
@Activate
public class SecurityFilterV1 implements Filter, RestExtension {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String token = resolveToken(httpRequest);
        if (!StringUtils.hasText(token)) {
            unauthorized(httpResponse, "Missing token");
            return;
        }

        Claims claims;
        try {
            claims = JwtUtil.parseToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            unauthorized(httpResponse, "Invalid or expired token: " + e.getMessage());
            return;
        }

        // 角色来自 token 的 roles claim；无角色则按匿名已认证用户处理
        List<String> roles = claims.get(JwtUtil.CLAIM_ROLES, List.class);
        List<SimpleGrantedAuthority> authorities = roles == null ? List.of()
                : roles.stream().map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r)).toList();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        System.out.println("🔐 [SecurityFilterV1] 认证通过: user=" + claims.getSubject()
                + ", roles=" + roles + ", path=" + httpRequest.getRequestURI());

        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    public String[] getPatterns() {
        // 只拦截 /order/**；/user/** 不拦（登录接口必须开放）
        return new String[]{"/order/*"};
    }

    @Override
    public int getPriority() {
        return -1000;
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HEADER_AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        System.out.println("🚫 [SecurityFilterV1] 拒绝访问: " + message);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":401,\"message\":\"Unauthorized: " + message + "\"}");
    }
}
