package SqlSession;

import ConfigurationModels.Configuration;

import java.sql.Connection;

public interface ISqlSessionFactory {

    abstract SqlSession openSession();

    public SqlSession openSession(Connection conn);

    Configuration getConfiguration();
}
