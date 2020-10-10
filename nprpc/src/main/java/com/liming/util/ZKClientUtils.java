package com.liming.util;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;


import java.util.HashMap;
import java.util.Map;

/**
 * 描述：和zookeeper通信的辅助工具类。
 */

public class ZKClientUtils {
    private static String rootPath = "/zookeeper";
    private ZkClient zkClient;
    private Map<String,String> ephemeralZnodeInfo;

    /**
     * 通过ip:port 字符串信息连接zkServer
     *
     * @param serverList ip：port
     */
    public ZKClientUtils(String serverList) {
        this.zkClient = new ZkClient(serverList,2000);
        this.ephemeralZnodeInfo = new HashMap<>();
        if(!this.zkClient.exists(rootPath)){
            this.zkClient.createPersistent(rootPath,null);
        }
    }

    /**
     * 关闭和zkServer的连接
     */
    public void close() {
        this.zkClient.close();
    }

    public static String getRootPath() {
        return rootPath;
    }
    public static void setRootPath(){
        ZKClientUtils.rootPath = rootPath;
    }

    /**
     * 创建临时性节点
     * @param path
     * @param data
     */
    public void createEphemeral(String path, String data) {
        path = rootPath + path;
        // 创建临时节点的时候,用map保存临时节点信息
        ephemeralZnodeInfo.put(path,data);
        // 如果不存在，就创建
        if(!this.zkClient.exists(path)){
            this.zkClient.createEphemeral(path, data);
        }

    }
    /**
     * 创建永久性节点
     * @param path
     * @param data
     */
    public void createPersistent(String path,String data){
        path = rootPath + path;
        // 如果不存在，就创建
        if(!this.zkClient.exists(path)){
            this.zkClient.createPersistent(path, data);
        }
    }

    public void addWatcher(String path){
        // 添加数据改变监听
        this.zkClient.subscribeDataChanges(rootPath + path, new IZkDataListener() {
            @Override
            public void handleDataChange(String s, Object o) throws Exception {

            }

            /**
             * 一定要设置znode节点监听,因为zkClient断掉,由于zkServer无法及时获取zkClient的关闭状态
             * 所以zkServer会等待session timeout时间以后,会把zkClient创建的临时节点全部删除掉，但是
             * 如果在session timeout时间内,又启动了zkClient,原来的节点还存在,所以无法创建。
             * 那么等session timeout时间后才能创建新的节点,所以添加监听,删除后马上创建新的。
             * @param path
             * @throws Exception
             */
            @Override
            public void handleDataDeleted(String path) throws Exception {
                System.out.println("watcher -> handleDataDeleted：" + path + "已经被删除！");
                String data = ephemeralZnodeInfo.get(path);
                if(data != null){
                    zkClient.createEphemeral(path,data);
                    System.out.println("watcher -> handleDataDeleted" + path + "已经被重新创建！");
                }
            }
        });

    }



    public String getData(String path) {
        path = rootPath + path;
        return this.zkClient.readData(path);
    }

    /**
     * 测试连接
     *
     * @param args
     */
//    public static void main(String[] args) {
//        ZKClientUtils zkClientUtils = new ZKClientUtils("192.168.154.132:2181");
////        zkClientUtils.createPersistent("/test2","lijiaxin");
//        System.out.println(zkClientUtils.getData("/test2"));
//
////        System.out.println(zkClientUtils.getData("/test1"));
//        //ystem.out.println(zkClientUtils.getRootPath());
//
//    }
}
