package ConfigurationModels;

import java.util.Properties;

public class DataSource {
    public String type;
    public Properties properties;

    public DataSource() {
        properties = new Properties();
    }
}
