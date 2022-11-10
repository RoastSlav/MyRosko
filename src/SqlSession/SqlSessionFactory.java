package SqlSession;

import ConfigurationModels.Configuration;

import java.sql.Connection;
import java.sql.SQLException;

public class SqlSessionFactory {
    private final DatabaseConnectionPool pool;
    private final Configuration config;

    protected SqlSessionFactory(Configuration configuration) {
        pool = DatabaseConnectionPool.getConnectionPool();
        config = configuration;
    }


    private SqlSession createSession() throws SQLException {
        Connection connection = pool.getConnection();
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

    Configuration getConfiguration() {
        return config;
    }
}
