package ConfigurationModels;

public class Environments {
    public Environment[] environments;
    public String defaultEnv;

    public Environments(Environment[] environments, String defaultEnv) {
        this.environments = environments;
        this.defaultEnv = defaultEnv;
    }
}
