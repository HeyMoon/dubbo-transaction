package com.dyh.transaction.enums;


public enum GlobalTransactionProcessExpectedStatusEnum implements BaseEnum {

    /**
     *
     **/
    Success((byte)1),

    /**
     *
     **/
    HasRollback((byte)2);


    private byte code;

    @Override
    public byte getCode() {
        return code;
    }

    public void setCode(byte code) {
        this.code = code;
    }

    private GlobalTransactionProcessExpectedStatusEnum(byte code) {
        this.code = code;
    }

    public static GlobalTransactionProcessExpectedStatusEnum findByCode(byte value) {
        switch (value) {

            case 1:
                return Success;

            case 2:
                return HasRollback;

            default:
                return null;
        }
    }
}
      