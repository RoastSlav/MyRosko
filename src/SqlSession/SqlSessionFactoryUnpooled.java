package SqlSession;

import ConfigurationModels.Configuration;
import ConfigurationModels.Environment;
import Exceptions.MyBatisException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqlSessionFactoryUnpooled extends SqlSessionFactory {
    private static String URL;
    private static String USERNAME;
    private static String PASSWORD;
    private final Configuration config;

    protected SqlSessionFactoryUnpooled(Configuration configuration, Environment env) {
        config = configuration;
        String driver = env.dataSource.properties.getProperty("db_driver");
        URL = env.dataSource.properties.getProperty("db_url");
        USERNAME = env.dataSource.properties.getProperty("db_user");
        PASSWORD = env.dataSource.properties.getProperty("db_password");
        System.setProperty("jdbc.DRIVER", driver);
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    private SqlSession createSession() throws SQLException {
        Connection connection = getConnection();
        return new SqlSession(connection, config);
    }

    public SqlSession openSession() {
        try {
            return createSession();
        } catch (SQLException e) {
            throw new MyBatisException(e);
        }
    }

    public SqlSession openSession(Connection conn) {
        return new SqlSession(conn, config);
    }

    public Configuration getConfiguration() {
        return config;
    }
}
