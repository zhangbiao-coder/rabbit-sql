package org.rabbit.sql.page;

/**
 * 抽象分页工具
 */
public abstract class AbstractPageHelper {
    private final int pageNumber;
    private final int pageSize;
    private int pageCount;
    private int recordCount;

    public AbstractPageHelper(int pageNumber, int pageSize) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    /**
     * 包裹分页的sql
     *
     * @param sql sql
     * @return 分页的sql
     */
    public abstract String wrapPagedSql(String sql);

    /**
     * 获取总页数
     *
     * @return 总页数
     */
    public int getPageCount() {
        return pageCount;
    }

    /**
     * 获取当前页码
     *
     * @return 当前页码
     */
    public int getPageNumber() {
        return pageNumber;
    }

    /**
     * 获取每页记录条数
     *
     * @return 每页记录条数
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * 获取数据总条数
     *
     * @return 总条数
     */
    public int getRecordCount() {
        return recordCount;
    }

    /**
     * 初始化
     *
     * @param count 记录条数
     */
    public void init(int count) {
        this.recordCount = count;
        pageCount = recordCount / pageSize;
        if (recordCount % pageSize != 0) {
            pageCount += 1;
        }
    }

    @Override
    public String toString() {
        return "AbstractPageHelper{" +
                "pageNumber=" + pageNumber +
                ", pageSize=" + pageSize +
                ", pageCount=" + pageCount +
                ", recordCount=" + recordCount +
                '}';
    }
}
