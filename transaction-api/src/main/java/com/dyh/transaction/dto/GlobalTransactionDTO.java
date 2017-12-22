package com.dyh.transaction.dto;

import com.dyh.transaction.enums.GlobalTransactionsStatusEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author dyh
 * @created at 2017 12 19 15:34
 */
@Data
public class GlobalTransactionDTO implements Serializable{
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
