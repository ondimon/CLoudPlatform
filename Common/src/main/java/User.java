import java.io.File;

public class User {
    String name;
    String homeDir;

    public String getName() {
        return name;
    }

    public String getHomeDir() {
        return homeDir;
    }

    public User(String name) {
        this.name = name;
        homeDir = name;
    }
}
