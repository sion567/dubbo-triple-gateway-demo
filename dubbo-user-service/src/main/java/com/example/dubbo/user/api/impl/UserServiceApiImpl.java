package com.example.dubbo.user.api.impl;

import com.example.dubbo.user.api.UserServiceApi;
import com.example.dubbo.user.mapper.AccountMapper;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class UserServiceApiImpl implements UserServiceApi {

    private final AccountMapper accountMapper;

    public UserServiceApiImpl(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @Override
    public Long getUserIdByUsername(String username) {
        var account = accountMapper.findByUsername(username);
        if (account == null) return null;
        return (Long) account.get("user_id");
    }
}