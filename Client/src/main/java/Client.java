import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class Client extends Application {

    public static void main(String[] args){
        launch(args);
    }

     @Override
     public void start(Stage primaryStage) throws Exception {
         SeverListener severListener = SeverListener.getInstance();
         Thread t = new Thread(severListener);
         t.start();

         while (!severListener.isConnect()) {
             if ( !t.isAlive() ) break;
         }

         if ( !severListener.isConnect() ) {
             new Alert(Alert.AlertType.ERROR, "Error connect to server").show();
             return;
         }

         FXMLLoader loader = new FXMLLoader(getClass().getResource("Authorization.fxml"));
         Parent root = loader.load();

         Scene scene = new Scene(root);
         primaryStage.setResizable(false);
         primaryStage.setTitle("Log in");
         primaryStage.setScene(scene);
         primaryStage.show();
         primaryStage.setOnCloseRequest(event -> {
             severListener.stop();
             Platform.exit();
         });
     }
}
