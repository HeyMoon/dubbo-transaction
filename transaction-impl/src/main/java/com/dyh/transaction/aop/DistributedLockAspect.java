package com.dyh.transaction.aop;

import com.dyh.transaction.coordinate.SharedReentrantLock;
import com.dyh.transaction.coordinate.ZkClient;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Created by dengyunhui on 2017/12/23 22:35.
 */
@Aspect
@Component
public class DistributedLockAspect {

    @Autowired
    private Environment env;


    @Pointcut("@annotation(com.dyh.transaction.aop.DistributedLock)")
    public void pointCut(){
    }

    @Around("pointCut()")
    public void doBefore(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        String zookeeperAddress = env.getProperty("zookeeper.address");
        String className = proceedingJoinPoint.getTarget().getClass().getName();

        SharedReentrantLock sharedReentrantLock = null;
        try {
            sharedReentrantLock = new SharedReentrantLock(
                    "/" + className,
                    -1,
                    TimeUnit.MILLISECONDS,
                    ZkClient.createSimple(zookeeperAddress));

            if (sharedReentrantLock.lock()){
                proceedingJoinPoint.proceed();
            }
        }finally {
            if (sharedReentrantLock != null){
                sharedReentrantLock.unlock();
            }
        }

    }

}