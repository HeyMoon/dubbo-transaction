package com.dyh.transaction.schedule;

import com.alibaba.fastjson.JSONObject;
import com.dyh.transaction.aop.DistributedLock;
import com.dyh.transaction.constant.Constants;
import com.dyh.transaction.dao.mapper.GlobalTransactionMapper;
import com.dyh.transaction.dao.mapper.GlobalTransactionProcessMapper;
import com.dyh.transaction.dao.model.GlobalTransaction;
import com.dyh.transaction.dao.model.GlobalTransactionProcess;
import com.dyh.transaction.enums.GlobalTransactionProcessExpectedStatusEnum;
import com.dyh.transaction.enums.GlobalTransactionProcessStatusEnum;
import com.dyh.transaction.enums.GlobalTransactionsStatusEnum;
import com.dyh.transaction.utils.ApplicationContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

/**
 * @author dyh
 * @created at 2017 12 19 16:44
 */
@Component
public class GlobalTransactionManager {
    private static Logger LOGGER = LoggerFactory.getLogger(GlobalTransactionManager.class);

    @Autowired
    private GlobalTransactionMapper globalTransactionMapper;

    @Autowired
    private GlobalTransactionProcessMapper globalTransactionProcessMapper;

    @Scheduled(cron = "0 0/1 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock
    public void execute(){

        LOGGER.info("--- 事务管理器开始 ---");

        /**
         * 处理全局事务状态为失败或者部分回滚的记录，这些全局事务下的成功的子事务过程应该回滚
         */
        List<GlobalTransaction> failedRecords = globalTransactionMapper.findFailedRecords();

        LOGGER.info("需要回滚全局事务数量:{} 编号集合:{}", failedRecords.size(), failedRecords.stream().map(gt -> gt.getId()).collect(toList()));

        if (!CollectionUtils.isEmpty(failedRecords)){
            for (GlobalTransaction globalTransaction : failedRecords) {
                List<GlobalTransactionProcess> globalTransactionProcesses = globalTransactionProcessMapper.findSuccessProcess(globalTransaction.getId());

                LOGGER.info("需要回滚全局事务编号:{} 事务过程数量:{} 事务过程编号集合:{}", globalTransaction.getId(), globalTransactionProcesses.size(), globalTransactionProcesses.stream().map(gt -> gt.getId()).collect(toList()));

                if (CollectionUtils.isEmpty(globalTransactionProcesses)){
                    //如果事务过程为空，则说明该全局事务不需要再做处理，直接修改状态
                    GlobalTransaction update = new GlobalTransaction();
                    update.setId(globalTransaction.getId());
                    update.setCurrSequence(0);
                    update.setStatus(GlobalTransactionsStatusEnum.HasRollback);

                    globalTransactionMapper.updateByPrimaryKeySelective(update);
                    continue;
                }

                int i = 0;
                for (;  i < globalTransactionProcesses.size(); i++) {
                    GlobalTransactionProcess globalTransactionProcess = globalTransactionProcesses.get(i);

                    LOGGER.info("需要回滚全局事务编号:{} 事务过程编号:{} 事务过程序号:{} 事务过程原状态:{} 事务过程期望状态:{} 动作:开始处理",
                            globalTransaction.getId(), globalTransactionProcess.getId(), globalTransactionProcess.getTransactionSequence(),
                            globalTransactionProcess.getStatus().name(), GlobalTransactionProcessExpectedStatusEnum.HasRollback.name());


                    if (globalTransactionProcess.getNextRetryTime().after(new Date())){
                        // 未到下次处理时间，跳出
                        LOGGER.info("需要回滚全局事务编号:{} 事务过程编号:{} 事务过程序号:{} 未到下次处理时间，跳出", globalTransaction.getId(), globalTransactionProcess.getId(), globalTransactionProcess.getTransactionSequence());
                        break;
                    }

                    if (globalTransactionProcess.getExpectedStatus() != GlobalTransactionProcessExpectedStatusEnum.HasRollback){
                        //更新process的期望状态为“已回滚”
                        GlobalTransactionProcess update = new GlobalTransactionProcess();
                        update.setId(globalTransactionProcess.getId());
                        update.setExpectedStatus(GlobalTransactionProcessExpectedStatusEnum.HasRollback);

                        globalTransactionProcessMapper.updateByPrimaryKeySelective(update);
                    }

                    String responseJson = null;
                    //call roll back method
                    try {
                        Class rollbackClazz = Class.forName(globalTransactionProcess.getServiceName());
                        Object o = ApplicationContextHolder.getBean(rollbackClazz);
                        Class clazz = o.getClass();
                        String rollbackMethodName = globalTransactionProcess.getRollbackMethodName();


                        Method[] methods = clazz.getDeclaredMethods();
                        for (Method method : methods) {
                            if (Objects.equals(method.getName(),rollbackMethodName)){
                                Object object = method.invoke(o);
                                responseJson = JSONObject.toJSONString(object);
                            }
                        }

                        //更新事务过程表为已回滚
                        GlobalTransactionProcess update = new GlobalTransactionProcess();
                        update.setId(globalTransactionProcess.getId());
                        update.setExpectedStatus(GlobalTransactionProcessExpectedStatusEnum.HasRollback);
                        update.setResponseJson(responseJson);
                        globalTransactionProcessMapper.updateByPrimaryKeySelective(update);

                        LOGGER.info("需要回滚全局事务编号:{} 事务过程编号:{} 事务过程序号:{} 事务过程原状态:{} 事务过程期望状态:{} 动作:已完成",
                                globalTransaction.getId(), globalTransactionProcess.getId(), globalTransactionProcess.getTransactionSequence(),
                                globalTransactionProcess.getStatus().name(), GlobalTransactionProcessExpectedStatusEnum.HasRollback.name());
                    }catch (Exception e){
                        LOGGER.info("需要回滚全局事务编号:{} 事务过程编号:{} 事务过程序号:{} 事务过程原状态:{} 事务过程期望状态:{} 动作:异常({})",
                                globalTransaction.getId(), globalTransactionProcess.getId(), globalTransactionProcess.getTransactionSequence(),
                                globalTransactionProcess.getStatus().name(), GlobalTransactionProcessExpectedStatusEnum.HasRollback.name(), e);

                        //更新事务过程表的重试次数和下次重试时间
                        GlobalTransactionProcess update = new GlobalTransactionProcess();
                        update.setId(globalTransactionProcess.getId());
                        update.setRetryTimeCount(globalTransactionProcess.getRetryTimeCount() + 1);
                        update.setNextRetryTime(new Date(new Date().getTime() + (30 * 1000)));
                        globalTransactionProcessMapper.updateByPrimaryKeySelective(update);

                        break;
                    }
                }


                if (i == 0){
                    LOGGER.info("需要回滚全局事务编号:{} 跳过", globalTransaction.getId());

                    continue;
                }else if (i == globalTransactionProcesses.size()){
                    //已回滚
                    GlobalTransaction update = new GlobalTransaction();
                    update.setId(globalTransaction.getId());
                    update.setStatus(GlobalTransactionsStatusEnum.HasRollback);
                    update.setCurrSequence(i > 0 ? globalTransactionProcesses.get(i - 1).getTransactionSequence() : 0);
                    globalTransactionMapper.updateByPrimaryKeySelective(update);

                    LOGGER.info("需要回滚全局事务编号:{} 已完成", globalTransaction.getId());
                }else {
                    //部分回滚
                    GlobalTransaction update = new GlobalTransaction();
                    update.setId(globalTransaction.getId());
                    update.setStatus(GlobalTransactionsStatusEnum.PartiallyRollback);
                    update.setCurrSequence(i >= 0 ? globalTransactionProcesses.get(i - 1).getTransactionSequence() : 0);
                    globalTransactionMapper.updateByPrimaryKeySelective(update);

                    LOGGER.info("需要回滚全局事务编号:{} 部分完成", globalTransaction.getId());
                }

            }
        }

        /**
         * 处理所有全局事务状态为成功，但对应子过程中存在失败状态的记录，这种情况下，应该对子过程顺序做向前处理
         */
        List<GlobalTransaction> globalTransactionList = globalTransactionMapper.findSuccessWithFailedProcessGlobals();

        if (!CollectionUtils.isEmpty(globalTransactionList)){
            LOGGER.info("需要向前全局事务数量:{} 编号集合:{}", globalTransactionList.size(), globalTransactionList.stream().map(gt -> gt.getId()).collect(toList()));

            for (GlobalTransaction globalTransaction : globalTransactionList) {
                if (globalTransaction.getStatus() != GlobalTransactionsStatusEnum.Success){
                    continue;
                }

                List<GlobalTransactionProcess> failedProcessList = globalTransactionProcessMapper.findFailedProcess(globalTransaction.getId());

                if (CollectionUtils.isEmpty(failedProcessList)){
                    return;
                }

                LOGGER.info("需要向前全局事务编号:{} 事务过程数量:{} 事务过程编号集合:{}", globalTransaction.getId(), failedProcessList.size(), failedProcessList.stream().map(gt -> gt.getId()).collect(toList()));

                int i = 0;

                for (; i < failedProcessList.size(); i++){
                    GlobalTransactionProcess globalTransactionProcess = failedProcessList.get(i);

                    LOGGER.info("需要向前全局事务编号:{} 事务过程编号:{} 事务过程序号:{} 事务过程原状态:{} 事务过程期望状态:{} 动作:开始处理",
                            globalTransaction.getId(), globalTransactionProcess.getId(), globalTransactionProcess.getTransactionSequence(),
                            globalTransactionProcess.getStatus().name(), GlobalTransactionProcessExpectedStatusEnum.Success.name());

                    if (globalTransactionProcess.getNextRetryTime().after(new Date())){
                        LOGGER.info("需要向前全局事务编号:{} 事务过程编号:{} 事务过程序号:{} 未到下次处理时间，跳出", globalTransaction.getId(), globalTransactionProcess.getId(), globalTransactionProcess.getTransactionSequence());
                        //未到下次处理时间，跳出
                        break;
                    }

                    //更新process的期望状态为“成功”
                    if (globalTransactionProcess.getExpectedStatus() != GlobalTransactionProcessExpectedStatusEnum.Success){
                        GlobalTransactionProcess update = new GlobalTransactionProcess();
                        update.setId(globalTransactionProcess.getId());
                        update.setExpectedStatus(GlobalTransactionProcessExpectedStatusEnum.Success);
                        globalTransactionProcessMapper.updateByPrimaryKeySelective(update);
                    }

                    String responseJson = null;
                    //call method
                    try {
                        //调用方法
                        Class serviceClazz = Class.forName(globalTransactionProcess.getServiceName());
                        Object o = ApplicationContextHolder.getBean(serviceClazz);
                        Class clazz = o.getClass();
                        String methodName = globalTransactionProcess.getMethodName();

                        Object[] params;
                        String requestJson = globalTransactionProcess.getRequestJson();
                        if (!StringUtils.isEmpty(requestJson)){
                            String[] arguments = requestJson.split(Constants.ARGUMENT_SPLIT);
                            params = new Object[arguments.length];

                            for (int j = 0; j < arguments.length; j++) {
                                params[j] = JSONObject.parse(arguments[j]);
                            }
                        }else {
                            params = null;
                        }

                        Method[] methods = clazz.getDeclaredMethods();
                        for (Method method : methods) {
                            if (Objects.equals(method.getName(),methodName)){
                                if (params == null){
                                    Object object = method.invoke(o);
                                    responseJson = JSONObject.toJSONString(object);
                                }else {
                                    Object object = method.invoke(o,params);
                                    responseJson = JSONObject.toJSONString(object);
                                }
                            }
                        }

                        //更新事务过程表为成功
                        GlobalTransactionProcess update = new GlobalTransactionProcess();
                        update.setId(globalTransactionProcess.getId());
                        update.setResponseJson(responseJson);
                        update.setStatus(GlobalTransactionProcessStatusEnum.Success);
                        globalTransactionProcessMapper.updateByPrimaryKeySelective(update);

                        LOGGER.info("需要向前全局事务编号:{} 事务过程编号:{} 事务过程序号:{} 事务过程原状态:{} 事务过程期望状态:{} 动作:已完成",
                                globalTransaction.getId(), globalTransactionProcess.getId(), globalTransactionProcess.getTransactionSequence(),
                                globalTransactionProcess.getStatus().name(), GlobalTransactionProcessExpectedStatusEnum.Success.name());

                    }catch (Exception e){
                        LOGGER.info("需要向前全局事务编号:{} 事务过程编号:{} 事务过程序号:{} 事务过程原状态:{} 事务过程期望状态:{} 动作:异常({})",
                                globalTransaction.getId(), globalTransactionProcess.getId(), globalTransactionProcess.getTransactionSequence(),
                                globalTransactionProcess.getStatus().name(), GlobalTransactionProcessExpectedStatusEnum.Success.name(), e.getMessage());

                        //更新事务过程表的重试次数和下次重试时间
                        GlobalTransactionProcess update = new GlobalTransactionProcess();
                        update.setId(globalTransactionProcess.getId());
                        update.setRetryTimeCount(globalTransactionProcess.getRetryTimeCount() + 1);
                        update.setNextRetryTime(new Date(new Date().getTime() + (30 * 1000)));
                        globalTransactionProcessMapper.updateByPrimaryKeySelective(update);
                        break;
                    }


                    if (i == 0){
                        LOGGER.info("需要向前全局事务编号:{} 跳过", globalTransaction.getId());
                        continue;
                    }else if (i == failedProcessList.size()){
                        LOGGER.info("需要向前全局事务编号:{} 已完成", globalTransaction.getId());
                        //已经全部向前
                    }else {
                        LOGGER.info("需要向前全局事务编号:{} 部分完成", globalTransaction.getId());
                        //部分向前，不更新状态
                    }
                }
            }
        }

        LOGGER.info("--- 事务管理器结束 ---");
    }

}
