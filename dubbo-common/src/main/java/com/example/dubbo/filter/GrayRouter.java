package com.example.dubbo.filter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.cluster.Router;

import java.util.List;
import java.util.stream.Collectors;

//Router 实现（Dubbo3 推荐实现 StateRouter，这里用老 Router 示意）
public class GrayRouter implements Router {
    @Override
    public URL getUrl() {
        return null;
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        String tag = RpcContext.getClientAttachment().getAttachment("x-dubbo-tag");
        if ("gray".equals(tag)) {
            return invokers.stream()
                    .filter(inv -> "gray".equals(inv.getUrl().getParameter("tag")))
                    .collect(Collectors.toList());
        }
        return invokers;
    }

    @Override
    public boolean isRuntime() {
        return false;
    }

    @Override
    public boolean isForce() {
        return false;
    }

    @Override public int getPriority() { return 0; }
}