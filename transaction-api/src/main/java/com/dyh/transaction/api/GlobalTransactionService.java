package com.dyh.transaction.api;

import com.dyh.transaction.dto.GlobalTransactionDTO;
import com.dyh.transaction.enums.GlobalTransactionsStatusEnum;

public interface GlobalTransactionService {


    GlobalTransactionDTO create(GlobalTransactionDTO globalTransaction);

    Boolean update(Integer globalTransactionId, Integer currSequence, GlobalTransactionsStatusEnum status);


}