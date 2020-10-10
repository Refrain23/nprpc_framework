package com.liming.callback;

public interface INotifyProvider {
    /**
     * 回调操作，RpcServer给RpcProvider上报接收到的rpc调用的相关方法和相关参数信息
     * @param serviceName  服务对象
     * @param methodName   对象方法
     * @param args         方法参数
     * @return             把rpc调用完成后的数据响应返回
     */
    byte[] notify(String serviceName,String methodName,byte[] args);

}
