import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.net.ssl.SSLException;

public class Client extends Application {


    public static void main(String[] args){
        launch(args);
    }

     @Override
    public void start(Stage primaryStage) throws Exception {
         SeverListener severListener = new SeverListener();
         new Thread(severListener).start();

         FXMLLoader loader = new FXMLLoader(getClass().getResource("Authorization.fxml"));
         Parent root = loader.load();
         ControllerAuthorization controllerAuthorization = loader.getController();
        controllerAuthorization.setSeverListener(severListener);


        Scene scene = new Scene(root);
        primaryStage.setResizable(false);
        primaryStage.setTitle("Log in");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
