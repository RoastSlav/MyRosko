package SqlSession;

import ConfigurationModels.Configuration;
import Parsers.ConfigurationParser;

import java.io.Reader;
import java.util.Properties;

public class SqlSessionFactoryBuilder {
    public ISqlSessionFactory build(Reader reader) {
        return this.build(reader, null);
    }

    public ISqlSessionFactory build(Reader reader, Properties props) {
        Configuration configuration;
        try {
            ConfigurationParser configurationParser = new ConfigurationParser(reader, props);
            configuration = configurationParser.parse();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (configuration.environments.defaultEnv.dataSource.type.equals("POOLED"))
            return new SqlSessionFactoryPooled(configuration);
        else
            return new SqlSessionFactoryUnpooled(configuration);
    }
}
