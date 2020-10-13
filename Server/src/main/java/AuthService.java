public interface AuthService {
    void start();
    void stop();
    boolean checkUser(String login, String password);
}
