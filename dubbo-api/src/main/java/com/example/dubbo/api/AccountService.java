package com.example.dubbo.api;

/**
 * 账户服务（RM）：扣减用户余额，参与 Seata AT 全局事务
 */
public interface AccountService {

    /**
     * 扣减账户余额
     *
     * @param userId 用户 id
     * @param money  扣减金额（元）
     */
    void debit(Long userId, double money);
}
