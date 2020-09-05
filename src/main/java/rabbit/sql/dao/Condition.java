package rabbit.sql.dao;

import rabbit.common.tuple.Pair;
import rabbit.sql.support.ICondition;
import rabbit.sql.support.IFilter;
import rabbit.sql.utils.SqlUtil;

import java.util.HashMap;
import java.util.Map;

import static rabbit.sql.utils.SqlUtil.unwrapValue;

/**
 * SQL条件拼装器
 */
public class Condition implements ICondition {
    private final Map<String, Object> args = new HashMap<>();
    private final StringBuilder conditions = new StringBuilder();
    private int arg_index = 0;

    Condition() {

    }

    /**
     * where
     *
     * @param filter 过滤器
     * @return 条件拼接器
     */
    public static Condition where(IFilter filter) {
        Condition cnd = new Condition();
        return cnd.concatFilterBy("", filter);
    }

    /**
     * 一段原生sql表达式
     *
     * @param sql sql
     * @return 条件拼接器
     */
    public Condition expression(String sql) {
        conditions.append(sql).append(" ");
        return this;
    }

    /**
     * and
     *
     * @param filter 过滤器
     * @return 条件拼接器
     */
    public Condition and(IFilter filter) {
        return concatFilterBy("and ", filter);
    }

    /**
     * or
     *
     * @param filter 过滤器
     * @return 条件拼接器
     */
    public Condition or(IFilter filter) {
        return concatFilterBy("or ", filter);
    }

    /**
     * 通过某个字符串对多组条件进行连接
     *
     * @param s      连接字符串
     * @param filter 过滤器
     * @return 条件拼接器
     */
    private Condition concatFilterBy(String s, IFilter filter) {
        if (filter.getValue() != IFilter.IGNORE_VALUE) {
            Pair<String, String> sf = getSpecialField(filter.getField(), filter.getValue());
            conditions.append(s).append(filter.getField()).append(filter.getOperator()).append(sf.getItem2()).append(" ");
            args.put(sf.getItem1(), unwrapValue(filter.getValue()));
        } else {
            conditions.append(s).append(filter.getField()).append(filter.getOperator());
        }
        return this;
    }

    /**
     * 获取经过特殊字符和自动编号处理的字段名占位符
     *
     * @param field 字段名
     * @param value 字段值
     * @return (字段名, 字段名占位符)
     */
    private Pair<String, String> getSpecialField(String field, Object value) {
        String s = field + SqlUtil.SEP + arg_index++;
        if (value instanceof Wrap) {
            Wrap wrapV = (Wrap) value;
            String prefix = wrapV.getStart();
            if (!prefix.equals("")) {
                prefix += " ";
            }
            return Pair.of(s, prefix + ":" + s + wrapV.getEnd());
        }
        return Pair.of(s, ":" + s);
    }

    /**
     * 获取参数
     *
     * @return 参数
     */
    @Override
    public Map<String, Object> getArgs() {
        return args;
    }

    /**
     * 获取拼接的where子句
     *
     * @return where子句
     */
    @Override
    public String getSql() {
        String cnds = conditions.toString();
        if (cnds.startsWith("and ")) {
            cnds = cnds.substring(4);
        } else if (cnds.startsWith("or ")) {
            cnds = cnds.substring(3);
        }
        return "\n where " + cnds;
    }
}
