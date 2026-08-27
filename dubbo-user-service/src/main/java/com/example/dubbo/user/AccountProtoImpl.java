package com.example.dubbo.user;

import com.example.dubbo.user.mapper.AccountMapper;
import com.example.proto.AccountProtoService;
import com.example.proto.DebitRequest;
import com.example.proto.DebitResponse;
import com.example.proto.DubboAccountProtoServiceTriple;
import io.seata.core.context.RootContext;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * 账户内部 RPC（IDL/Protobuf 模式，Triple 二进制协议）。
 * 供 order-service 在 Seata 全局事务中调用（XID 与身份由 ContextPropagation 过滤器透传，
 * 与序列化方式无关）。旧的 Java 接口版 AccountService 已删除。
 */
@DubboService
public class AccountProtoImpl extends DubboAccountProtoServiceTriple.AccountProtoServiceImplBase {

    private final AccountMapper accountMapper;

    public AccountProtoImpl(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @Override
    @PreAuthorize("hasRole('USER')")  // 下游服务也做鉴权，不只认证
    public DebitResponse debit(DebitRequest request) {
        long userId = request.getUserId();
        double money = request.getMoney();
        System.out.println("💰 [AccountProto] 扣款开始, userId=" + userId + ", money=" + money
                + ", XID=" + RootContext.getXID());

        int rows = accountMapper.debit(userId, money);
        if (rows == 0) {
            // 返回失败由调用方抛异常触发全局回滚（IDL 方法返回值风格，不用回调）
            return DebitResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("余额不足, userId=" + userId)
                    .build();
        }
        Double remaining = accountMapper.selectMoney(userId);
        System.out.println("✅ [AccountProto] 扣款完成, 剩余余额=" + remaining);
        return DebitResponse.newBuilder()
                .setSuccess(true)
                .setRemaining(remaining == null ? 0 : remaining)
                .build();
    }
}
