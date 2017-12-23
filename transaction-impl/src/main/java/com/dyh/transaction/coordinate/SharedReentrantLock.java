package com.dyh.transaction.coordinate;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.RevocationListener;
import org.apache.curator.framework.state.ConnectionStateListener;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by dengyunhui on 2017/8/27.
 */
public class SharedReentrantLock {
    private String lockPath;
    private long maxWait = -1;
    private TimeUnit waitUnit;
    private CuratorFramework client;
    private InterProcessMutex lock;
    private Map<Thread,Boolean> lockedThread = new WeakHashMap<>();

    private ConnectionStateListener stateListener = (client, newState) -> {
        if (Boolean.FALSE.equals(lockedThread.get(Thread.currentThread()))){
            //如果当前lock没有获取锁,则忽略
            return;
        }

        switch (newState){
            case LOST:
                lockedThread.clear();
                createLock();
                break;
            default:
                System.out.println(newState.toString());
        }
    };

    private RevocationListener<InterProcessMutex> revocationListener;



    public static void main(String[] args) throws Exception{
        SharedReentrantLock sharedReentrantLock = new SharedReentrantLock("/lock",-1,TimeUnit.MILLISECONDS,ZkClient.createSimple("127.0.0.1:2181"));
        //SharedReentrantLock sharedReentrantLock2 = new SharedReentrantLock("/lock",-1,TimeUnit.MILLISECONDS,ZkClient.createSimple("127.0.0.1:2181"));
        boolean hasLock = sharedReentrantLock.lock();
        System.out.println(hasLock);
        new CountDownLatch(1).await();
        //sharedReentrantLock2.lock();

    }

    public SharedReentrantLock(String lockPath, long maxWait, TimeUnit waitUnit, CuratorFramework client) {
        this.lockPath = lockPath;
        this.maxWait = maxWait;
        this.waitUnit = waitUnit;
        this.client = client;
        revocationListener = forLock -> {
            if (!forLock.isAcquiredInThisProcess()){
                return;
            }

            try {
                forLock.release();
            }catch (Exception e){
                e.printStackTrace();
            }
        };

        createLock();
    }

    private void createLock() {
        this.lock = new InterProcessMutex(client, lockPath);
        this.lock.makeRevocable(revocationListener);
        client.getConnectionStateListenable().addListener(stateListener);
    }

    public boolean lock() {
        boolean result;
        try {
            client.start();
            result = lock.acquire(maxWait,waitUnit);
            lockedThread.put(Thread.currentThread(),Boolean.TRUE);
        } catch (Exception e) {
            result = false;
            e.printStackTrace();
        }
        return result;
    }

    public void unlock(){
        try{
            client.close();
            lock.release();
        }catch (Exception e){
            //
        }
    }


    /**
     * Make the lock revocable.
     * Your listener will get called when another process/thread wants you to release the lock.
     * Revocation is cooperative.
     * @param listener
     */
    public void makeRevocable(RevocationListener<InterProcessMutex> listener){
        lock.makeRevocable(listener);
    }
}