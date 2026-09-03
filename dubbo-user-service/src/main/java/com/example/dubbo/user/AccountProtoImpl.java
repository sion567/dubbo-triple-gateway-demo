package com.example.dubbo.user;

import com.example.dubbo.user.mapper.UserMapper;
import com.example.proto.AccountProtoService;
import com.example.proto.DebitRequest;
import com.example.proto.DebitResponse;
import io.seata.core.context.RootContext;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@DubboService
public class AccountProtoImpl implements AccountProtoService {

    private final UserMapper userMapper;

    public AccountProtoImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @PreAuthorize("hasRole('USER')")
    public DebitResponse debit(DebitRequest request) {
        long userId = request.getUserId();
        BigDecimal money = BigDecimal.valueOf(request.getMoney());
        System.out.println("💰 [AccountProto] 扣款开始, userId=" + userId + ", money=" + money
                + ", XID=" + RootContext.getXID());

        int rows = userMapper.debit(userId, money);
        if (rows == 0) {
            return DebitResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("余额不足, userId=" + userId)
                    .build();
        }
        BigDecimal remaining = userMapper.selectMoney(userId);
        System.out.println("✅ [AccountProto] 扣款完成, 剩余余额=" + remaining);
        return DebitResponse.newBuilder()
                .setSuccess(true)
                .setRemaining(remaining != null ? remaining.doubleValue() : 0)
                .build();
    }

    @Override
    public CompletableFuture<DebitResponse> debitAsync(DebitRequest request) {
        return CompletableFuture.completedFuture(debit(request));
    }
}
