package rabbit.sql.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.common.utils.ResourceUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SQL文件解析管理器
 */
public final class SQLFileManager {
    private final static Logger log = LoggerFactory.getLogger(LightDao.class);
    private final ReentrantLock lock = new ReentrantLock();
    static final Map<String, String> RESOURCE = new HashMap<>();
    static final Map<String, Long> LAST_MODIFIED = new HashMap<>();

    private String basePath;
    private boolean checkModified;

    private static final String NAME_OPEN = "/*[";
    private static final String NAME_CLOSE = "]*/";
    private static final String PART_OPEN = "/*{";
    private static final String PART_CLOSE = "}*/";
    private static final String ANNOTATION_START = "/*";
    private static final String ANNOTATION_END = "*/";
    private static final String SEPARATOR = ";";

    SQLFileManager() {
    }

    /**
     * 构造函数
     *
     * @param basePath sql基本目录
     */
    public SQLFileManager(String basePath) {
        this.basePath = basePath;
    }

    /**
     * 解析sql文件
     *
     * @param path 路径
     * @throws IOException IOexp
     */
    private void resolveSqlContent(Path path) throws IOException {
        Map<String, String> singleResource = new HashMap<>();
        String fileName = path.getFileName().toString();
        String prefix = fileName.substring(0, fileName.length() - 3);
        String previousSqlName = "";
        boolean isAnnotation = false;
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimLine = line.trim();
                if (!trimLine.isEmpty()) {
                    if (trimLine.startsWith(NAME_OPEN) && trimLine.endsWith(NAME_CLOSE)) {
                        previousSqlName = prefix + trimLine.substring(3, trimLine.length() - 3);
                        singleResource.put(previousSqlName, "");
                    } else if (trimLine.startsWith(PART_OPEN) && trimLine.endsWith(PART_CLOSE)) {
                        previousSqlName = "${" + prefix + trimLine.substring(3, trimLine.length() - 3) + "}";
                        singleResource.put(previousSqlName, "");
                    } else {
                        // 排除单行注释
                        if (!trimLine.startsWith("--")) {
                            // 排除块注释
                            if (trimLine.startsWith(ANNOTATION_START)) {
                                // 没有找到块注释结束 则标记下面的代码都是注释
                                if (!trimLine.endsWith(ANNOTATION_END)) {
                                    isAnnotation = true;
                                }
                                // 直到找到块注释结束符号，标记注释结束
                            } else if (trimLine.endsWith(ANNOTATION_END)) {
                                isAnnotation = false;
                            } else if (!isAnnotation && !previousSqlName.equals("")) {
                                String prepareLine = singleResource.get(previousSqlName) + line;
                                if (trimLine.endsWith(SEPARATOR)) {
                                    singleResource.put(previousSqlName, prepareLine.substring(0, prepareLine.lastIndexOf(SEPARATOR)));
                                    log.debug("scan to get SQL [{}]：{}", previousSqlName, singleResource.get(previousSqlName));
                                } else {
                                    singleResource.put(previousSqlName, prepareLine.concat("\n"));
                                }
                            }
                        }
                    }
                }
            }
        }
        mergeSqlPartIfNecessary(singleResource);
        RESOURCE.putAll(singleResource);
    }

    /**
     * 执行合并SQL片段
     *
     * @param partName sql片段名
     */
    private void doMergeSqlPart(final String partName, Map<String, String> sqlResource) {
        String pn = "${" + partName.substring(partName.indexOf(".") + 1);
        boolean has = false;
        for (String key : sqlResource.keySet()) {
            if (!key.startsWith("${")) {
                if (sqlResource.get(key).contains(pn)) {
                    sqlResource.put(key, sqlResource.get(key).replace(pn, sqlResource.get(partName)));
                }
                if (sqlResource.get(key).contains(pn)) {
                    has = true;
                }
            }
        }
        if (has) {
            doMergeSqlPart(partName, sqlResource);
        }
    }

    /**
     * 合并SQL可复用片段到包含片段名的SQL中
     */
    private void mergeSqlPartIfNecessary(Map<String, String> sqlResource) {
        sqlResource.keySet().stream()
                .filter(k -> k.startsWith("${"))
                .forEach(k -> doMergeSqlPart(k, sqlResource));
    }

    /**
     * 如果有检测到文件修改过，则重新加载已修改过的sql文件
     */
    private void reloadIfNecessary() throws IOException, URISyntaxException {
        if (!LAST_MODIFIED.isEmpty())
            ResourceUtil.getClassPathResources(basePath, ".sql")
                    .filter(p -> {
                        File f = p.toFile();
                        // 如果有文件并且文件修改过
                        if (LAST_MODIFIED.containsKey(f.getName())) {
                            long timestamp = LAST_MODIFIED.get(f.getName());
                            return timestamp != f.lastModified();
                        }
                        // 如果是新文件则不过滤
                        return true;
                    }).forEach(p -> {
                try {
                    log.debug("removing expired SQL cache...");
                    resolveSqlContent(p);
                    File f = p.toFile();
                    String name = f.getName();
                    //更新最后一次修改的时间
                    LAST_MODIFIED.put(name, f.lastModified());
                    log.debug("reload modified sql file:{}", name);
                } catch (IOException e) {
                    log.error("resolve SQL file with an error:{}", e.getMessage());
                }
            });
    }

    /**
     * 初始化加载sql到缓存中
     *
     * @throws IOException        IOExp
     * @throws URISyntaxException URIExp
     */
    public void init() throws IOException, URISyntaxException {
        ResourceUtil.getClassPathResources(basePath, ".sql")
                .forEach(p -> {
                    try {
                        resolveSqlContent(p);
                    } catch (IOException e) {
                        log.error("resolve SQL file with an error:{}", e.getMessage());
                    }
                    File f = p.toFile();
                    LAST_MODIFIED.put(f.getName(), f.lastModified());
                });
    }

    /**
     * 查看sql资源
     */
    public void look() {
        RESOURCE.forEach((k, v) -> {
            System.out.println("[" + k + "] -> " + v);
        });
    }

    /**
     * 获取一条sql
     *
     * @param name sql名
     * @return sql
     * @throws IOException        IOExp
     * @throws URISyntaxException URIExp
     */
    public String get(String name) throws IOException, URISyntaxException {
        if (checkModified) {
            lock.lock();
            reloadIfNecessary();
            lock.unlock();
        }
        if (RESOURCE.containsKey(name)) {
            return RESOURCE.get(name);
        }
        throw new NoSuchElementException(String.format("no SQL named [%s] was found.", name));
    }

    /**
     * 获取基本目录
     *
     * @return 基本目录
     */
    public String getBasePath() {
        return basePath;
    }

    /**
     * 是否在每次获取sql时都检查文件是否更新
     *
     * @param checkModified 是否检查更新
     */
    public void setCheckModified(boolean checkModified) {
        this.checkModified = checkModified;
    }
}
