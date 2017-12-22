package com.dyh.transaction.enums;

public enum GlobalTransactionProcessStatusEnum implements BaseEnum {

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
    Unknown((byte)4),

    /**
     *
     **/
    HasRollback((byte)5);


    private byte code;

    @Override
    public byte getCode() {
        return code;
    }

    public void setCode(byte code) {
        this.code = code;
    }

    GlobalTransactionProcessStatusEnum(byte code) {
        this.code = code;
    }

    public static GlobalTransactionProcessStatusEnum findByCode(byte value) {
        switch (value) {

            case 1:
                return New;

            case 2:
                return Success;

            case 3:
                return Fail;

            case 4:
                return Unknown;

            case 5:
                return HasRollback;

            default:
                return null;
        }
    }
}
      