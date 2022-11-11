package SqlSession;

import ConfigurationModels.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqlSessionFactoryUnpooled implements ISqlSessionFactory {
    private final Configuration config;
    private static String URL;
    private static String USERNAME;
    private static String PASSWORD;

    protected SqlSessionFactoryUnpooled(Configuration configuration) {
        config = configuration;
        String driver = config.environments.defaultEnv.dataSource.properties.getProperty("db_driver");
        URL = config.environments.defaultEnv.dataSource.properties.getProperty("db_url");
        USERNAME = config.environments.defaultEnv.dataSource.properties.getProperty("db_user");
        PASSWORD = config.environments.defaultEnv.dataSource.properties.getProperty("db_password");
        System.setProperty("jdbc.DRIVER", driver);
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    private SqlSession createSession() throws SQLException {
        Connection connection = getConnection();
        return new SqlSession(connection);
    }

    public SqlSession openSession() {
        try {
            return createSession();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public SqlSession openSession(Connection conn) {
        return new SqlSession(conn);
    }

    public Configuration getConfiguration() {
        return config;
    }
}
