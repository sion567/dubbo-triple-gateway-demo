package com.example.dubbo.security;

/**
 * 保存当前线程正在处理的原始 Authorization token，
 * 供 ContextPropagationConsumerFilter 在发起下游 RPC 时继续透传（链式调用不丢身份）。
 * 由 ContextPropagationProviderFilter set/clear，生命周期与一次请求对齐。
 */
public final class RpcAuthHolder {

    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();

    private RpcAuthHolder() {
    }

    public static void set(String token) {
        TOKEN.set(token);
    }

    public static String get() {
        return TOKEN.get();
    }

    public static void clear() {
        TOKEN.remove();
    }
}
