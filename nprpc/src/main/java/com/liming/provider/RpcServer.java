package com.liming.provider;

import com.liming.RpcMetaProto;
import com.liming.callback.INotifyProvider;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;


/**
 * 描述：Rpc网络服务器，使用Netty开发
 */
public class RpcServer {
    private INotifyProvider iNotifyProvider;

    public RpcServer(INotifyProvider iNotifyProvider) {
        this.iNotifyProvider = iNotifyProvider;
    }

    public void start(String ip, int port) {
        System.out.println("======RpcServer启动成功======");
        // 1、创建主事件线程，用于处理新用户连接
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // 2、创建工作线程，用于处理用户读写的网络操作
        EventLoopGroup workerGroup = new NioEventLoopGroup(3);
        // 3、创建服务器启动端助手来配置参数
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        System.out.println("=====线程组绑定成功=======");
        serverBootstrap.group(bossGroup, workerGroup)   // 4、设置两个线程组
                .channel(NioServerSocketChannel.class)  // 5、使用NioServerSocketChannel作为服务器通道的实现
                .option(ChannelOption.SO_BACKLOG, 1024)// 6、设置线程队列中等待连接的个数
                .option(ChannelOption.SO_KEEPALIVE,true)
                .childHandler(new ChannelInitializer<SocketChannel>() { // 7、创建一个通道初始化对象
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        System.out.println("========channel初始化成功========");
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        // 8、向pipeline中添加自定义的hander类
                        pipeline.addLast(new RpcServerHandler());
                        // 9、添加编码器
                        System.out.println("========handler绑定成功========");
                        pipeline.addLast("encoder", new ObjectEncoder());
                        // 10、添加解码器
//                        pipeline.addLast("decoder", new ObjectDecoder());

                    }
                });
        try {
            // 阻塞，开启网路服务
            ChannelFuture channelFuture = serverBootstrap.bind(ip, port).sync();
            System.out.println("======ip:" + ip + "port:" + port + "绑定成功！=========");
            // 关闭网络服务
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            System.out.println("=======线程组释放！==========");
        }
    }

    /**
     * 描述：自定义服务器业务处理类,继承来netty的ChannelInboundHandlerAdapter类,重写其中方法
     */
    private class RpcServerHandler extends ChannelInboundHandlerAdapter {
        /**
         * 处理接收到事件
         *
         * @param ctx
         * @param msg
         * @throws Exception
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            /**
             *  ByteBuf 是Netty的缓冲区，需要将得到的字节流强转为ByteBuf类型
             *  request 就是远程调用端发送过来的rpc调用请求所包含的所有信息参数
             *
             *  发起调用的数据格式：headerSize + UserServiceRpc(服务对象)login(对象方法) + zhangsan123456
             *  20 + UserServiceRpclogin + 参数
             */
            System.out.println("======channelRead成功=========");
            ByteBuf request = (ByteBuf) msg;


            // 1、先读取头部信息的长度
            int headerSize = request.readInt();


            // 2、读取头部信息(服务对象名称和服务方法名称)
            byte[] metaBuf = new byte[headerSize];
            request.readBytes(metaBuf);

            // 3、返序列化生成RpcMeta,获取服务对象名和他的方法名
            RpcMetaProto.RpcMeta rpcMeta = RpcMetaProto.RpcMeta.parseFrom(metaBuf);
            String serviceName = rpcMeta.getServiceName();
            String methodName = rpcMeta.getMethodName();

            // 4、读取rpc方法的参数,request.readableBytes()是request的可读字节数
            byte[] argBuf = new byte[request.readableBytes()];
            request.readBytes(argBuf);

            // 5、serviceName -> methodName(argBuf)
            byte[] response = iNotifyProvider.notify(serviceName, methodName, argBuf);


            // 6、把rpc调用的响应值response通过网络发送给调用方
            ByteBuf responseBuf = Unpooled.buffer(response.length);
            responseBuf.writeBytes(response);
            ChannelFuture channelFuture = ctx.writeAndFlush(responseBuf);

            // 7、模拟http响应后，直接关闭连接
            if (channelFuture.isSuccess()) {
                System.out.println("=====发送数据成功！========");
                ctx.close();
            }
        }

        /**
         * 连接异常处理,关闭连接
         *
         * @param ctx
         * @param cause
         * @throws Exception
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
        }
    }
}
