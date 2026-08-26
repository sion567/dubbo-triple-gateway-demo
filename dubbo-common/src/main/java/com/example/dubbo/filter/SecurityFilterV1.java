package com.example.dubbo.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Activate
public class SecurityFilterV1 implements Filter, RestExtension {

    public SecurityFilterV1() {
        System.out.println("------SecurityFilterV1 constructor------");
    }

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String token = resolveToken(httpRequest);
        if (StringUtils.hasText(token) && validateToken(token)) {
            String username = getUsernameFromToken(token);
            List<SimpleGrantedAuthority> authorities =
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // ✅ 放行请求，并立即返回
            chain.doFilter(request, response);
            return;
        }

        // Token 无效，返回 401
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResponse.setContentType("application/json");
        httpResponse.getWriter().write("{\"code\":401,\"message\":\"Unauthorized: Invalid token\"}");
    }

    @Override
    public String[] getPatterns() {
        // 如果只想拦截 /order 相关路径，可以尝试精确匹配
        // 或者确认 Dubbo 是否支持 /* 通配
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

    private boolean validateToken(String token) {
        // TODO: 生产环境替换为真实 JWT 验证逻辑
        return "valid-mock-token".equals(token);
    }

    private String getUsernameFromToken(String token) {
        // TODO: 生产环境从 JWT Claims 中提取用户名
        return "dubbo_user";
    }
}
