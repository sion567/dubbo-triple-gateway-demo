package com.example.dubbo.seata;

import io.seata.core.context.RootContext;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;

/**
 * 提供端过滤器：从 attachment 取出 XID 并绑定到 RootContext，
 * 这样本服务的分支事务才能注册到同一个全局事务下，异常时一起回滚。
 */
@Activate(group = CommonConstants.PROVIDER)
public class SeataXidProviderFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        String xid = invocation.getAttachment(SeataXidConsumerFilter.XID_ATTACHMENT_KEY);
        if (xid == null) {
            return invoker.invoke(invocation);
        }
        RootContext.bind(xid);
        try {
            return invoker.invoke(invocation);
        } finally {
            RootContext.unbind();
        }
    }
}
