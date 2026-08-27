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

/**
 * 403 映射：@PreAuthorize 拒绝时抛出的 AccessDeniedException（含 Spring Security 6 的
 * AuthorizationDeniedException 子类）默认会被 tri rest 包装成 500，
 * 本过滤器把它翻译成携带 403 状态码的 HttpStatusException，让客户端拿到明确的 403。
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
        if (unwrap(ex) instanceof AccessDeniedException denied) {
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
