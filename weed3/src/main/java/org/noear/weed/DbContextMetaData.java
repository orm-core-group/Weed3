package org.noear.weed;

import org.noear.weed.wrap.DbType;
import org.noear.weed.wrap.ColumnWrap;
import org.noear.weed.wrap.TableWrap;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

class DbContextMetaData {
    protected String _schema;
    protected String _catalog;
    protected Map<String, TableWrap> _tables = new HashMap<>();
    protected DbType _dbType = DbType.Unknown;

    public DbType dbType(){
        return _dbType;
    }

    public TableWrap getTableWrap(String name){
        return _tables.get(name);
    }
    public String getTablePk1(String name) {
        TableWrap tw = _tables.get(name);
        return tw == null ? null : tw.getPk1();
    }

    protected void initMetaData(DbContext db) {
        Connection conn = null;
        try {
            conn = db.getConnection();
            DatabaseMetaData md = conn.getMetaData();

            //1.
            setDatabaseType(db, md.getDatabaseProductName());

            //2.
            setSchema(db, conn);

            //3.
            setTables(md);

        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void setDatabaseType(DbContext db, String driverName) {
        if (driverName != null) {
            String pn = driverName.toLowerCase().replace(" ", "");

            if (pn.indexOf("mysql") >= 0) {
                db._dbType = DbType.MySQL;
            } else if (pn.indexOf("mariadb") >= 0) {
                db._dbType = DbType.MariaDB;
            } else if (pn.indexOf("sqlserver") >= 0) {
                db._dbType = DbType.SQLServer;
            } else if (pn.indexOf("oracle") >= 0) {
                db._dbType = DbType.Oracle;
            } else if (pn.indexOf("postgresql") >= 0) {
                db._dbType = DbType.PostgreSQL;
            } else if (pn.indexOf("db2") >= 0) {
                db._dbType = DbType.DB2;
            } else if (pn.indexOf("sqlite") >= 0) {
                db._dbType = DbType.SQLite;
            }else if (pn.indexOf("h2") >= 0) {
                db._dbType = DbType.H2;
            }

            if (db._dbType == DbType.MySQL ||
                    db._dbType == DbType.MariaDB ||
                        db._dbType == DbType.SQLite) {
                db.formater().fieldFormatSet("`%`");
                db.formater().objectFormatSet("`%`");
            } else if(db._dbType == DbType.SQLServer){
                db.formater().fieldFormatSet("[%]");
                db.formater().objectFormatSet("[%]");
            }  else {
                //SQLServer, PostgreSQL, DB2
                db.formater().fieldFormatSet("\"%\"");
                db.formater().objectFormatSet("\"%\"");
            }
        }
    }

    private void setSchema(DbContext db, Connection conn) throws SQLException {
        try {
            db._catalog = conn.getCatalog();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        if (db._schema != null) {
            return;
        }

        try {
            db._schema = conn.getSchema();
        } catch (Throwable e) {
            switch (db._dbType) {
                case PostgreSQL:
                    db._schema = "public";
                    break;
                case SQLServer:
                    db._schema = "dbo";
                case Oracle:
                    db._schema = conn.getMetaData().getUserName();
                    break;
            }
        }
    }

    private void setTables(DatabaseMetaData md) throws SQLException {
        ResultSet rs = null;
        rs = md.getTables(_catalog, _schema, null, new String[]{"TABLE", "VIEW"});
        while (rs.next()) {
            String name = rs.getString("TABLE_NAME");
            String remarks = rs.getString("REMARKS");
            TableWrap tWrap = new TableWrap(name, remarks);
            _tables.put(name, tWrap);
        }
        rs.close();

        for (String key : _tables.keySet()) {
            TableWrap tWrap = _tables.get(key);
            rs = md.getColumns(_catalog, _schema, key, "%");

            while (rs.next()) {
                ColumnWrap cw = new ColumnWrap();
                cw.name = rs.getString("COLUMN_NAME");
                cw.type = rs.getInt("DATA_TYPE");
                cw.size = rs.getInt("COLUMN_SIZE");
                cw.remarks = rs.getString("REMARKS");
                tWrap.addColumn(cw);
            }
            rs.close();

            rs = md.getPrimaryKeys(_catalog,_schema,key);
            while (rs.next()) {
                String idName=rs.getString("COLUMN_NAME");
                tWrap.addPk(idName);
            }
            rs.close();
        }
    }
}
