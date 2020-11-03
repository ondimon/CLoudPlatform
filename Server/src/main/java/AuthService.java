public interface AuthService {
    boolean start();
    void stop();
    boolean checkUser(String login, String password);
}
