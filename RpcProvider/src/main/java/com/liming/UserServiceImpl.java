package com.liming;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

/**
 * 描述：原来是服务器的本地方法,现在要发布成RPC方法
 * 想要变成远程方法，要继承protobuf生成的类UserServiceProto.UserServiceRpc,之后重写protobuf中定义的方法
 * 这个类UserServiceImpl实现了UserServiceProto.UserServiceRpc这个接口
 */

public class UserServiceImpl extends UserServiceProto.UserServiceRpc {
    /**
     * 登录业务
     *
     * @param name
     * @param pwd
     * @return
     */
    public boolean login(String name, String pwd) {
//        System.out.println("call UserServiceImpl -> login");
////        System.out.println("name:" + name);
////        System.out.println("pwd:" + pwd);
////        return true;
        if(name.equals("zhangsan") && pwd.equals("123456"))
            return true;
        else
            return false;
    }

    public boolean reg(String name, String pwd, int age, String sex, String phone) {
        System.out.println("call UserServiceImpl -> reg");
        System.out.println("name:" + name);
        System.out.println("pwd:" + pwd);
        System.out.println("age:" + age);
        System.out.println("phone:" + phone);
        return true;
    }

    /**
     * login的RPC代理方法
     *
     * @param controller 可以接收方法的执行状态 先忽略
     * @param request    从requset里面提取到远程RPC调用方法需要的参数
     * @param done
     */

    @Override
    public void login(RpcController controller, UserServiceProto.LoginRequest request, RpcCallback<UserServiceProto.Response> done) {
        // 1、从requset里面提取到远程RPC调用方法需要的参数
        System.out.println("======调用了login方法=====");
        String name = request.getName();
        String pwd = request.getPwd();
        // 2、根据解析的参数做本地业务
        boolean result = login(name, pwd);
        System.out.println("result="+result);
        // 3、填写方法的响应值
        UserServiceProto.Response.Builder builder = UserServiceProto.Response.newBuilder();
        builder.setErrno(0);
        builder.setErrinfo("返回值错误");
        builder.setResult(result);

        // 4、把response对象交给RPC框架，由框架负责处理发送RPC调用响应值,在框架中重写run方法。
        done.run(builder.build());
    }

    /**
     * reg的RPC代理方法
     *
     * @param controller 可以接收方法的执行状态 先忽略
     * @param request
     * @param done
     */

    @Override
    public void reg(RpcController controller, UserServiceProto.RegRequest request, RpcCallback<UserServiceProto.Response> done) {

    }
}
