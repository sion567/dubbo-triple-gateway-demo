package com.example.dubbo.security;

import io.seata.core.context.RootContext;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;

/**
 * 统一上下文透传（消费端）：一次 RPC 同时携带 Seata XID 和身份 token。
 *
 * - XID：当前线程若在全局事务中，透传给下游（下游分支事务挂同一全局事务）
 * - token：原始 Authorization Bearer token 透传，下游自行验签
 *   （JWT 即信任边界：绕过网关直连也打不穿，无需明文 X-User-Info 和内部签名）
 */
@Activate(group = CommonConstants.CONSUMER)
public class ContextPropagationConsumerFilter implements Filter {

    public static final String XID_KEY = "ctx-xid";
    public static final String AUTH_KEY = "ctx-authorization";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        String xid = RootContext.getXID();
        if (xid != null) {
            invocation.setAttachment(XID_KEY, xid);
        }
        String token = RpcAuthHolder.get();
        if (token != null) {
            invocation.setAttachment(AUTH_KEY, token);
        }
        return invoker.invoke(invocation);
    }
}
