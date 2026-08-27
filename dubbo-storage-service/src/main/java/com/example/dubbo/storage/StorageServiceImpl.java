package com.example.dubbo.storage;

import com.example.dubbo.api.StorageService;
import com.example.dubbo.storage.mapper.StorageMapper;
import io.seata.core.context.RootContext;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

/**
 * 库存服务（RM）：作为分支事务参与全局事务（XID、身份由 ContextPropagation 过滤器还原）。
 */
@DubboService
public class StorageServiceImpl implements StorageService {

    private final StorageMapper storageMapper;

    public StorageServiceImpl(StorageMapper storageMapper) {
        this.storageMapper = storageMapper;
    }

    @Override
    @PreAuthorize("hasRole('USER')")  // 下游服务也做鉴权，不只认证
    public void deduct(String productCode, int count) {
        System.out.println("📉 [StorageService] 扣库存开始, productCode=" + productCode
                + ", count=" + count + ", XID=" + RootContext.getXID());

        int rows = storageMapper.deduct(productCode, count);
        if (rows == 0) {
            throw new RuntimeException("库存不足, productCode=" + productCode);
        }
        System.out.println("✅ [StorageService] 扣库存完成, 剩余=" + storageMapper.selectCount(productCode));
    }

    @Override
    public List<Map<String, Object>> list() {
        return storageMapper.selectAll();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> save(Map<String, Object> storage) {
        String productCode = String.valueOf(storage.get("productCode"));
        int count = Integer.parseInt(String.valueOf(storage.get("count")));
        storageMapper.upsert(productCode, count);
        return Map.of("code", 0, "message", "ok");
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> delete(String productCode) {
        int rows = storageMapper.delete(productCode);
        return rows > 0 ? Map.of("code", 0, "message", "ok") : Map.of("code", 404, "message", "商品不存在");
    }
}
