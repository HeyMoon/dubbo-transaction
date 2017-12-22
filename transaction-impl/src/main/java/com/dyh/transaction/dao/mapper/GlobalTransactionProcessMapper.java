package com.dyh.transaction.dao.mapper;

import com.dyh.transaction.dao.model.GlobalTransactionProcess;

import java.util.List;

/**
 * Interface for table -- global_transaction_process
 * Created by ASUS on 2017-25-18 16:25:36
 */
public interface GlobalTransactionProcessMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(GlobalTransactionProcess record);

    int insertSelective(GlobalTransactionProcess record);

    GlobalTransactionProcess selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(GlobalTransactionProcess record);

    List<GlobalTransactionProcess> findSuccessProcess(Integer transactionId);

    List<GlobalTransactionProcess> findFailedProcess(Integer transactionId);
}