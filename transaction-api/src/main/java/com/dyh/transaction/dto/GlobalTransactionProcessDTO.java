package com.dyh.transaction.dto;

import com.dyh.transaction.enums.GlobalTransactionProcessExpectedStatusEnum;
import com.dyh.transaction.enums.GlobalTransactionProcessStatusEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author dyh
 * @created at 2017 12 19 15:35
 */
@Data
public class GlobalTransactionProcessDTO implements Serializable{

    /**
     *
     */
    private Integer id;

    /**
     *
     */
    private Integer transactionId;

    /**
     * 过程所属序列号
     */
    private Integer transactionSequence;

    /**
     * 过程当前状态，1：新建；2：成功；3：失败；4：未知，5：已回滚；
     */
    private GlobalTransactionProcessStatusEnum status;

    /**
     * 过程目标状态，1：成功；2：已回滚；
     */
    private GlobalTransactionProcessExpectedStatusEnum expectedStatus;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务版本
     */
    private String versionName;

    /**
     * 方法名称
     */
    private String methodName;

    /**
     * 回滚方法名称
     */
    private String rollbackMethodName;

    /**
     * 重试次数
     */
    private Integer retryTimeCount;

    /**
     * 下次重试时间
     */
    private Date nextRetryTime;

    /**
     *
     */
    private Date createdAt;

    /**
     *
     */
    private Date updatedAt;

    /**
     * 过程请求参数Json序列化
     */
    private String requestJson;

    /**
     * 过程响应参数Json序列化
     */
    private String responseJson;

}
