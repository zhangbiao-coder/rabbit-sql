package rabbit.sql.support;

import rabbit.sql.utils.JdbcUtil;
import rabbit.sql.utils.SqlUtil;
import rabbit.common.tuple.Pair;
import rabbit.common.types.DataRow;
import rabbit.sql.types.Param;
import rabbit.sql.types.ParamMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * jdbc基本操作支持
 */
public abstract class JdbcSupport {
    private final static Logger log = LoggerFactory.getLogger(JdbcSupport.class);

    /**
     * 设置数据源
     *
     * @return 数据源
     */
    protected abstract DataSource getDataSource();

    /**
     * 获取连接对象
     *
     * @return 连接对象
     */
    protected abstract Connection getConnection();

    /**
     * 释放连接的实现逻辑，比如在开启事务同步时，应保持连接，
     * 而不应该释放，在执行其他非事务操作完成时应立即释放
     *
     * @param connection 连接对象
     * @param dataSource 数据源
     */
    protected abstract void releaseConnection(Connection connection, DataSource dataSource);

    /**
     * 提供一个抽象方法供实现类对单前要执行的sql做一些准备操作
     *
     * @param sql sql
     * @return 处理后的sql
     */
    protected abstract String getSql(String sql);

    /**
     * 执行一句sql
     *
     * @param sql      sql
     * @param callback 执行声明回调函数
     * @param <T>      结果类型参数
     * @return 任意类型
     */
    public <T> T execute(final String sql, StatementCallback<T> callback) {
        CallableStatement statement = null;
        Connection connection = getConnection();
        try {
            statement = connection.prepareCall(sql);
            return callback.doInStatement(statement);
        } catch (SQLException e) {
            JdbcUtil.closeStatement(statement);
            statement = null;
            releaseConnection(connection, getDataSource());
            throw new RuntimeException(String.format("execute target sql[%s]:%s", sql, e.getMessage()));
        } finally {
            JdbcUtil.closeStatement(statement);
            releaseConnection(connection, getDataSource());
        }
    }

    /**
     * 批量执行 insert，update，delete（e.g. insert into table (a,b,c) values (:v1,:v2,:v3)）
     *
     * @param sql  sql
     * @param args 数据
     * @return 总的受影响的行数
     */
    public int executeNonQuery(final String sql, final Collection<Map<String, Param>> args) {
        if (args == null || args.size() < 1) {
            throw new NoSuchElementException("args is null or length less than 1.");
        }
        Map<String, Param> firstArg = args.stream().findFirst().get();
        String sourceSql = SqlUtil.resolveSqlPart(getSql(sql), firstArg);
        log.debug("SQL:{}", sourceSql);
        log.debug("Args:{}", args);

        Pair<String, List<String>> preparedSqlAndArgNames = SqlUtil.getPreparedSqlAndIndexedArgNames(sourceSql);
        final List<String> argNames = preparedSqlAndArgNames.getItem2();
        final String preparedSql = preparedSqlAndArgNames.getItem1();

        return execute(preparedSql, sc -> {
            int i = 0;
            for (Map<String, Param> arg : args) {
                JdbcUtil.registerParams(sc, arg, argNames);
                i += sc.executeUpdate();
            }
            log.info("{} rows updated!", i);
            return i;
        });
    }

    /**
     * 执行一句非查询语句
     *
     * @param sql sql
     * @param arg 参数
     * @return 受影响的行数
     */
    public int executeNonQuery(final String sql, final Map<String, Param> arg) {
        return executeNonQuery(sql, Collections.singletonList(arg));
    }

    /**
     * 执行一句非查询语句
     *
     * @param sql  sql
     * @param args 一组参数
     * @return 受影响的行数
     */
    public int executeNonQueryOfDataRow(final String sql, final Collection<DataRow> args) {
        if (args == null || args.size() < 1) {
            throw new NoSuchElementException("args is null or length less than 1.");
        }
        Map<String, Param> firstArg = args.stream()
                .map(r -> r.toMap(Param::IN))
                .findFirst().get();
        String sourceSql = SqlUtil.resolveSqlPart(getSql(sql), firstArg);
        log.debug("SQL:{}", sourceSql);
        log.debug("Args:{}", args);

        Pair<String, List<String>> preparedSqlAndArgNames = SqlUtil.getPreparedSqlAndIndexedArgNames(sourceSql);
        final List<String> argNames = preparedSqlAndArgNames.getItem2();
        final String preparedSql = preparedSqlAndArgNames.getItem1();

        return execute(preparedSql, sc -> {
            int i = 0;
            for (DataRow row : args) {
                Map<String, Param> arg = row.toMap(Param::IN);
                JdbcUtil.registerParams(sc, arg, argNames);
                i += sc.executeUpdate();
            }
            log.info("{} rows updated!", i);
            return i;
        });
    }

