package com.dyh.transaction.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.dyh.transaction.api.GlobalTransactionProcessService;
import com.dyh.transaction.dao.mapper.GlobalTransactionProcessMapper;
import com.dyh.transaction.dao.model.GlobalTransactionProcess;
import com.dyh.transaction.dto.GlobalTransactionProcessDTO;
import com.dyh.transaction.enums.GlobalTransactionProcessStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author dyh
 * @created at 2017 12 19 14:42
 */
@Service(protocol = "dubbo", cluster = "failfast")
public class GlobalTransactionProcessServiceImpl implements GlobalTransactionProcessService {

    @Autowired
    private GlobalTransactionProcessMapper globalTransactionProcessMapper;

    @Override
    public GlobalTransactionProcessDTO create(GlobalTransactionProcessDTO dto) {
        GlobalTransactionProcess globalTransactionProcess = new GlobalTransactionProcess();
        globalTransactionProcess.setCreatedAt(dto.getCreatedAt());
        globalTransactionProcess.setExpectedStatus(dto.getExpectedStatus());
        globalTransactionProcess.setMethodName(dto.getMethodName());
        globalTransactionProcess.setNextRetryTime(dto.getNextRetryTime());
        globalTransactionProcess.setRetryTimeCount(dto.getRetryTimeCount());
        globalTransactionProcess.setRequestJson(dto.getRequestJson());
        globalTransactionProcess.setResponseJson(dto.getResponseJson());
        globalTransactionProcess.setRollbackMethodName(dto.getRollbackMethodName());
        globalTransactionProcess.setServiceName(dto.getServiceName());
        globalTransactionProcess.setStatus(dto.getStatus());
        globalTransactionProcess.setTransactionId(dto.getTransactionId());
        globalTransactionProcess.setTransactionSequence(dto.getTransactionSequence());
        globalTransactionProcess.setVersionName(dto.getVersionName());
        globalTransactionProcessMapper.insert(globalTransactionProcess);
        dto.setId(globalTransactionProcess.getId());

        return dto;
    }

    @Override
    public Boolean update(Integer globalTransactionProcessId, String responseJson, GlobalTransactionProcessStatusEnum status) {
        GlobalTransactionProcess update = new GlobalTransactionProcess();
        update.setId(globalTransactionProcessId);
        update.setResponseJson(responseJson);
        update.setStatus(status);

        int n = globalTransactionProcessMapper.updateByPrimaryKeySelective(update);
        return n == 1;
    }
}
