package com.liming.provider;

import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Service;
import com.liming.callback.INotifyProvider;
import com.liming.util.ZKClientUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 描述：rpc方法提供的站点，只需要一个站点就可以发布当前主机的的所有rpc方法，用单列模式设计RpcProvider
 */
public class RpcProvider implements INotifyProvider {
    private static final String SERVER_IP = "ip";
    private static final String SERVER_PORT = "port";
    private static final String ZKServer = "zookeeper";
    private String serverIp;
    private int serverPort;
    private String zkServer;
    private ThreadLocal<byte[]> responseBufLocal;  // 使用ThreadLocal，作为响应返回值


    public RpcProvider() {
        this.serviceMap = new HashMap<>();
        this.responseBufLocal = new ThreadLocal<>();

    }

    /**
     * 启动rpc站点服务
     */
    public void start() {
        /*System.out.println("rpc服务站点启动！");
        serviceMap.forEach((k, v) -> {
            System.out.println(k + "服务对象注册成功！");
            v.methodMap.forEach((a, b) -> {
                System.out.println(k + "的服务对象的" + a + "方法已经注册成功！");
            });
        });*/

        /**
         * 向zookeeper上注册 service 和 method
         */
        ZKClientUtils zkClientUtils = new ZKClientUtils(zkServer);
        serviceMap.forEach((k, v) -> {
            String path = "/" + k;
            // 创建永久节点 serviceName
            zkClientUtils.createPersistent(path, null);
            v.methodMap.forEach((a, b) -> {
                String createPath = path + "/" + a;
                zkClientUtils.createEphemeral(createPath, serverIp + ":" + serverPort);
                // 给临时节点创建监听
                zkClientUtils.addWatcher(createPath);
                System.out.println(k+"服务对象的" + a + "方法" + "已经在" + createPath + serverIp + ":" + serverPort+ "下已经注册成功！");
            });

        });
        System.out.println("rpc server start at " + serverIp + ":" + serverPort);
        /**
         * 启动rpc网络服务
         */
        RpcServer rpcServer = new RpcServer(this);
        rpcServer.start(serverIp, serverPort);
    }

    /**
     * notify方法是在多线程方法中用到的，所以返回的request可以用LocalThread
     * 接收RpcServer网络模块上的报的rpc调用相关信息参数，执行具体的rpc方法调用
     *
     * @param serviceName 服务对象
     * @param methodName  对象方法
     * @param args        方法参数
     * @return
     */
    @Override
    public byte[] notify(String serviceName, String methodName, byte[] args) {
        System.out.println("========notify调用成功=========");
        ServiceInfo serviceInfo = serviceMap.get(serviceName);
        // 获取服务对象
        Service service = serviceInfo.service;
        // 获取对象方法
        Descriptors.MethodDescriptor method = serviceInfo.methodMap.get(methodName);

        // 从args反序列化出method方法的参数
        Message request = service.getRequestPrototype(method).toBuilder().build();
        try {
            request  = request.getParserForType().parseFrom(args);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        /**
         * rpc对象：service
         * rpc对象的方法：method
         * rpc方法的参数：request
         * 调用方法的返回值：response
         * 多线程调用，为了保证线程安全，所以使用ThreadLocal类型作为返回值
         * 根据method.getName() -> login
         *
         */
        service.callMethod(method, null, request, response -> {
            responseBufLocal.set(response.toByteArray());
        });
        return responseBufLocal.get();

    }


    /**
     * 服务方法的类型信息,map(方法名,方法描述)
     */
    private class ServiceInfo {
        public ServiceInfo() {
            this.service = null;
            this.methodMap = new HashMap<>();
        }

        Service service;
        Map<String, Descriptors.MethodDescriptor> methodMap;
    }

    /**
     * rpc站点：包含rpc所有服务对象和服务方法
     */
    private Map<String, ServiceInfo> serviceMap;

    /**
     * 注册rpc方法，只要是变成rpc方法的类，都实现了com.google.protobuf.Service这个接口
     *
     * @param service com.google.protobuf中的Service接口
     */
    public void registerRpcService(Service service) {
        Descriptors.ServiceDescriptor descriptorForType = service.getDescriptorForType();
        // 获取服务对象的名字
        String serviceName = descriptorForType.getName();
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.service = service;
        // 获取服务对象的所有方法列表
        List<Descriptors.MethodDescriptor> methodList = descriptorForType.getMethods();
        // 遍历方法列表
        for (Descriptors.MethodDescriptor method : methodList) {
            // 获取方法名字
            String methodName = method.getName();
            // 将方法名字和方法描述加到方法类型的信息的map中
            serviceInfo.methodMap.put(methodName, method);
        }
        // 将服务对象名和服务对象的所有方法添加到服务站点的map中
        serviceMap.put(serviceName, serviceInfo);

    }


    /**
     * 封装RpcProvider对象的创建细节
     */
    public static class Builder {
        /**
         * 饿汉模式 创建单列对象
         */
        private static RpcProvider INSTANCE = new RpcProvider();

        /**
         * 从配置文件中读取rpc serverIp、serverPort,给INSTANCE对象初始化
         * properties 存的是 key-value 键值对
         *
         * @param configFile 配置文件
         */
        public RpcProvider build(String configFile) {
            Properties properties = new Properties();
            try {
                properties.load(Builder.class.getClassLoader().getResourceAsStream(configFile));
                INSTANCE.setServerIp(properties.getProperty(SERVER_IP));
                INSTANCE.setServerPort(Integer.parseInt(properties.getProperty(SERVER_PORT)));
                INSTANCE.setZkServer(properties.getProperty(ZKServer));
                return INSTANCE;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * 返回一个对象建造器
     *
     * @return
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getZkServer() {
        return zkServer;
    }

    public void setZkServer(String zkServer) {
        this.zkServer = zkServer;
    }
}
