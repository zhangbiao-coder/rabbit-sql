package tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import func.BeanUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nutz.dao.entity.Record;
import org.nutz.json.Json;
import org.rabbit.common.tuple.Pair;
import org.rabbit.common.types.DataRow;
import org.rabbit.common.utils.ResourceUtil;
import org.rabbit.common.utils.StringUtil;
import org.rabbit.sql.dao.Condition;
import org.rabbit.sql.dao.Filter;
import org.rabbit.sql.support.IFilter;
import org.rabbit.sql.support.SQLFileManager;
import org.rabbit.sql.page.AbstractPageHelper;
import org.rabbit.sql.page.impl.OraclePageHelper;
import org.rabbit.sql.page.Pageable;
import org.rabbit.sql.types.Order;
import org.rabbit.sql.types.Param;
import org.rabbit.sql.dao.Params;
import org.rabbit.sql.types.ValueWrap;
import org.rabbit.sql.utils.SqlUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.sql.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tests {

    static class JsonIncludeFilter implements IFilter {

        @Override
        public String getField() {
            return "json";
        }

        @Override
        public String getOperator() {
            return " @> ";
        }

        @Override
        public Object getValue() {
            return "'{aaa}'";
        }
    }

    @BeforeClass
    public static void init() {

    }

    @Test
    public void testMatcher() throws Exception {
        String str = "select t.id || 'number' || 'name:cyx', '{\"name\":\"user\"}' from test.user";
        String regex = "'[^']*'";
        Matcher matcher = Pattern.compile(regex, Pattern.MULTILINE).matcher(str);
        while (matcher.find()) {
            str = str.replace(matcher.group(), "${*}");
        }
        System.out.println(str);
    }

    @Test
    public void sqlReplace() throws Exception {
        String str = "select t.id || 'number' || 'age:age,name:cyx', '{\"name\":\"user\"}'::jsonb " +
                "from test.user " +
                "where id = :id::integer and id > :idc and name=text :username";

//        String sql = "insert into test.user(idd,name,id,age,address) values (:id,:name::integer,:idd" + SEP + "::float,integer :age,date :address)";
//        String sql2 = "select * from test.user where id = '1' and tag = '1' and num = '1' and name = :name";
//        String jsonSql = "select '{\"a\":[1,2,3],\"b\":[4,5,6]}'::json #>> '{b,1}'";
        Pair<String, List<String>> pair = SqlUtil.getPreparedSqlAndIndexedArgNames(str);
        System.out.println(pair.getItem1());
        System.out.println(pair.getItem2());
    }

    @Test
    public void CndTest() throws Exception {
        Condition condition = Condition.New().where(Filter.eq("id", 5))
                .and(Filter.gt("age", ValueWrap.wrapEnd(26, "::text")))
                .and(Filter.eq("name", "chengyuxing"), Filter.isNotNull("address"))
                .or(Filter.endsWith("name", "jack"))
                .and(Filter.gt("id", ValueWrap.wrapStart("interval", "7 minutes")))
                .and(new JsonIncludeFilter())
                .orderBy("id")
                .orderBy("px", Order.DESC);

        Map<String, Param> params = Params.builder()
                .putIn("name", "cyx")
                .putIn("age", ValueWrap.wrapEnd("21", "::integer"))
                .putIn("time", ValueWrap.wrapStart("timestamp", "1993-5-10"))
                .build();

        String insert = SqlUtil.generateInsert("test.user", params);
        String update = SqlUtil.generateUpdate("test.user", params);

        System.out.println(condition.getString());
        System.out.println(condition.getParams());

        System.out.println(insert);
        System.out.println(update);
    }

    @Test
    public void replaceTest() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("name", ValueWrap.wrapEnd("cyx", "::text"));
        args.put("age", ValueWrap.wrapStart("integer", 26));
        args.put("address", "kunming");

        AtomicReference<String> sql = new AtomicReference<>("select * from test.user where id = :id and name = :name or age = :age");

        args.keySet().forEach(k -> {
            if (args.get(k) instanceof ValueWrap) {
                ValueWrap v = (ValueWrap) args.get(k);
                sql.set(sql.get().replace(":" + k, v.getStart() + " :" + k + v.getEnd()));
            }
        });
        System.out.println(sql.get());
    }

    @Test
    public void recordTest() throws Exception {
        Record record = new Record();
        record.put("name", "cyx");
        System.out.println(record.getString("age"));
    }

    @Test
    public void testFile() throws IOException, URISyntaxException {
        Path p = ResourceUtil.getClassPathResources("pgsql/data.sql", ".sql")
                .findFirst().get();
        System.out.println(p);
    }

    @Test
    public void javaType() throws Exception {
        byte[] bytes = new byte[1];
        System.out.println(Date.class.getName());

        String[] names = new String[]{"name", "age", "boy"};
        Object[] values = new Object[]{"chengyuxing", 26, true};
        DataRow row = DataRow.of(names, values);
        System.out.println(row);
    }

    @Test
    public void placeholder() {
        String sql = "select *\n" +
                "from test.\"user\" --用户表\n" +
                "/*啊/*dddd*/啊啊啊*/\n" +
                "where id = :id\n" +
                "${cnd} \n" +
                "/*插入语句。*/\n";

        Pattern pattern = Pattern.compile("/\\*(?<name>.+)\\*/");
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            System.out.println(matcher.group("name"));
        }
        System.out.println(sql.replaceAll(pattern.pattern(), ""));
    }

    @Test
    public void SqlFileTest() throws IOException, URISyntaxException {
        SQLFileManager manager = new SQLFileManager("pgsql");
        manager.init();
        manager.look();
        System.out.println("----");
        System.out.println(manager.get("data.query"));
    }

    @Test
    public void LambdaTest() throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String name = BeanUtil.convert2fieldName(User::getName);
        System.out.println(name);
    }

    private static <T> void lambda(Class<T> clazz, Function<T, Object> fun) throws IllegalAccessException, InstantiationException {
        T ins = clazz.newInstance();
        System.out.println(fun.apply(ins));
    }

    @Test
    public void asd() throws Exception {
        System.out.println(StringUtil.moveSpecialChars("test.user      t"));
    }

    @Test
    public void pageTest() throws Exception {
        AbstractPageHelper page = OraclePageHelper.of(12, 10);
        page.init(100);
        Pageable<Integer> pageable = Pageable.of(page, Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
        System.out.println(pageable);
        System.out.println(Json.toJson(pageable));
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(pageable));
    }
}