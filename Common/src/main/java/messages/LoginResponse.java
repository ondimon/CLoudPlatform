package messages;

public class LoginResponse extends Message {
    private boolean loginSuccess;

    public boolean isLoginSuccess() {
        return loginSuccess;
    }

    public void setLoginSuccess(boolean loginSuccess) {
        this.loginSuccess = loginSuccess;
    }

    public LoginResponse() {
    }

}
