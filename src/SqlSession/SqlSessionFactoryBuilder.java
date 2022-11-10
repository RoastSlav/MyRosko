package SqlSession;

import ConfigurationModels.Configuration;
import Parsers.ConfigurationParser;

import java.io.Reader;
import java.util.Properties;

public class SqlSessionFactoryBuilder {
    public SqlSessionFactory build(Reader reader) {
        return this.build(reader, null);
    }

    public SqlSessionFactory build(Reader reader, Properties props) {
        Configuration configuration;
        try {
            ConfigurationParser configurationParser = new ConfigurationParser(reader, props);
            configuration = configurationParser.parse();
        } catch (Exception e) {
            throw new RuntimeException();
        }
        DatabaseConnectionPool.setConfig(configuration);

        return new SqlSessionFactory(configuration);
    }
}
