package rabbit.sql;

import rabbit.common.types.DataRow;
import rabbit.sql.page.AbstractPageHelper;
import rabbit.sql.page.Pageable;
import rabbit.sql.support.ICondition;
import rabbit.sql.types.Param;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * dao接口
 */
public interface Light {

    /**
     * 执行一条原始sql
     *
     * @param sql 原始sql
     * @return 如果执行成功，DML语句返回1，DDL语句返回0
     */
    long execute(String sql);

    /**
     * 执行一条原始sql
     *
     * @param sql 原始sql
     * @return 如果执行成功，DML语句返回1，DDL语句返回0
     */
    long execute(String sql, Map<String, Param> params);

    /**
     * 插入
     *
     * @param tableName 表名
     * @param data      数据
     * @return 受影响的行数
     */
    int insert(String tableName, Map<String, Param> data);

    /**
     * 插入
     *
     * @param tableName 表名
     * @param row       数据
     * @return 受影响的行数
     */
    int insert(String tableName, DataRow row);

    /**
     * 批量插入
     *
     * @param tableName 表名
     * @param data      数据
     * @return 受影响的行数
     */
    int insert(String tableName, Collection<Map<String, Param>> data);

    /**
     * 删除
     *
     * @param tableName 表名
     * @param ICondition 条件
     * @return 受影响的行数
     */
    int delete(String tableName, ICondition ICondition);

    /**
     * 更新
     *
     * @param tableName 表名
     * @param data      数据
     * @param ICondition 条件
     * @return 受影响的行数
     */
    int update(String tableName, Map<String, Param> data, ICondition ICondition);

    /**
     * 查询<br>
     * e.g. select * from table
     *
     * @param sql     查询sql
     * @param convert 转换
     * @param <T>     目标类型
     * @return 收集为流的结果集
     */
    <T> Stream<T> query(String sql, Function<DataRow, T> convert);

    /**
     * 查询<br>
     * e.g. select * from table
     *
     * @param sql       查询sql
     * @param convert   转换
     * @param fetchSize 条数
     * @param <T>       目标类型
     * @return 收集为流的结果集
     */
    <T> Stream<T> query(String sql, Function<DataRow, T> convert, long fetchSize);

    /**
     * 查询<br>
     * e.g. select * from table where id = :id
     *
     * @param sql     查询sql
     * @param convert 转换
     * @param args    参数
     * @param <T>     目标类型
     * @return 收集为流的结果集
     */
    <T> Stream<T> query(String sql, Function<DataRow, T> convert, Map<String, Param> args);

    /**
     * 查询<br>
     * e.g. select * from table where id = :id
     *
     * @param sql       查询sql
     * @param convert   转换
     * @param args      参数
     * @param fetchSize 条数
     * @param <T>       目标类型
     * @return 收集为流的结果集
     */
    <T> Stream<T> query(String sql, Function<DataRow, T> convert, Map<String, Param> args, long fetchSize);

    /**
     * 查询<br>
     * e.g. select * from table
     *
     * @param sql       查询sql
     * @param convert   转换
     * @param ICondition 参数
     * @param <T>       目标类型
     * @return 收集为流的结果集
     */
    <T> Stream<T> query(String sql, Function<DataRow, T> convert, ICondition ICondition);

    /**
     * 查询<br>
     * e.g. select * from table
     *
     * @param sql       查询sql
     * @param convert   转换
     * @param ICondition 参数
     * @param fetchSize 条数
     * @param <T>       目标类型
     * @return 收集为流的结果集
     */
    <T> Stream<T> query(String sql, Function<DataRow, T> convert, ICondition ICondition, long fetchSize);

    /**
     * 分页查询
     *
     * @param recordQuery 查询SQL
     * @param countQuery  查询记录数SQL
     * @param convert     行转换
     * @param args        参数
     * @param page        分页对象
     * @param <T>         目标类型
     * @return 分页的结果集
     */
    <T> Pageable<T> query(String recordQuery, String countQuery, Function<DataRow, T> convert, Map<String, Param> args, AbstractPageHelper page);

    /**
     * 分页查询
     *
     * @param recordQuery 查询SQL
     * @param countQuery  查询记录数SQL
     * @param convert     行转换
     * @param ICondition   条件拼接器
     * @param page        分页对象
     * @param <T>         目标类型
     * @return 分页的结果集
     */
    <T> Pageable<T> query(String recordQuery, String countQuery, Function<DataRow, T> convert, ICondition ICondition, AbstractPageHelper page);

    /**
     * 分页查询
     *
     * @param recordQuery 查询SQL
     * @param convert     行转换
     * @param ICondition   条件拼接器
     * @param page        分页对象
     * @param <T>         目标类型
     * @return 分页的结果集
     */
    <T> Pageable<T> query(String recordQuery, Function<DataRow, T> convert, ICondition ICondition, AbstractPageHelper page);

    /**
     * 分页查询
     *
     * @param recordQuery 查询SQL
     * @param convert     行转换
     * @param args        参数
     * @param page        分页对象
     * @param <T>         目标类型
     * @return 分页的结果集
     */
    <T> Pageable<T> query(String recordQuery, Function<DataRow, T> convert, Map<String, Param> args, AbstractPageHelper page);

    /**
     * 分页查询
     *
     * @param recordQuery 查询SQL
     * @param convert     行转换
     * @param page        分页对象
     * @param <T>         目标类型
     * @return 分页的结果集
     */
    <T> Pageable<T> query(String recordQuery, Function<DataRow, T> convert, AbstractPageHelper page);

    /**
     * 获取一条<br>
     * e.g. select * from table
     *
     * @param sql     查询sql
     * @param convert 转换
     * @param <T>     目标类型
     * @return 空或一条
     */
    <T> Optional<T> fetch(String sql, Function<DataRow, T> convert);

    /**
     * 获取一条<br>
     * e.g. select * from table
     *
     * @param sql       查询sql
     * @param convert   转换
     * @param ICondition 条件
     * @param <T>       目标类型
     * @return 空或一条
     */
    <T> Optional<T> fetch(String sql, Function<DataRow, T> convert, ICondition ICondition);

    /**
     * 获取一条<br>
     * e.g. select * from table where id = :id
     *
     * @param sql     查询sql
     * @param convert 转换
     * @param args    参数
     * @param <T>     目标类型
     * @return 空或一条
     */
    <T> Optional<T> fetch(String sql, Function<DataRow, T> convert, Map<String, Param> args);

    /**
     * 判断是否存在数据行
     *
     * @param sql sql
     * @return 是否存在
     */
    boolean exists(String sql);

    /**
     * 判断是否存在数据行
     *
     * @param sql       sql
     * @param ICondition 条件
     * @return 是否存在
     */
    boolean exists(String sql, ICondition ICondition);

    /**
     * 判断是否存在数据行
     *
     * @param sql  sql
     * @param args 参数
     * @return 是否存在
     */
    boolean exists(String sql, Map<String, Param> args);

    /**
     * 执行存储过程或函数
     * e.g. call func(:arg1,:argn...)
     *
     * @param name 过程名
     * @param args 参数 （占位符名字，参数对象）
     * @return 一个或多个结果
     */
    DataRow call(String name, Map<String, Param> args);

    /**
     * 获取数据库的元数据信息
     *
     * @return 数据库的元数据信息
     */
    DatabaseMetaData getMetaData() throws SQLException;
}