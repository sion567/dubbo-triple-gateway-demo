package com.example.dubbo.filter;

import jakarta.servlet.*;
import jakarta.servlet.Filter;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import java.io.IOException;

@Activate(group = CommonConstants.PROVIDER, order = 1000)
public class AuditProviderFilter implements Filter, BaseFilter.Listener {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
//        AuditContext.clear();
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
//        AuditContext.clear();
    }

//    @Override
//    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
//        // 前置：从 ServerAttachment 取 x-user-id 塞进 AuditContext
//        String op = RpcContext.getServerAttachment().getAttachment("x-user-id");
////        AuditContext.set(new AuditInfo(op, ...));
//        try {
//            Result result = invoker.invoke(invocation);
//            // 同步结果顺手处理；异步结果靠 Listener
//            return result;
//        } finally {
//            // 注意异步场景下 finally 里别急着 clear，
//            // 用 Listener.onResponse/onError 里清
//        }
//    }

}