    /**
     * 执行一句非查询语句
     *
     * @param sql sql
     * @param arg 参数
     * @return 受影响的行数
     */
    public int executeNonQueryOfDataRow(final String sql, final DataRow arg) {
        return executeNonQueryOfDataRow(sql, Collections.singletonList(arg));
    }

    /**
     * 查询
     *
     * @param sql        sql
     * @param convert    类型转换
     * @param fetchSize  请求条数
     * @param args       参数 （占位符名字，参数对象）
     * @param ICondition 条件
     * @param <T>        类型参数
     * @return 结果集
     */
    public <T> Stream<T> query(final String sql, Function<DataRow, T> convert, final long fetchSize, Map<String, Param> args, ICondition ICondition) {
        Map<String, Param> params = new HashMap<>();
        String sourceSql = getSql(sql);
        if (args != null)
            params.putAll(args);
        if (ICondition != null) {
            params.putAll(ICondition.getParams());
            sourceSql += ICondition.getSql();
        }
        sourceSql = SqlUtil.resolveSqlPart(sourceSql, params);
        log.debug("SQL：{}", sourceSql);
        log.debug("Args:{}", params);

        Pair<String, List<String>> preparedSqlAndArgNames = SqlUtil.getPreparedSqlAndIndexedArgNames(sourceSql);
        final List<String> argNames = preparedSqlAndArgNames.getItem2();
        final String preparedSql = preparedSqlAndArgNames.getItem1();

        return execute(preparedSql, sc -> {
            JdbcUtil.registerParams(sc, params, argNames);
            ResultSet resultSet = sc.executeQuery();
            return JdbcUtil.resolveResultSet(resultSet, fetchSize, convert);
        });
    }

    /**
     * 执行存储过程或函数
     *
     * @param sql  sql
     * @param args 参数
     * @return DataRow
     */
    public DataRow executeCall(final String sql, Map<String, Param> args) {
        String sourceSql = SqlUtil.resolveSqlPart(getSql(sql), args);
        log.debug("SQL：{}", sourceSql);
        log.debug("Args：{}", args);

        Pair<String, List<String>> preparedSqlAndArgNames = SqlUtil.getPreparedSqlAndIndexedArgNames(sourceSql);
        final String executeSql = preparedSqlAndArgNames.getItem1();
        final List<String> argNames = preparedSqlAndArgNames.getItem2();

        return execute(executeSql, sc -> {
            JdbcUtil.registerParams(sc, args, argNames);
            sc.execute();
            Param[] params = args.values().toArray(new Param[0]);
            String[] fields = new String[params.length];
            Object[] values = new Object[params.length];
            String[] types = new String[params.length];
            for (int i = 0; i < params.length; i++) {
                if (params[i].getParamMode() == ParamMode.OUT || params[i].getParamMode() == ParamMode.IN_OUT) {
                    Object result = sc.getObject(i + 1);
                    fields[i] = "column" + i;
                    if (result instanceof ResultSet) {
                        Stream<DataRow> rowStream = JdbcUtil.resolveResultSet((ResultSet) result, -1, row -> row);
                        values[i] = rowStream;
                        types[i] = "java.util.stream.Stream<DataRow>";
                        log.info("boxing a result with type: cursor,convert to Stream<DataRow>,get result by name:{} or index:{}!", fields[i], i);
                    } else {
                        values[i] = result;
                        types[i] = result.getClass().getName();
                        log.info("boxing a result with type:{},get result by name:{} or index:{}!", types[i], fields[i], i);
                    }
                }
            }
            return DataRow.of(fields, types, values);
        });
    }
}
