package com.liming.consumer;

import com.google.protobuf.*;
import com.liming.RpcMetaProto;
import com.liming.controller.NpRpcController;
import com.liming.util.ZKClientUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;

public class RpcConsumer implements RpcChannel {
    private static final String ZKServer = "zookeeper";
    private String zkServer;

    public RpcConsumer(String configFile) {
        Properties properties = new Properties();
        try {
            properties.load(RpcConsumer.class.getClassLoader().getResourceAsStream(configFile));
            this.zkServer = properties.getProperty(ZKServer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * stub代理对象，需要接收一个实现了RpcChannel的对象(RpcConsumer)，当用stub调用任意rpc方法的时候
     * 全部都调用了当前这个RpcChannel的callMethod方法
     *
     * @param methodDescriptor
     * @param rpcController
     * @param message
     * @param message1
     * @param rpcCallback
     */
    @Override
    public void callMethod(Descriptors.MethodDescriptor methodDescriptor,
                           RpcController rpcController,  // 控制台显示，比如一些错误信息
                           Message message,  // request
                           Message message1, // response
                           RpcCallback<Message> rpcCallback) {
        /**
         *  打包参数，递交给网络
         *  rpc调用参数格式：headSize + serviceName + methodName + args
         */
        System.out.println("==========调用了rpc方法============");
        Descriptors.ServiceDescriptor service = methodDescriptor.getService();
        String serviceName = service.getName();
        String methodName = methodDescriptor.getName();
        /**
         * 根据serviceName 和 methodName 在zookeeper上找到对应的主机ip和port
         */
        ZKClientUtils zkClientUtils = new ZKClientUtils(zkServer);
        String ip = "";
        int port = 0;
        String path = "/" + serviceName + "/" + methodName;
        String hostStr = zkClientUtils.getData(path);  // 得到主机的地址
        zkClientUtils.close();                         // 得到地址后可以断开zookeeper连接
        if(hostStr == null){
            rpcController.setFailed("connect failed,please check server!");
            rpcCallback.run(message1);
            return;   //  没有找到，直接返回
        } else{
            String[] host = hostStr.split(":");
            ip = host[0];
            port = Integer.parseInt(host[1]);
        }


        // 序列化头部信息 对象名+方法名
        RpcMetaProto.RpcMeta.Builder builder = RpcMetaProto.RpcMeta.newBuilder();
        builder.setServiceName(serviceName);
        builder.setMethodName(methodName);
        byte[] metaBuf = builder.build().toByteArray();

        // 序列化参数
        byte[] argBuf = message.toByteArray();

        /**
         * 整合rpc方法参数信息
         */
        ByteBuf buf = Unpooled.buffer(4 + metaBuf.length + argBuf.length);
        buf.writeInt(metaBuf.length);   // headSize
        buf.writeBytes(metaBuf);        // serviceName+methodName
        buf.writeBytes(argBuf);         // args

        // 待发送的已经整合好的数据
        byte[] sendBuf = buf.array();


        // 通过网络发送rpc调用的请求信息
        Socket client = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;

        try {
            System.out.println("========client创建成功=======");
            client = new Socket();
            client.connect(new InetSocketAddress(ip, port));
            System.out.println("rpcServer成功连接到provider");
            outputStream = client.getOutputStream();
            inputStream = client.getInputStream();

            // 发送数据
            outputStream.write(sendBuf);
            outputStream.flush();

            // wait等待rpc调用响应

            ByteArrayOutputStream recvBuf = new ByteArrayOutputStream();
            byte[] resBuf = new byte[1024];
            int size = inputStream.read(resBuf);

            /**
             * size有可能是0,因为RpcProvider封装Response响应参数的时候，
             * 如果响应参数的成员变量的值都是默认值，实际上RpcProvider递给RpcServer的就是空值
             */
            if (size > 0) {
                System.out.println("=======远程调用成功了，返回结果==========");
                recvBuf.write(resBuf, 0, size);
                rpcCallback.run(message1.getParserForType().parseFrom(recvBuf.toByteArray()));
            } else {
                rpcCallback.run(message1);
            }
        } catch (IOException e) {
            rpcController.setFailed("connect failed,please check server!");
            rpcCallback.run(message1);
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (client != null) {
                    client.close();
                }
            } catch (IOException e) {
                e.printStackTrace();

            }
        }
    }
}
