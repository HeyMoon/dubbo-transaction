package com.dyh.transaction.api;

import com.dyh.transaction.dto.GlobalTransactionProcessDTO;
import com.dyh.transaction.enums.GlobalTransactionProcessStatusEnum;

public interface GlobalTransactionProcessService {


    GlobalTransactionProcessDTO create(GlobalTransactionProcessDTO globalTransactionProcess);

    Boolean update(Integer globalTransactionProcessId, String responseJson, GlobalTransactionProcessStatusEnum status);

}