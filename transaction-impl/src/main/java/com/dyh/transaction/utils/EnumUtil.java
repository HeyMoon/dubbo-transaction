package com.dyh.transaction.utils;

import com.dyh.transaction.enums.BaseEnum;

/**
 * @author dyh
 * @created at 2017 12 19 15:47
 */
public class EnumUtil {
    public static <E extends Enum<?> & BaseEnum> E codeOf(Class<E> enumClass, byte code) {
        E[] enumConstants = enumClass.getEnumConstants();
        for (E e : enumConstants) {
            if (e.getCode() == code)
                return e;
        }
        return null;
    }

}
