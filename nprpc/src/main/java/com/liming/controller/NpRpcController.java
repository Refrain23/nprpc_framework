package com.liming.controller;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

public class NpRpcController implements RpcController {
    private String errorText;
    private boolean isFailed;
    @Override
    public void reset() {
        this.isFailed = false;
        this.errorText = "";
    }

    @Override
    public boolean failed() {
        return isFailed;
    }

    @Override
    public String errorText() {
        return errorText;
    }

    @Override
    public void startCancel() {

    }

    @Override
    public void setFailed(String s) {
        this.isFailed = true;
        this.errorText = s;
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void notifyOnCancel(RpcCallback<Object> rpcCallback) {

    }
}
