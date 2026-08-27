package com.example.dubbo.seata;

import io.seata.core.context.RootContext;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;

/**
 * 消费端过滤器：把当前全局事务的 XID 塞进 RPC attachment，随请求传给提供方。
 * Seata 官方只提供了 dubbo 2.x 的适配（io.seata:seata-dubbo），
 * Dubbo3 下按官方推荐方式自行透传 XID。
 */
@Activate(group = CommonConstants.CONSUMER)
public class SeataXidConsumerFilter implements Filter {

    public static final String XID_ATTACHMENT_KEY = "seata-xid";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        String xid = RootContext.getXID();
        if (xid != null) {
            invocation.setAttachment(XID_ATTACHMENT_KEY, xid);
        }
        return invoker.invoke(invocation);
    }
}
