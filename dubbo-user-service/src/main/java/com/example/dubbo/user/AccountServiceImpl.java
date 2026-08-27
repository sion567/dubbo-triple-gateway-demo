package com.example.dubbo.user;

import com.example.dubbo.api.AccountService;
import com.example.dubbo.user.mapper.AccountMapper;
import io.seata.core.context.RootContext;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * 账户服务（RM）：作为分支事务参与全局事务。
 * XID 和身份已由 ContextPropagationProviderFilter 还原（token 从 order-service 链式透传），
 * 本类无需任何事务注解，Seata 代理数据源会自动把本次更新注册为分支事务。
 */
@DubboService
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;

    public AccountServiceImpl(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @Override
    @PreAuthorize("hasRole('USER')")  // 下游服务也做鉴权，不只认证
    public void debit(Long userId, double money) {
        System.out.println("💰 [AccountService] 扣款开始, userId=" + userId + ", money=" + money
                + ", XID=" + RootContext.getXID());

        // 余额不足时 update 影响行数为 0，抛异常触发全局回滚
        int rows = accountMapper.debit(userId, money);
        if (rows == 0) {
            throw new RuntimeException("余额不足, userId=" + userId);
        }
        System.out.println("✅ [AccountService] 扣款完成, 剩余余额=" + accountMapper.selectMoney(userId));
    }
}
