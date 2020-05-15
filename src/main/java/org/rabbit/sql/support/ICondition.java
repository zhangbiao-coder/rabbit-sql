package org.rabbit.sql.support;

import org.rabbit.sql.types.Param;

import java.util.Map;

/**
 * 条件拼接接口
 */
public interface ICondition {
    /**
     * 获取条件拼装器中的参数
     *
     * @return 参数
     */
    Map<String, Param> getParams();

    /**
     * 获取条件sql字符串
     *
     * @return sql
     */
     String getString();
}