package com.cc.bms.base.config.db;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态数据源变量存放与操作。
 * @author chenChao
 * @since JDK 1.8.0_172
 */
public class DynamicDataSourceContextHolder {

    /**
     * 数据源变量临时存放
     */
    private static final ThreadLocal<String> contextHolder = new ThreadLocal<>();

    /**
     * 记录数据源id
     */
    public static List<String> dataSourcesIds = new ArrayList<>();

    public static void set(String dataSourceType) {
        contextHolder.set(dataSourceType);
    }

    public static String get() {
        return contextHolder.get();
    }

    public static void remove() {
        contextHolder.remove();
    }

    public static Boolean contains(String dataSourceId) {
        return dataSourcesIds.contains(dataSourceId);
    }
}
