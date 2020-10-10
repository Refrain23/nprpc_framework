package com.liming;


import com.liming.consumer.RpcConsumer;
import com.liming.controller.NpRpcController;

/**
 * Hello world!
 */
public class ConsumerApp {

    public static void main(String[] args) {
        /**
         * stub代理对象，需要接收一个实现了RpcChannel的对象(RpcConsumer)，当用stub调用任意rpc方法的时候
         * 全部都调用了当前这个RpcChannel的callMethod方法
         */
        UserServiceProto.UserServiceRpc.Stub stub = UserServiceProto.UserServiceRpc.newStub(new RpcConsumer("config.properties"));
        /**
         * 先封装loginRequest，将参数序列化
         */
        UserServiceProto.LoginRequest.Builder builder = UserServiceProto.LoginRequest.newBuilder();
        builder.setName("zhangsan");
        builder.setPwd("123456");
        NpRpcController controller = new NpRpcController();
        stub.login(controller, builder.build(), response -> {
            /**
             * response中就是rpc方法调用以后的返回值
             *
             */
            if (controller.failed()) { // 如果方法未调用成功
                System.out.println(controller.errorText());
            } else {
                System.out.println("receive rpc call response");
                if (response.getErrno() == 0) {  // 调用正常，输出返回结果
                    System.out.println(response.getResult());
                } else {                       // 调用失败，打印错误信息
                    System.out.println(response.getErrinfo());
                }
            }
        });
    }
}
