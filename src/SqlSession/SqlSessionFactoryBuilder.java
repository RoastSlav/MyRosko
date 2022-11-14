package SqlSession;

import ConfigurationModels.Configuration;
import ConfigurationModels.Environment;
import Exceptions.MyBatisException;
import Parsers.ConfigurationParser;

import java.io.Reader;
import java.util.NoSuchElementException;
import java.util.Properties;

public class SqlSessionFactoryBuilder {
    public SqlSessionFactory build(Reader reader) {
        return this.build(reader, null, null);
    }

    public SqlSessionFactory build(Reader reader, Properties props) {
        return this.build(reader, props, null);
    }

    public SqlSessionFactory build(Reader reader, String environment) {
        return this.build(reader, null, environment);
    }

    public SqlSessionFactory build(Reader reader, Properties props, String environment) {
        Configuration configuration;
        try {
            ConfigurationParser configurationParser = new ConfigurationParser(reader, props);
            configuration = configurationParser.parse();
        } catch (Exception e) {
            throw new MyBatisException(e);
        }

        Environment env = null;
        if (environment == null) {
            env = configuration.environments.defaultEnv;
        } else {
            Environment[] environments = configuration.environments.environments;
            for (int i = 0; i < environments.length; i++) {
                if (environments[i].id.equals(environment))
                    env = environments[i];
            }
        }

        if (env == null)
            throw new NoSuchElementException("There isn't an environment with this id: " + environment);

        if (env.dataSource.type.equals("POOLED"))
            return new SqlSessionFactoryPooled(configuration);
        else
            return new SqlSessionFactoryUnpooled(configuration);
    }
}
