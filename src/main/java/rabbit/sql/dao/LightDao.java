package rabbit.sql.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.common.types.DataRow;
import rabbit.sql.Light;
import rabbit.sql.datasource.DataSourceUtil;
import rabbit.sql.page.IPageable;
import rabbit.sql.support.ICondition;
import rabbit.sql.support.JdbcSupport;
import rabbit.sql.types.Ignore;
import rabbit.sql.types.Param;
import rabbit.sql.utils.SqlUtil;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Stream;

import static rabbit.sql.utils.SqlUtil.dynamicSql;

/**
 * <p>如果配置了{@link SQLFileManager },则接口所有方法都可以通过 <b>&amp;文件夹名.文件名.sql</b> 名来获取sql文件内的sql,通过<b>&amp;</b>
 * 前缀符号来判断如果是sql名则获取sql否则当作sql直接执行</p>
 * 指定sql名执行：
 * <blockquote>
 * <pre>try ({@link Stream}&lt;{@link DataRow}&gt; s = light.query("&amp;data.query")) {
 *     s.map({@link DataRow}::toMap).forEach(System.out::println);
 *   }</pre>
 * </blockquote>
 *
 * @see rabbit.sql.support.JdbcSupport
 * @see rabbit.sql.Light
 */
public class LightDao extends JdbcSupport implements Light {
    private final static Logger log = LoggerFactory.getLogger(LightDao.class);
    private final DataSource dataSource;
    private SQLFileManager sqlFileManager;
    private DatabaseMetaData metaData;

    /**
     * 构造函数
     *
     * @param dataSource 数据源
     */
    public LightDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 实例化一个LightDao对象
     *
     * @param dataSource 数据源
     * @return LightDao实例
     */
    public static LightDao of(DataSource dataSource) {
        return new LightDao(dataSource);
    }

    /**
     * 指定sql文件解析管理器
     *
     * @param sqlFileManager sql文件解析管理器
     */
    public void setSqlFileManager(SQLFileManager sqlFileManager) {
        this.sqlFileManager = sqlFileManager;
        try {
            sqlFileManager.init();
        } catch (IOException e) {
            log.error("sql file is not exists:{}", e.getMessage());
        } catch (URISyntaxException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public long execute(String sql) {
        return executeNonQuery(sql, Collections.emptyList());
    }

    @Override
    public long execute(String sql, Map<String, Object> args) {
        return executeNonQuery(sql, Collections.singletonList(args));
    }

    @Override
    public int insert(String tableName, Map<String, Object> data) {
        return insert(tableName, data, null);
    }

    @Override
    public int insert(String tableName, Map<String, Object> data, Ignore ignore) {
        return executeNonQuery(SqlUtil.generateInsert(tableName, data, ignore), Collections.singletonList(data));
    }

    @Override
    public int insert(String tableName, DataRow row) {
        return insert(tableName, row, null);
    }

    @Override
    public int insert(String tableName, DataRow row, Ignore ignore) {
        return executeNonQueryOfDataRow(SqlUtil.generateInsert(tableName, row.toMap(), ignore), Collections.singletonList(row));
    }

    @Override
    public int insert(String tableName, Collection<Map<String, Object>> data) {
        if (data != null && data.size() > 0) {
            Map<String, Object> first = data.stream().findFirst().get();
            return executeNonQuery(SqlUtil.generateInsert(tableName, first, null), data);
        }
        return -1;
    }

    @Override
    public int delete(String tableName, ICondition ICondition) {
        return executeNonQuery("delete from " + tableName + " " + ICondition.getSql(), Collections.singletonList(ICondition.getArgs()));
    }

    @Override
    public int update(String tableName, Map<String, Object> data, ICondition ICondition) {
        data.putAll(ICondition.getArgs());
        return executeNonQuery(SqlUtil.generateUpdate(tableName, data) + ICondition.getSql(), Collections.singletonList(data));
    }

    @Override
    public Stream<DataRow> query(String sql) {
        return query(sql, Args.create());
    }

    @Override
    public Stream<DataRow> query(String sql, Map<String, Object> args) {
        try {
            return executeQueryStream(sql, args);
        } catch (SQLException ex) {
            log.error(ex.toString());
        }
        return Stream.empty();
    }

    @Override
    public <T> IPageable<T> query(String recordQuery, int page, int size) {
        return new Pageable<>(this, recordQuery, page, size);
    }

    @Override
    public Optional<DataRow> fetch(String sql) {
        return fetch(sql, Args.create());
    }

    @Override
    public Optional<DataRow> fetch(String sql, Map<String, Object> args) {
        try (Stream<DataRow> s = query(sql, args)) {
            return s.findFirst();
        }
    }

    @Override
    public boolean exists(String sql) {
        return fetch(sql).isPresent();
    }

    @Override
    public boolean exists(String sql, Map<String, Object> args) {
        return fetch(sql, args).isPresent();
    }

    /**
     * 执行一个存储过程<br>
     * PostgreSQL执行获取一个游标类型的结果：
     * <blockquote>
     * <pre>
     *  {@link List}&lt;{@link DataRow}&gt; rows = {@link rabbit.sql.transaction.Tx}.using(() -&gt;
     *    light.function("call test.func2(:c::refcursor)",
     *       Args.create("c",Param.IN_OUT("result", OUTParamType.REF_CURSOR))
     *       ).get(0));
     *   </pre>
     * </blockquote>
     *
     * @param name 过程名
     * @param args 参数 （占位符名字，参数对象）
     * @return 包含至少一个结果的DataRow结果集
     */
    @Override
    public DataRow procedure(String name, Map<String, Param> args) {
        return executeCall(name, args);
    }

    /**
     * 执行一个函数<br>
     * 同{@code procedure(String name, Map<String, Param> args)}方法
     *
     * @param name 函数名
     * @param args 参数（占位符名字，参数对象）
     * @return 包含至少一个结果的DataRow结果集
     */
    @Override
    public DataRow function(String name, Map<String, Param> args) {
        return procedure(name, args);
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        if (metaData == null) {
            metaData = getConnection().getMetaData();
        }
        return metaData;
    }

    /**
     * 如果使用取地址符"&amp;sql文件名.sql名"则获取sql文件中已缓存的sql
     *
     * @param sql sql或sql名
     * @return sql
     */
    @Override
    protected String prepareSql(String sql, Map<String, Object> args) {
        String trimEndedSql = SqlUtil.trimEnd(sql);
        if (sql.startsWith("&")) {
            if (sqlFileManager != null) {
                try {
                    trimEndedSql = SqlUtil.trimEnd(sqlFileManager.get(sql.substring(1)));
                } catch (IOException | URISyntaxException e) {
                    log.error("get SQL failed:{}", e.getMessage());
                }
            } else {
                throw new NullPointerException("can not find property 'sqlPath' or SQLFileManager init failed!");
            }
        }
        return dynamicSql(trimEndedSql, args);
    }

    @Override
    protected DataSource getDataSource() {
        return dataSource;
    }

    @Override
    protected Connection getConnection() {
        try {
            return DataSourceUtil.getConnection(dataSource);
        } catch (SQLException e) {
            log.error("fetch connection failed:{}", e.getMessage());
            return null;
        }
    }

    /**
     * 释放连接对象，如果有事务存在，并不会执行真正的释放
     *
     * @param connection 连接对象
     * @param dataSource 数据源
     */
    @Override
    protected void releaseConnection(Connection connection, DataSource dataSource) {
        DataSourceUtil.releaseConnectionIfNecessary(connection, dataSource);
    }
}