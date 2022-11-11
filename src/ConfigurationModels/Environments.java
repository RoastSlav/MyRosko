package ConfigurationModels;

public class Environments {
    public Environment[] environments;
    public Environment defaultEnv;

    public Environments(Environment[] environments, Environment defaultEnv) {
        this.environments = environments;
        this.defaultEnv = defaultEnv;
    }
}
