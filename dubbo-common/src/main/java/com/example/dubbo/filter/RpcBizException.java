package com.example.dubbo.filter;

public class RpcBizException extends RuntimeException {
    private final int bizCode;
    private final String bizMessage;

    public RpcBizException(int bizCode, String bizMessage) {
        super(bizMessage);
        this.bizCode = bizCode;
        this.bizMessage = bizMessage;
    }

    public int getBizCode() {
        return bizCode;
    }

    public String getBizMessage() {
        return bizMessage;
    }
}