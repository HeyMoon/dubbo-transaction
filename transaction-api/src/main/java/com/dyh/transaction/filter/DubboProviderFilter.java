package com.dyh.transaction.filter;

import com.alibaba.dubbo.rpc.*;
import com.alibaba.fastjson.JSONObject;
import com.dyh.transaction.annotation.GlobalTransactional;
import com.dyh.transaction.api.GlobalTransactionService;
import com.dyh.transaction.constant.Constants;
import com.dyh.transaction.context.Context;
import com.dyh.transaction.context.InheritableRpcContextAdapter;
import com.dyh.transaction.dto.GlobalTransactionDTO;
import com.dyh.transaction.enums.GlobalTransactionsStatusEnum;
import com.dyh.transaction.utils.ApplicationContextHolder;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Objects;

/**
 * @author dyh
 * @created at 2017 12 18 15:21
 */
public class DubboProviderFilter implements Filter{

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        System.out.println("DubboProviderFilter....... interface:" + invoker.getInterface().getSimpleName() + ",method:" + invocation.getMethodName());
        Class interfaceClass = invoker.getInterface();
        String methodName = invocation.getMethodName();

        RpcInvocation rpcInvocation = (RpcInvocation) invocation;

        String json = rpcInvocation.getAttachment(Constants.TRANSACTION_CONTEXT);
        if (json != null){
            InheritableRpcContextAdapter.getContext().put(Constants.TRANSACTION_CONTEXT,json);
        }


        Method[] methods = interfaceClass.getDeclaredMethods();

        boolean isGlobalTransaction = false;

        for (Method m : methods){
            if (Objects.equals(methodName,m.getName()) && m.isAnnotationPresent(GlobalTransactional.class)){
                isGlobalTransaction = true;
            }
        }

        GlobalTransactionService globalTransactionService = null;
        GlobalTransactionDTO globalTransaction = null;
        Context context = null;

        if (isGlobalTransaction){
            context = new Context();
            context.setGlobalTransactional(true);

            globalTransactionService = ApplicationContextHolder.getBean(GlobalTransactionService.class);

            globalTransaction = new GlobalTransactionDTO();
            globalTransaction.setCurrSequence(0);
            globalTransaction.setCreatedAt(new Date());
            globalTransaction.setStatus(GlobalTransactionsStatusEnum.New);

            GlobalTransactionDTO rst = globalTransactionService.create(globalTransaction);

            globalTransaction.setId(rst.getId());
            context.setCurrentTransactionSequence(0);
            context.setTransactionId(rst.getId());

            rpcInvocation.setAttachment(Constants.TRANSACTION_CONTEXT, JSONObject.toJSONString(context));
            InheritableRpcContextAdapter.getContext().put(Constants.TRANSACTION_CONTEXT,JSONObject.toJSONString(context));
        }

        boolean success;
        Result result = null;
        try {
            result = invoker.invoke(rpcInvocation);
            success = true;
        }catch (Exception e){
            success = false;
        }finally {
            InheritableRpcContextAdapter.getContext().clear();
        }

        if (globalTransactionService != null && globalTransaction != null && context != null){
            System.out.println("globalTransactionId:" + globalTransaction.getId() + ",currentTransactionSequence:" + context.getCurrentTransactionSequence() + "success:" + success);
            globalTransactionService.update(globalTransaction.getId(), context.getCurrentTransactionSequence(), success ? GlobalTransactionsStatusEnum.Success : GlobalTransactionsStatusEnum.Fail);
        }

        return result;
    }

}