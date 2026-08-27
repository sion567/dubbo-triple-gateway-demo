package com.example.dubbo.storage;

import com.example.dubbo.storage.mapper.StorageMapper;
import com.example.proto.DeductRequest;
import com.example.proto.DeductResponse;
import com.example.proto.DubboStorageProtoServiceTriple;
import io.seata.core.context.RootContext;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * 库存内部 RPC（IDL/Protobuf 模式）：供 order-service 在 Seata 全局事务中调用。
 * REST 端点（list/save/delete）仍在 StorageServiceImpl，两套并存互不影响。
 */
@DubboService
public class StorageProtoImpl extends DubboStorageProtoServiceTriple.StorageProtoServiceImplBase {

    private final StorageMapper storageMapper;

    public StorageProtoImpl(StorageMapper storageMapper) {
        this.storageMapper = storageMapper;
    }

    @Override
    @PreAuthorize("hasRole('USER')")
    public DeductResponse deduct(DeductRequest request) {
        String productCode = request.getProductCode();
        int count = request.getCount();
        System.out.println("📉 [StorageProto] 扣库存开始, productCode=" + productCode
                + ", count=" + count + ", XID=" + RootContext.getXID());

        int rows = storageMapper.deduct(productCode, count);
        if (rows == 0) {
            return DeductResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("库存不足, productCode=" + productCode)
                    .build();
        }
        Integer remaining = storageMapper.selectCount(productCode);
        System.out.println("✅ [StorageProto] 扣库存完成, 剩余=" + remaining);
        return DeductResponse.newBuilder()
                .setSuccess(true)
                .setRemaining(remaining == null ? 0 : remaining)
                .build();
    }
}
