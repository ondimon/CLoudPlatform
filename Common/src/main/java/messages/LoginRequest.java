package messages;

public class LoginRequest extends Message {
    private String login;
    private String password;
    private boolean loginSuccess;

    public boolean isLoginSuccess() {
        return loginSuccess;
    }

    public void setLoginSuccess(boolean loginSuccess) {
        this.loginSuccess = loginSuccess;
    }

    public LoginRequest(String login, String password) {
        this.login = login;
        this.password = password;
        this.loginSuccess = false;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}
