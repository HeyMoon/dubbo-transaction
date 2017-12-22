package com.dyh.transaction.dao.model;

import com.dyh.transaction.enums.GlobalTransactionsStatusEnum;
import lombok.Data;

import java.util.Date;

/**
 * Model for table -- global_transaction
 * Created by ASUS on 2017-25-18 16:25:36
 */
@Data
public class GlobalTransaction {
    /**
     * 
     */
    private Integer id;

    /**
     * 状态，1：新建；2：成功；3：失败；4：已回滚；5：已部分回滚；99：挂起；
     */
    private GlobalTransactionsStatusEnum status;

    /**
     * 当前过程序列号
     */
    private Integer currSequence;

    /**
     * 
     */
    private Date createdAt;

    /**
     * 
     */
    private Date updatedAt;

}