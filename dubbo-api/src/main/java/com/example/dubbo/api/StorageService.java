package com.example.dubbo.api;

/**
 * 库存服务（RM）：扣减商品库存，参与 Seata AT 全局事务
 */
public interface StorageService {

    /**
     * 扣减库存。库存不足时抛异常，触发全局回滚
     *
     * @param productCode 商品编码
     * @param count       扣减数量
     */
    void deduct(String productCode, int count);
}
