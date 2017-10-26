package util;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * DBHelper类，用于连接和关闭数据库
 */
public class DBHelper {
    public static final String url = "jdbc:mysql://localhost:3306/reptile";
    public static final String name = "com.mysql.jdbc.Driver";
    public static final String user = "root";
    public static final String password = "0619";

    public Connection conn = null;
    public PreparedStatement pst = null;
    private static DBHelper mDbHelper;

    public DBHelper() {
        try {
            Class.forName(name);//指定连接类型
            conn = (Connection) DriverManager.getConnection(url, user, password);//获取连接
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static DBHelper getInstance(){
        if (mDbHelper == null){
            mDbHelper = new DBHelper();
        }
        return mDbHelper;
    }

    public PreparedStatement useSql(String sql){
        try {
            return (PreparedStatement) conn.prepareStatement(sql);//准备执行语句
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void close() {
        try {
            this.conn.close();
            this.pst.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
