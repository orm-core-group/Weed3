package org.noear.weed;

import org.noear.weed.ext.Act1;
import org.noear.weed.ext.Act1Ex;
import org.noear.weed.xml.XmlSqlLoader;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by noear on 14-6-12.
 * 数据库上下文
 */
public class DbContext {
    /**
     * 最后次执行命令
     */
    public Command lastCommand;
    /**
     * 充许多片段执行
     */
    public boolean allowMultiQueries;
    /**
     * 编译模式（用于产生代码）
     */
    public boolean isCompilationMode = false;


    //数据集名称
    private String _schemaName;
    private IDbFormater _formater = new DbFormater();
    //特性支持
    private Map<String, String> _attrMap = new HashMap<>();
    //数据源
    private DataSource _dataSource;
    //代码注解
    private String _codeHint = null;

    //
    // 构建函数 start
    //
    public DbContext() {
    }

    //基于线程池配置（如："proxool."）
    //fieldFormat："`%`"
    public DbContext(String schemaName, String url) {
        _schemaName = schemaName;
        _dataSource = new DbDataSource(url);
    }

    //基于手动配置（无线程池）
    public DbContext(String schemaName, String url, String user, String password) {
        _schemaName = schemaName;
        _dataSource = new DbDataSource(url, user, password);
    }

    public DbContext(String schemaName, DataSource dataSource) {
        _schemaName = schemaName;
        _dataSource = dataSource;
    }

    //
    // 构建函数 end
    //

    /**
     * 特性设置
     */
    public DbContext attrSet(String name, String value) {
        _attrMap.put(name, value);
        return this;
    }

    /**
     * 特性获取
     */
    public String attr(String name) {
        return _attrMap.get(name);
    }


    /**
     * 数据源设置
     */
    public DbContext dataSourceSet(DataSource dataSource) {
        _dataSource = dataSource;
        return this;
    }

    /**
     * 获取数据源
     */
    public DataSource dataSource() {
        return _dataSource;
    }


    /**
     * 数据集合名称设置
     */
    public DbContext schemaNameSet(String schemaName) {
        _schemaName = schemaName;
        return this;
    }

    /**
     * 代码注解设置
     */
    public DbContext codeHintSet(String hint) {
        _codeHint = hint;
        return this;
    }

    /**
     * 代码注解获取
     */
    public String codeHint() {
        return _codeHint;
    }


    /*是否配置了schema*/
    public boolean hasSchema() {
        return _schemaName != null;
    }

    /*获取schema*/
    public String getSchema() {
        return _schemaName;
    }

    //
    // 执行能力支持
    //

    /**
     * 获取连接
     */
    public Connection getConnection() throws SQLException {
        return _dataSource.getConnection();
    }

    /**
     * 执行代码，返回影响行数
     */
    public int exec(String code, Object... args) throws Exception {
        return new DbQuery(this).sql(new SQLBuilder().append(code, args)).execute();
    }

    /**
     * 输入SQL，获取查询器
     */
    public DbQuery sql(String code, Object... args) {
        return sql(new SQLBuilder().append(code, args));
    }

    /**
     * 输入SQL builder，获取查询器
     */
    public DbQuery sql(Act1<SQLBuilder> buildRuner) {
        SQLBuilder sql = new SQLBuilder();
        buildRuner.run(sql);
        return sql(sql);
    }

    /**
     * 输入SQL builder，获取查询器
     */
    public DbQuery sql(SQLBuilder sqlBuilder) {
        return new DbQuery(this).sql(sqlBuilder);
    }


    /**
     * 输入process name，获取process执行对象
     */
    public DbProcedure call(String process) {
        if (process.startsWith("@")) {
            XmlSqlLoader.tryLoad();
            return new DbSqlProcedure(this).sql(process.substring(1));
        }

        if (process.indexOf(" ") > 0) {
            return new DbQueryProcedure(this).sql(process);
        }

        return new DbStoredProcedure(this).call(process);
    }


    /**
     * 获取一个表对象［用于操作插入也更新］
     */
    public DbTableQuery table(String table) {
        return new DbTableQuery(this).table(table);
    }

    public DbTran tran(Act1Ex<DbTran, SQLException> handler) throws SQLException {
        return new DbTran(this);
    }

    public DbTran tran() {
        return new DbTran(this);
    }

    public DbTranQueue tranQueue(Act1Ex<DbTranQueue, SQLException> handler) throws SQLException {
        return new DbTranQueue();
    }

    //
    // 格式化处理
    //

    /**
     * 字段格式符设置
     */
    public DbContext fieldFormatSet(String format) {
        _formater.fieldFormatSet(format);
        return this;
    }

    /**
     * 对象格式符设置
     */
    public DbContext objectFormatSet(String format) {
        _formater.objectFormatSet(format);
        return this;
    }

    public DbContext formaterSet(IDbFormater formater){
        _formater = formater;
        return this;
    }
    public IDbFormater formater(){
        return _formater;
    }
}
