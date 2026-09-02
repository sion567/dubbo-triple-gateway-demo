package com.example.dubbo.filter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.RouterFactory;

@Activate
public class GrayRouterFactory implements RouterFactory {
    @Override
    public Router getRouter(URL url) {
        return new GrayRouter();
    }
}