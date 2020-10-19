import messages.LoginRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import messages.LoginResponse;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ControllerAuthorization implements Initializable {
    private SeverListener severListener;

    @FXML
    public TextField loginField;

    @FXML
    public PasswordField passwordField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        severListener = SeverListener.getInstance();
        severListener.setCallback(message -> {
            if(message  instanceof LoginResponse ) {
                LoginResponse messageLogin = (LoginResponse) message;
                if (messageLogin.isLoginSuccess()) {
                    severListener.setToken(messageLogin.getToken());
                    loadFileScreen(messageLogin);
                }else{
                    Platform.runLater(() -> showAlertWindow("Invalid login or password"));

                }
            }
        });

        Platform.runLater(() -> loginField.requestFocus());
    }

    public void logIn(javafx.event.ActionEvent actionEvent) {
        String login = loginField.getText();
        String pass = passwordField.getText();
        LoginRequest messageLogin = new LoginRequest(login, pass);
        severListener.sendMessage(messageLogin);
    }

    public void loadFileScreen(LoginResponse messageLogin) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    loginField.getScene().getWindow().hide();

                    Stage stage = new Stage();
                    stage.setTitle("File manager");

                    FXMLLoader loader = new FXMLLoader(getClass().getResource("FileManager.fxml"));
                    Parent window = loader.load();
                    //ControllerFileManager controllerFileManager = loader.getController();

                    Scene scene = new Scene(window);

                    stage.setResizable(true);
                    stage.setScene(scene);
                    stage.setOnCloseRequest(event -> {
                        severListener.stop();
                        Platform.exit();
                    });
                    stage.show();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void showAlertWindow(String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

}
