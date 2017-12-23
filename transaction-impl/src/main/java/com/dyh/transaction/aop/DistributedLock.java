package com.dyh.transaction.aop;

import java.lang.annotation.*;

/**
 * Created by tangliu on 2016/4/11.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

}
