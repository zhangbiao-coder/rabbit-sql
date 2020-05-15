package org.rabbit.sql.types;

import org.rabbit.sql.support.IOutParam;

/**
 * 参数对象
 */
public final class Param {
    private Object value;
    private IOutParam type;
    private ParamMode paramMode;

    /**
     * 入参
     *
     * @param value 值 (可以是明确的值或包装值)
     * @return param
     * @see ValueWrap
     */
    public static Param IN(Object value) {
        Param param = new Param();
        param.value = value;
        param.paramMode = ParamMode.IN;
        return param;
    }

    /**
     * 出参 （仅用于存储过程和函数）
     *
     * @param type 出参类型
     * @return param
     */
    public static Param OUT(IOutParam type) {
        Param param = new Param();
        param.type = type;
        param.paramMode = ParamMode.OUT;
        return param;
    }

    /**
     * 包含（出参和入参）
     *
     * @param value 入参值 (可以是明确的值或包装值)
     * @param type  出参类型
     * @return param
     * @see ValueWrap
     */
    public static Param IN_OUT(Object value, IOutParam type) {
        Param param = new Param();
        param.paramMode = ParamMode.IN_OUT;
        param.type = type;
        param.value = value;
        return param;
    }

    /**
     * 指定SQL模版代码段插入到指定位置
     *
     * @param template SQL模版
     * @return param
     */
    public static Param TEMPLATE(String template) {
        Param param = new Param();
        param.paramMode = ParamMode.TEMPLATE;
        param.value = template;
        return param;
    }

    public Object getValue() {
        return value;
    }

    public ParamMode getParamMode() {
        return paramMode;
    }

    public IOutParam getType() {
        return type;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
