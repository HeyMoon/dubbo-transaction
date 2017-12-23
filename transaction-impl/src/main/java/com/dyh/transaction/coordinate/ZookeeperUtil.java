package com.dyh.transaction.coordinate;

import com.dyh.transaction.utils.NetworkUtil;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Created by dengyunhui on 2017/12/23 16:33.
 */
public class ZookeeperUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperUtil.class);
    
    private String connectionString;
    
    private ZooKeeper zk;
    
    private int sessionTimeout;

    public ZookeeperUtil(String connectionString, int sessionTimeout) {
        this.connectionString = connectionString;
        this.sessionTimeout = sessionTimeout;
    }

    public static void main(String[] args) {
        /**
         * 用法
         */
        ZookeeperUtil zookeeperUtil = new ZookeeperUtil("127.0.0.1:2181",15000);
        zookeeperUtil.connect();
        zookeeperUtil.createCurrentNode(generateKey("serviceName","versionName"));
        System.out.println("......................" + masterMap);
        try {
            new CountDownLatch(1).await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public void connect() {
        try {
            zk = new ZooKeeper(connectionString, sessionTimeout, watchedEvent -> {
                if (watchedEvent.getState() == Watcher.Event.KeeperState.Expired) {
                    LOGGER.info("ZookeeperUtil session timeout to  {} [Zookeeper]", connectionString);
                    destroy();
                    connect();

                } else if (Watcher.Event.KeeperState.SyncConnected == watchedEvent.getState()) {
                    LOGGER.info("ZookeeperUtil connected to  {} [Zookeeper]", connectionString);
                    addMasterRoute();
                    
                } else if (Watcher.Event.KeeperState.Disconnected == watchedEvent.getState()) {
                    //zookeeper重启或zookeeper实例重新创建
                    LOGGER.error("Registry {} zookeeper 连接断开，可能是zookeeper重启或重建");

                    masterMap.clear(); //断开连接后，认为，master应该失效，避免某个孤岛一直以为自己是master

                    destroy();
                    connect();
                }
            });
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void destroy() {
        if (zk != null) {
            try {
                LOGGER.info("ZookeeperUtil closing connection to zookeeper {}", connectionString);
                zk.close();
                zk = null;
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

    }

    public void addOrUpdateServerInfo(String path, String data) {
        String[] paths = path.split("/");

        String createPath = "/";
        for (int i = 1; i < paths.length - 1; i++) {
            createPath += paths[i];
            addPersistServerNode(createPath, "");
            createPath += "/";
        }

        addServerInfo(path, data);
    }

    /**
     * 添加持久化的节点
     *
     * @param path
     * @param data
     */
    private void addPersistServerNode(String path, String data) {
        Stat stat = exists(path);

        if (stat == null)
            zk.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, persistNodeCreateCb, data);
    }

    private Stat exists(String path) {
        Stat stat = null;
        try {
            stat = zk.exists(path, false);
        } catch (KeeperException e) {
        } catch (InterruptedException e) {
        }
        return stat;
    }

    /**
     * 添加持久化节点回调方法
     */
    private AsyncCallback.StringCallback persistNodeCreateCb = (rc, path, ctx, name) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOGGER.info("创建节点:{},连接断开，重新创建", path);
                addPersistServerNode(path, (String) ctx);
                break;
            case OK:
                LOGGER.info("创建节点:{},成功", path);
                break;
            case NODEEXISTS:
                LOGGER.info("创建节点:{},已存在", path);
                updateServerInfo(path, (String) ctx);
                break;
            default:
                LOGGER.info("创建节点:{},失败", path);
        }
    };


    /**
     * 异步添加serverInfo,为临时节点，如果server挂了就木有了
     */
    public void addServerInfo(String path, String data) {
        zk.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, serverAddrCreateCb, data);
    }

    /**
     * 异步添加serverInfo的回调处理
     */
    private AsyncCallback.StringCallback serverAddrCreateCb = (rc, path, ctx, name) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOGGER.info("添加serviceInfo:{},连接断开，重新添加", path);
                addOrUpdateServerInfo(path, (String) ctx);
                break;
            case OK:
                LOGGER.info("添加serviceInfo:{},成功", path);
                break;
            case NODEEXISTS:
                LOGGER.info("添加serviceInfo:{},已存在，删掉后重新添加", path);
                try {
                    zk.delete(path, -1);
                } catch (Exception e) {
                    LOGGER.error("删除serviceInfo:{} 失败:{}", path, e.getMessage());
                }
                addOrUpdateServerInfo(path, (String) ctx);
                break;
            default:
                LOGGER.info("添加serviceInfo:{}，出错", path);
        }
    };

    /**
     * 异步更新节点信息
     */
    public void updateServerInfo(String path, String data) {
        zk.setData(path, data.getBytes(), -1, serverAddrUpdateCb, data);
    }

    /**
     * 异步更新节点信息的回调方法
     */
    private AsyncCallback.StatCallback serverAddrUpdateCb = (rc, path1, ctx, stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                updateServerInfo(path1, (String) ctx);
                return;
        }
    };

    public void setconnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    //----------------------------竞选master-------------------------------------------------------------------------
    public static Map<String, Boolean> masterMap = new HashMap<>();

    private static final String BASE_PATH = "/master/services/";

    /**
     * 竞选Master
     * <p>
     * /soa/master/services/**.**.**.AccountService:1.0.0-0000000001   data [192.168.99.100]
     */
    public void createCurrentNode(String key) {
        zk.create(BASE_PATH + key + "-", serverIP.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL, masterCreateCallback, key);
    }

    private AsyncCallback.StringCallback masterCreateCallback = (rc, path, ctx, name) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                try {
                    Stat stat = zk.exists(name, false);
                    if (stat == null) {
                        createCurrentNode((String) ctx);
                    } else {
                        checkIsMaster((String) ctx, name.replace(path, ""));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case OK:
                LOGGER.info("节点({})添加成功，判断是否master", name);
                String currentId = name.replace(path, "");
                checkIsMaster((String) ctx, currentId);
                break;
            case NODEEXISTS:
                LOGGER.error("创建节点({})时发现已存在，这是不可能发生的!!!", name);
                break;
            case NONODE:
                LOGGER.error("({})的父节点不存在，创建失败", name);
                break;
            default:
                LOGGER.error("创建({})异常：{}", path, KeeperException.Code.get(rc));
        }
    };


    /**
     * 根据serviceKey, 当前容器中serviceKey对应在zookeeper中的id, 判断当前节点是否master
     *
     * @param serviceKey
     * @param currentId
     */
    private boolean checkIsMaster(String serviceKey, String currentId) {
        boolean result = false;

        try {
            List<String> children = zk.getChildren("/master/services", false).stream().filter(s -> s.startsWith(serviceKey + "-")).collect(Collectors.toList());

            if (children.size() <= 0) {
                createCurrentNode(serviceKey);
                return result;
            }

            Collections.sort(children);

            String least = children.get(0).replace((serviceKey + "-"), "");
            if (least.equals(currentId)) {
                result = true;
                masterMap.put(serviceKey, true);
                LOGGER.info("({})竞选master成功, master({})", serviceKey, serverIP);
            } else {
                masterMap.put(serviceKey, false);
                LOGGER.info("({})竞选master失败，当前节点为({}), 监听最小节点({})", serviceKey, serviceKey + "-" + currentId, children.get(0));

                try {
                    String masterData = new String(zk.getData(BASE_PATH + children.get(0), false, null));
                    LOGGER.info("{}的master节点为{}", serviceKey, masterData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ifLeastNodeExist(serviceKey, currentId, children.get(0).replace(serviceKey + "-", ""));
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 监控最小节点是否存在，存在则保持监听，不存在则继续判断自己是否master
     */
    private void ifLeastNodeExist(String serviceKey, String currentId, String leastId) {

        zk.exists(BASE_PATH + serviceKey + "-" + leastId, event -> {
            if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
                LOGGER.info("最小节点({})被删除，当前节点({})竞选master", event.getPath(), serviceKey + "-" + currentId);
                checkIsMaster(serviceKey, currentId);
            }

        }, (rc, path, ctx, stat) -> {

            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    ifLeastNodeExist(serviceKey, currentId, leastId);
                    break;
                case NONODE:
                    LOGGER.info("最小节点({})不存在，当前节点({})竞选master", serviceKey + "-" + leastId, serviceKey + "-" + currentId);
                    checkIsMaster(serviceKey, currentId);
                    break;
                case OK:
                    if (stat == null) {
                        LOGGER.info("最小节点({})不存在，当前节点({})竞选master", serviceKey + "-" + leastId, serviceKey + "-" + currentId);
                        checkIsMaster(serviceKey, currentId);
                    } else
                        LOGGER.info("最小节点({})存在，当前节点({})保持对最小节点监听", serviceKey + "-" + leastId, serviceKey + "-" + currentId);

                    break;
                default:
                    checkIsMaster(serviceKey, currentId);
            }

        }, serviceKey + "-" + currentId);
    }


    public static String generateKey(String serviceName, String versionName) {
        return serviceName + ":" + versionName;
    }

    private static final String serverIP = NetworkUtil.getServerIp();

    /**
     * 创建/soa/master/services节点
     */
    private void addMasterRoute() {
        String[] paths = BASE_PATH.split("/");
        String route = "/";
        for (int i = 1; i < paths.length; i++) {
            route += paths[i];
            addPersistServerNode(route, "");
            route += "/";
        }
    }
    
    
}