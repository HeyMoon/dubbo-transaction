package com.dyh.transaction.filter;

import com.alibaba.dubbo.rpc.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dyh.transaction.annotation.GlobalTransactionalProcess;
import com.dyh.transaction.api.GlobalTransactionProcessService;
import com.dyh.transaction.constant.Constants;
import com.dyh.transaction.context.Context;
import com.dyh.transaction.context.InheritableRpcContextAdapter;
import com.dyh.transaction.dto.GlobalTransactionProcessDTO;
import com.dyh.transaction.enums.GlobalTransactionProcessExpectedStatusEnum;
import com.dyh.transaction.enums.GlobalTransactionProcessStatusEnum;
import com.dyh.transaction.utils.ApplicationContextHolder;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Objects;

/**
 * @author dyh
 * @created at 2017 12 18 15:22
 */
@Slf4j
public class DubboConsumerFilter implements Filter{


    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        System.out.println("DubboConsumerFilter....... interface:" + invoker.getInterface().getSimpleName() + ",method:" + invocation.getMethodName());
        Class interfaceClass = invoker.getInterface();
        String methodName = invocation.getMethodName();

        RpcInvocation rpcInvocation = (RpcInvocation) invocation;

        Method[] methods = interfaceClass.getMethods();

        GlobalTransactionProcessService globalTransactionProcessService = null;
        Context context = null;
        GlobalTransactionProcessDTO transactionProcess = null;

        boolean success = false;
        boolean unknown = false;

        for (Method method : methods) {
            if (Objects.equals(methodName,method.getName())){
                boolean isTransactionProcess = method.isAnnotationPresent(GlobalTransactionalProcess.class);

                if (isTransactionProcess){
                    String contextJson = rpcInvocation.getAttachment(Constants.TRANSACTION_CONTEXT);
                    if (contextJson == null){
                        contextJson = InheritableRpcContextAdapter.getContext().get(Constants.TRANSACTION_CONTEXT);
                    }

                    if (contextJson != null){
                        context = JSON.parseObject(contextJson,Context.class);

                        if (context != null){
                            boolean isGlobalTransactional = context.isGlobalTransactional();

                            if (isGlobalTransactional){
                                transactionProcess =  new GlobalTransactionProcessDTO();

                                transactionProcess.setCreatedAt(new Date());
                                transactionProcess.setExpectedStatus(GlobalTransactionProcessExpectedStatusEnum.Success);
                                transactionProcess.setMethodName(methodName);
                                transactionProcess.setNextRetryTime(new Date(new Date().getTime() + 30 * 1000));
                                transactionProcess.setRetryTimeCount(0);

                                Object[] arguments = rpcInvocation.getArguments();
                                if (arguments != null && arguments.length > 0){
                                    StringBuilder stringBuilder = new StringBuilder(arguments.length);
                                    for (Object a : arguments) {
                                        stringBuilder.append(JSONObject.toJSONString(a)).append(Constants.ARGUMENT_SPLIT);
                                    }

                                    if (stringBuilder.length() > 0){
                                        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                                    }

                                    transactionProcess.setRequestJson(stringBuilder.toString());
                                }

                                transactionProcess.setResponseJson("");
                                transactionProcess.setRollbackMethodName(methodName + "_rollback");
                                transactionProcess.setServiceName(interfaceClass.getName());
                                transactionProcess.setStatus(GlobalTransactionProcessStatusEnum.New);
                                transactionProcess.setTransactionId(context.getTransactionId());
                                transactionProcess.setTransactionSequence(context.getCurrentTransactionSequence() + 1);
                                transactionProcess.setVersionName("");

                                context.setCurrentTransactionSequence(context.getCurrentTransactionSequence() + 1);
                                rpcInvocation.setAttachment(Constants.TRANSACTION_CONTEXT,JSONObject.toJSONString(context));
                                InheritableRpcContextAdapter.getContext().put(Constants.TRANSACTION_CONTEXT,JSONObject.toJSONString(context));
                                try {
                                    globalTransactionProcessService = ApplicationContextHolder.getBean(GlobalTransactionProcessService.class);

                                    GlobalTransactionProcessDTO globalTransactionProcessDTO = globalTransactionProcessService.create(transactionProcess);
                                    transactionProcess.setId(globalTransactionProcessDTO.getId());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    log.error("DubboConsumerFilter occurs exception:\n{}",e);
                                }
                            }
                        }
                    }
                }
            }
        }

        Result result = null;
        try {
            result = invoker.invoke(rpcInvocation);
            success = true;
        }catch (RpcException e){
            e.printStackTrace();
            log.error("DubboConsumerFilter occurs exception:\n{}",e);
            unknown = true;
        }catch (Exception e){
            e.printStackTrace();
            log.error("DubboConsumerFilter occurs exception:\n{}",e);
            unknown = false;
        }
        finally {
            if (transactionProcess != null && globalTransactionProcessService != null && context != null){
                final GlobalTransactionProcessStatusEnum status = success ? GlobalTransactionProcessStatusEnum.Success : (unknown ? GlobalTransactionProcessStatusEnum.Unknown : GlobalTransactionProcessStatusEnum.Fail);

                if (transactionProcess.getId() != null) {
                    try {
                        System.out.println("globalTransactionProcessId:" + transactionProcess.getId() + "success:" + success + "");
                        globalTransactionProcessService.update(transactionProcess.getId(), result == null ? "" : JSONObject.toJSONString(result), status);
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("DubboConsumerFilter occurs exception:\n{}",e);
                    }
                }
            }
        }

        return result;
    }

}