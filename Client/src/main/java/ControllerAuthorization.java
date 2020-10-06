import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

public class ControllerAuthorization implements Initializable {
    private SeverListener severListener;

    @FXML
    public TextField loginField;

    @FXML
    public PasswordField passwordField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> loginField.requestFocus());
    }

    public void logIn(javafx.event.ActionEvent actionEvent) {
        String login = loginField.getText();
        String pass = passwordField.getText();
        MessageLogin messageLogin = new MessageLogin(login, pass);
        severListener.sendMessage(messageLogin);
    }

    public void setSeverListener(SeverListener severListener) {
        this.severListener = severListener;
    }
}
