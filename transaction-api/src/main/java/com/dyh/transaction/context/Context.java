package com.dyh.transaction.context;

import lombok.Data;

/**
 * @author dyh
 * @created at 2017 12 18 15:52
 */
@Data
public class Context {

    private String calleeIp;

    private int calleePort;

    private long calleeTimeout;

    private Integer seqId;

    private int failedTimes = 0;

    private boolean isGlobalTransactionProcess;

    private boolean isGlobalTransactional;

    private Integer currentTransactionSequence = 0;

    private Integer transactionId = 0;

}
