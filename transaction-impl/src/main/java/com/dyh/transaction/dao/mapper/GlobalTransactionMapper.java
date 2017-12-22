package com.dyh.transaction.dao.mapper;

import com.dyh.transaction.dao.model.GlobalTransaction;

import java.util.List;

/**
 * Interface for table -- global_transaction
 * Created by ASUS on 2017-25-18 16:25:36
 */
public interface GlobalTransactionMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(GlobalTransaction record);

    int insertSelective(GlobalTransaction record);

    GlobalTransaction selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(GlobalTransaction record);

    List<GlobalTransaction> findFailedRecords();

    List<GlobalTransaction> findSuccessWithFailedProcessGlobals();
}