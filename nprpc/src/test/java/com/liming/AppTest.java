package com.liming;

import static org.junit.Assert.assertTrue;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

/**
 * protobuf的序列化和返序列化测试
 */
public class AppTest {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void test1() {
        // 通过工厂获得类
        TestProto.LoginRequest.Builder builder = TestProto.LoginRequest.newBuilder();
        builder.setName("liming");
        builder.setPwd("123456");
        // 通过builder生成实体类对象,longinRequest 为实体类对象，必须由builder初始化值后才能创建对象
        TestProto.LoginRequest loginRequest = builder.build();

        System.out.println(loginRequest.getName() + ":" + loginRequest.getPwd());
        // 将对象序列化，转化为字节流
        byte[] sendbuf = loginRequest.toByteArray();


        // 反序列化，将字节流转化为对象
        try {
            TestProto.LoginRequest request = TestProto.LoginRequest.parseFrom(sendbuf);
            System.out.println(request.getName() + ":" + request.getPwd());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    /**
     * properties测试
     */
    @Test
    public void test12() throws IOException {
        Properties properties = new Properties();
        properties.load(AppTest.class.getResourceAsStream("config.properties"));
        System.out.println(properties.getProperty("ip"));
        System.out.println(properties.getProperty("port"));
        System.out.println(properties.getProperty("zookeeper"));
    }
}
