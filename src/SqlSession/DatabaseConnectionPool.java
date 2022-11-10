package SqlSession;

import ConfigurationModels.Configuration;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

class DatabaseConnectionPool {
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

    private static final int MAX_CONNECTIONS = 10;
    private static final int MIN_CONNECTIONS = 3;
    private static final int TEN_MINUTES_IN_MILLIS = 600000;
    private static final Timer activityTimer = new Timer();
    private static final ConnectionWrapper[] connections = new ConnectionWrapper[MAX_CONNECTIONS];
    private static int connectionsCount = 0;
    private static DatabaseConnectionPool pool;
    private static String URL;
    private static String USERNAME;
    private static String PASSWORD;
    private static Configuration config;

    private static void initialize() {
        System.setProperty("jdbc.DRIVER", config.properties.getProperty("db_driver"));
        setActivityTimer();
        URL = config.properties.getProperty("db_url");
        USERNAME = config.properties.getProperty("db_user");
        PASSWORD = config.properties.getProperty("db_password");
        for (int i = 0; i < MIN_CONNECTIONS; i++) {
            try {
                connections[i] = new ConnectionWrapper(URL, USERNAME, PASSWORD);
                connectionsCount++;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private DatabaseConnectionPool(Configuration config) {
        pool = new DatabaseConnectionPool();
        DatabaseConnectionPool.config = config;
        initialize();
    }

    private DatabaseConnectionPool() {
    }

    public static DatabaseConnectionPool getConnectionPool() {
        if (pool == null || config == null)
            throw new IllegalStateException("The pool was not configured");
        return pool;
    }

    public static void setConfig(Configuration config) {
        pool = new DatabaseConnectionPool(config);
    }

    public Connection getConnection() throws SQLException {
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
            throw new RuntimeException("There are no available connections");

        result.available = false;
        var handler = new ConnectionHandler(result.connection);
        Object proxy = Proxy.newProxyInstance(
                ClassLoader.getSystemClassLoader(),
                new Class[] { Connection.class } , handler);
        return (Connection) proxy;
    }

    public static void releaseConnection(Connection conn) {
        for (ConnectionWrapper connection : connections) {
            if (connection.connection.equals(conn)) {
                connection.available = true;
                connection.lastUsed = System.currentTimeMillis();
                break;
            }
        }
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
                            System.out.println("Could not close connection");
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
}
