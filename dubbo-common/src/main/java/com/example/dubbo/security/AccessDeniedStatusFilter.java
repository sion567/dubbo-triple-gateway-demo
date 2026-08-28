package com.example.dubbo.security;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

/**
 * 鉴权异常映射（默认都会被 tri rest 包装成 500）：
 * - AccessDeniedException（含 AuthorizationDeniedException）→ 403，已登录但权限不足
 * - AuthenticationCredentialsNotFoundException → 401，Provider 侧 SecurityContext 为空，
 *   说明 token 没透传到或验签失败（透传链路断了），不是权限问题
 */
@Activate(group = CommonConstants.PROVIDER, order = 10000)
public class AccessDeniedStatusFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        Result result = invoker.invoke(invocation);
        if (!(result instanceof AsyncRpcResult asyncResult)) {
            translate(result);
            return result;
        }
        return asyncResult.whenCompleteWithContext((r, t) -> translate(r));
    }

    private void translate(Result r) {
        Throwable ex = r.getException();
        if (ex == null) {
            return;
        }
        Throwable cause = unwrap(ex);
        if (cause instanceof AuthenticationCredentialsNotFoundException) {
            r.setException(new HttpStatusException(401, "Unauthorized: no Authentication "
                    + "in SecurityContext — token missing or not propagated to provider", cause));
            System.out.println("⛔ [AccessDeniedStatusFilter] 401: token 未透传到 Provider，"
                    + "检查 Authorization attachment / JWT_SECRET 是否一致");
        } else if (cause instanceof AccessDeniedException denied) {
            r.setException(new HttpStatusException(403, "Forbidden: "
                    + denied.getMessage(), denied));
            System.out.println("⛔ [AccessDeniedStatusFilter] 403: " + r.getException().getMessage());
        }
    }

    private Throwable unwrap(Throwable ex) {
        while (ex.getCause() != null && ex != ex.getCause()) {
            ex = ex.getCause();
        }
        return ex;
    }
}
