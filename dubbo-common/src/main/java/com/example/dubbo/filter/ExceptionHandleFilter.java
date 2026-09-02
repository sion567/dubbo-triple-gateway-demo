package com.example.dubbo.filter;

import com.example.dubbo.vo.ErrorResp;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
/**
 * 所有异常统一拦截转成 RpcResult，不用每个方法 try-catch
 */
@Activate(group = CommonConstants.PROVIDER)
public class ExceptionHandleFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        Result result = invoker.invoke(invocation);
        if (result.hasException()) {
            Throwable e = result.getException();
            // 业务异常 → 转成统一错误码
            if (e instanceof RpcBizException bizEx) {
                // 构造一个统一的错误响应对象返回
                return AsyncRpcResult.newDefaultAsyncResult(
                        new ErrorResp(bizEx.getBizCode(), bizEx.getMessage()),
                        invocation
                );
            }
            // 系统异常 → 打日志 + 返回通用错误
//            log.error("unexpected error on {}#{}",
//                    invocation.getServiceName(), invocation.getMethodName(), e);
            return AsyncRpcResult.newDefaultAsyncResult(
                    new ErrorResp(500, "系统繁忙"), invocation
            );
        }
        return result;
    }
}