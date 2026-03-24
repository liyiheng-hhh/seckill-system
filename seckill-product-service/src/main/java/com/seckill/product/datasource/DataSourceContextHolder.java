package com.seckill.product.datasource;

public final class DataSourceContextHolder {

    private static final ThreadLocal<DataSourceType> CONTEXT = new ThreadLocal<>();

    private DataSourceContextHolder() {
    }

    public static void useMaster() {
        CONTEXT.set(DataSourceType.MASTER);
    }

    public static void useSlave() {
        CONTEXT.set(DataSourceType.SLAVE);
    }

    public static DataSourceType getCurrent() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
