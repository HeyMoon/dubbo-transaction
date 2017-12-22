package com.dyh.transaction.enums;

public enum GlobalTransactionsStatusEnum implements BaseEnum {

    /**
     *
     **/
    New((byte)1),

    /**
     *
     **/
    Success((byte)2),

    /**
     *
     **/
    Fail((byte)3),

    /**
     *
     **/
    HasRollback((byte)4),

    /**
     *
     **/
    PartiallyRollback((byte)5),

    /**
     *
     **/
    Suspend((byte)99);


    private byte code;

    GlobalTransactionsStatusEnum(byte code) {
        this.code = code;
    }

    @Override
    public byte getCode() {
        return code;
    }

    public void setCode(byte code) {
        this.code = code;
    }

    public static GlobalTransactionsStatusEnum findByCode(byte value) {
        switch (value) {

            case 1:
                return New;

            case 2:
                return Success;

            case 3:
                return Fail;

            case 4:
                return HasRollback;

            case 5:
                return PartiallyRollback;

            case 99:
                return Suspend;

            default:
                return null;
        }
    }
}
      