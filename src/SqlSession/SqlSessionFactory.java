package SqlSession;

import ConfigurationModels.Configuration;

import java.sql.Connection;

public abstract class SqlSessionFactory {

    public abstract SqlSession openSession();

    public abstract SqlSession openSession(Connection conn);

    public abstract Configuration getConfiguration();
}
