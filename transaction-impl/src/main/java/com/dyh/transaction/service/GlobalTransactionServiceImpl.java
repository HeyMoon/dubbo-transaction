package com.dyh.transaction.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.dyh.transaction.api.GlobalTransactionService;
import com.dyh.transaction.dao.mapper.GlobalTransactionMapper;
import com.dyh.transaction.dao.model.GlobalTransaction;
import com.dyh.transaction.dto.GlobalTransactionDTO;
import com.dyh.transaction.enums.GlobalTransactionsStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author dyh
 * @created at 2017 12 18 16:15
 */
@Service(protocol = "dubbo", cluster = "failfast")
public class GlobalTransactionServiceImpl implements GlobalTransactionService {

    @Autowired
    private GlobalTransactionMapper globalTransactionMapper;


    @Override
    public GlobalTransactionDTO create(GlobalTransactionDTO dto) {
        GlobalTransaction globalTransaction = new GlobalTransaction();
        globalTransaction.setStatus(dto.getStatus());
        globalTransaction.setCreatedAt(dto.getCreatedAt());
        globalTransaction.setCurrSequence(dto.getCurrSequence());

        globalTransactionMapper.insert(globalTransaction);

        dto.setId(globalTransaction.getId());
        return dto;
    }

    @Override
    public Boolean update(Integer globalTransactionId, Integer currSequence, GlobalTransactionsStatusEnum status) {
        GlobalTransaction update = new GlobalTransaction();
        update.setId(globalTransactionId);
        update.setCurrSequence(currSequence);
        update.setStatus(status);

        int n = globalTransactionMapper.updateByPrimaryKeySelective(update);

        return n == 1;
    }
}
