package com.example.dubbo.filter;

import jakarta.servlet.Filter;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.spring.extension.SpringExtensionInjector;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestExtension;

import java.util.function.Supplier;

@Activate
public class SecurityFilter implements Supplier<Filter>, RestExtension {
    private Filter filter;

    public SecurityFilter() {
        System.out.println("------SecurityFilter constructor------");
    }

    public SecurityFilter(FrameworkModel frameworkModel) {
        SpringExtensionInjector injector = SpringExtensionInjector.get(frameworkModel.defaultApplication());
        this.filter = injector.getInstance(JwtSecurityFilter.class, null);

//        SpringExtensionInjector injector = SpringExtensionInjector.get(frameworkModel.getInternalApplicationModel());
//        this.filter = injector.getInstance(Filter.class, "springSecurityFilterChain");
    }

    @Override
    public String[] getPatterns() {
        return new String[]{"/order/**"};
    }

    @Override
    public Filter get() {
        System.out.println("SecurityFilterV2 executed");
        return this.filter;
    }

    @Override
    public int getPriority() {
        return -1000;
    }
}
