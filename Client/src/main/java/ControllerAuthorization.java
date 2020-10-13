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

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.Callable;

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
            System.out.println("call back");
            System.out.println(message);
            System.out.println(message  instanceof MessageLogin);
            if(message  instanceof MessageLogin) {
                MessageLogin messageLogin = (MessageLogin) message;
                if (messageLogin.isLoginSuccess()) {
                    severListener.setToken(messageLogin.getToken());
                    loadFileScreen(messageLogin);
                }else{
                    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Invalid login or password").show());

                }
            }
        });

        Platform.runLater(() -> loginField.requestFocus());
    }

    public void logIn(javafx.event.ActionEvent actionEvent) {
        String login = loginField.getText();
        String pass = passwordField.getText();
        MessageLogin messageLogin = new MessageLogin(login, pass);
        severListener.sendMessage(messageLogin);
    }

    public void loadFileScreen(MessageLogin messageLogin) {
//        final UUID uuid = messageLogin.getToken();
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

    public void setSeverListener(SeverListener severListener) {

    }
}
