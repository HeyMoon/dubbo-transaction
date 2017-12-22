package com.dyh.transaction.annotation;

import java.lang.annotation.*;

/**
 * Created by tangliu on 2016/4/11.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GlobalTransactional {

}
