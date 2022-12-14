package SqlSession;

import ConfigurationModels.Configuration;
import ConfigurationModels.Environment;
import Exceptions.MyBatisException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

public class SqlSessionFactoryPooled extends SqlSessionFactory {
    private static final int MAX_CONNECTIONS = 10;
    private static final int MIN_CONNECTIONS = 3;
    private static final int TEN_MINUTES_IN_MILLIS = 600000;
    private static String URL;
    private static String USERNAME;
    private static String PASSWORD;
    private static Timer activityTimer;
    private static ConnectionWrapper[] connections;
    private static int connectionsCount;
    private final Configuration config;

    protected SqlSessionFactoryPooled(Configuration configuration, Environment env) {
        config = configuration;
        String driver = env.dataSource.properties.getProperty("driver");
        URL = env.dataSource.properties.getProperty("url");
        USERNAME = env.dataSource.properties.getProperty("username");
        PASSWORD = env.dataSource.properties.getProperty("password");
        System.setProperty("jdbc.DRIVER", driver);
        intializePool();
    }

    private static void setActivityTimer() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                int activeCons = 0;
                for (int i = 0; i < connectionsCount; i++) {
                    if (activeCons < MIN_CONNECTIONS) {
                        activeCons++;
                        continue;
                    } else if (connections[i].lastUsed + TEN_MINUTES_IN_MILLIS < System.currentTimeMillis() &&
                            connections[i].timeGiven != -1) {
                        try {
                            connections[i].connection.close();
                            connections[i] = null;
                            connectionsCount--;
                        } catch (SQLException e) {
                            throw new MyBatisException("Could not close connection " + connections[i]);
                        }
                    }

                    if (connections[i].timeGiven + TEN_MINUTES_IN_MILLIS > System.currentTimeMillis()) {
                        throw new IllegalStateException("The connection was not returned");
                    }
                }
                setActivityTimer();
            }
        };
        activityTimer.schedule(task, TEN_MINUTES_IN_MILLIS);
    }

    private static void releaseConnection(Connection conn) {
        for (ConnectionWrapper connection : connections) {
            if (connection == null)
                break;
            if (connection.connection.equals(conn)) {
                connection.available = true;
                connection.lastUsed = System.currentTimeMillis();
                break;
            }
        }
    }

    private void intializePool() {
        connections = new ConnectionWrapper[MAX_CONNECTIONS];
        activityTimer = new Timer();
        connectionsCount = 0;
        setActivityTimer();
        for (int i = 0; i < MIN_CONNECTIONS; i++) {
            try {
                connections[i] = new ConnectionWrapper(URL, USERNAME, PASSWORD);
                connectionsCount++;
            } catch (SQLException e) {
                throw new MyBatisException(e);
            }
        }
    }

    private Connection getPooledConnection() throws SQLException {
        ConnectionWrapper result = connections[0];

        for (int i = 1; i < connections.length; i++) {
            if (connections[i] != null && connections[i].available) {
                if (connections[i].lastUsed + TEN_MINUTES_IN_MILLIS < result.lastUsed + TEN_MINUTES_IN_MILLIS)
                    result = connections[i];
            }
        }

        if (!result.available) {
            for (int i = 0; i < connections.length; i++) {
                if (connections[i].connection.isClosed() || connections[i] == null) {
                    connections[i] = new ConnectionWrapper(URL, USERNAME, PASSWORD);
                }
            }
        }

        if (!result.available)
            throw new MyBatisException("There are no available connections");

        result.available = false;
        var handler = new ConnectionHandler(result.connection);
        Object proxy = Proxy.newProxyInstance(
                ClassLoader.getSystemClassLoader(),
                new Class[]{Connection.class}, handler);
        return (Connection) proxy;
    }

    private SqlSession createSession() throws SQLException {
        Connection conn = getPooledConnection();
        return new SqlSession(conn, config);
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

    static class ConnectionWrapper {
        Connection connection;
        boolean available;
        long lastUsed;
        long timeGiven;

        ConnectionWrapper(String url, String username, String password) throws SQLException {
            connection = DriverManager.getConnection(url, username, password);
            lastUsed = System.currentTimeMillis();
            timeGiven = -1;
            available = true;
        }
    }

    static class ConnectionHandler implements InvocationHandler {
        private final Connection con;

        public ConnectionHandler(Connection con) {
            this.con = con;
        }

        public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
            if (m.getName().equals("close")) {
                releaseConnection((Connection) proxy);
            }
            return m.invoke(con, args);
        }
    }
}
