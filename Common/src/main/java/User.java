public class User {
    String name;
    String homeDir;
    String currentDir;

    public String getCurrentDir() {
        return currentDir;
    }

    public void setCurrentDir(String currentDir) {
        this.currentDir = currentDir;
    }

    public String getName() {
        return name;
    }

    public String getHomeDir() {
        return homeDir;
    }

    public User(String name) {
        this.name = name;
        homeDir = name;
        setCurrentDir(homeDir);
    }
}
