package com.example.dubbo.storage;

import com.example.dubbo.api.StorageService;
import com.example.dubbo.storage.mapper.StorageMapper;
import io.seata.core.context.RootContext;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 库存服务（RM）：作为分支事务参与全局事务（XID 由 Provider 过滤器自动绑定）。
 */
@DubboService
public class StorageServiceImpl implements StorageService {

    private final StorageMapper storageMapper;

    public StorageServiceImpl(StorageMapper storageMapper) {
        this.storageMapper = storageMapper;
    }

    @Override
    public void deduct(String productCode, int count) {
        System.out.println("📉 [StorageService] 扣库存开始, productCode=" + productCode
                + ", count=" + count + ", XID=" + RootContext.getXID());

        int rows = storageMapper.deduct(productCode, count);
        if (rows == 0) {
            throw new RuntimeException("库存不足, productCode=" + productCode);
        }
        System.out.println("✅ [StorageService] 扣库存完成, 剩余=" + storageMapper.selectCount(productCode));
    }
}
