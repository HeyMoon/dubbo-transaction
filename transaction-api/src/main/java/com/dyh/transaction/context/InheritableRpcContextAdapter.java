package com.dyh.transaction.context;

/**
 * @author dyh
 * @created at 2017 12 21 16:58
 */
public class InheritableRpcContextAdapter {

    private static final InheritableRpcContext SINGLETON = new InheritableRpcContext();

    public static InheritableRpcContext getContext() {
        return SINGLETON;
    }

}