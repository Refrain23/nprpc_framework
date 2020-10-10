package com.liming;

import com.liming.provider.RpcProvider;

/**
 *
 */
public class ProviderApp {
    public static void main(String[] args) {
        /**
         * 启动一个可以提供rpc远程方法调用的Server
         * 1、需要一个RpcProvider(rpc中的)对象
         * 2、向RpcProvider上注册rpc方法 ： serServiceImpl.login、serServiceImpl.reg
         * 3、启动RpcProvider这个Server站点  阻塞等待远程rpc方法的请求
         */
        /**
         *  step1  创建RpcProvider对象
         */

        RpcProvider.Builder builder = RpcProvider.newBuilder();
        RpcProvider rpcProvider = builder.build("config.properties");

        /**
         * step2 向RpcProvider对象注册方法
         *  UserServiceImpl : 服务对象名
         *  login、reg ：服务方法名
         */
        rpcProvider.registerRpcService(new UserServiceImpl());
        /**
         * step3 启动rpc server这个站点，阻塞等待远程rpc调用请求
         */
        rpcProvider.start();
    }
}





