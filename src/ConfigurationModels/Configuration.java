package ConfigurationModels;

import java.util.Properties;

public class Configuration {
    public TypeAlias[] typeAliases;
    public Environments environments;
    public Mapper[] mappers;
    public Properties properties;

    public Configuration() {
        properties = new Properties();
    }
}
