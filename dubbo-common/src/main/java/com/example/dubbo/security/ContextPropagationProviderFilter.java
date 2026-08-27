package com.example.dubbo.security;

import io.jsonwebtoken.Claims;
import io.seata.core.context.RootContext;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一上下文透传（提供端）：还原 XID 和身份。
 *
 * 1. XID：绑定 RootContext（Seata 分支事务注册进同一全局事务）
 * 2. 身份：attachment 里的原始 JWT 自行验签（信任边界是 JWT 本身，不是网关），
 *    验签通过重建 SecurityContext（roles + perms 都作为 authority），
 *    同时把原始 token 存入 RpcAuthHolder，供本服务继续调用下游时透传（链式不丢身份）
 *
 * token 的两个来源（都汇聚到 ctx-authorization）：
 * - 网关转发：Authorization HTTP Header 经 Triple 自动转为小写 attachment
 * - 上游服务 RPC：ContextPropagationConsumerFilter 主动塞入
 */
@Activate(group = CommonConstants.PROVIDER, order = -10000)
public class ContextPropagationProviderFilter implements Filter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        // ---- XID ----
        String xid = invocation.getAttachment(ContextPropagationConsumerFilter.XID_KEY);
        boolean xidBound = false;
        if (xid != null) {
            RootContext.bind(xid);
            xidBound = true;
        }

        // ---- 身份 ----
        String token = resolveToken(invocation);
        boolean authBound = false;
        if (token != null) {
            try {
                Claims claims = JwtUtil.parseToken(token);
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
                org.springframework.security.core.context.SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
                RpcAuthHolder.set(token); // 供本服务再调下游时透传
                authBound = true;
                System.out.println("🔑 [ContextPropagation] 认证通过: user=" + claims.getSubject()
                        + ", authorities=" + authorities);
            } catch (Exception e) {
                System.err.println("⚠️ [ContextPropagation] JWT 验签失败，按匿名处理: " + e.getMessage());
            }
        }

        try {
            return invoker.invoke(invocation);
        } finally {
            if (authBound) {
                RpcAuthHolder.clear();
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
            }
            if (xidBound) {
                RootContext.unbind();
            }
        }
    }

    private String resolveToken(Invocation invocation) {
        String raw = invocation.getAttachment(ContextPropagationConsumerFilter.AUTH_KEY);
        if (raw == null) {
            // 网关直转场景：HTTP Authorization header 自动转成的 attachment
            raw = RpcContext.getServerAttachment()
                    .getAttachment(ContextPropagationConsumerFilter.AUTH_KEY);
        }
        if (raw == null) {
            raw = RpcContext.getServerAttachment().getAttachment("authorization");
        }
        if (raw != null && raw.startsWith(BEARER_PREFIX)) {
            return raw.substring(BEARER_PREFIX.length());
        }
        return raw != null && !raw.isBlank() ? raw : null;
    }
}
