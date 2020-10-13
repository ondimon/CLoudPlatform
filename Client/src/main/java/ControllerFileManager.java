import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class ControllerFileManager implements Initializable {
    private SeverListener severListener;

    @FXML
    ListView<String> serverFiles;

    @FXML
    ListView<String> clientFiles;

    @FXML
    TextField pathDir;

    @FXML
    Button buttonUpload;

    @FXML
    Button buttonDownload;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        severListener = SeverListener.getInstance();
        severListener.setCallback(message -> {
            if(message instanceof FileListResponse) {
                updateServerFileList((FileListResponse) message);
            }
        });

        sendFileListRequest();

    }

    public void updateServerFileList(FileListResponse message) {
        Platform.runLater(() -> {
                    serverFiles.getItems().clear();
                    for(String fileName : message.getFileList()) {
                        serverFiles.getItems().add(fileName);
                    }});
    }

    public void updateClientFileList() {
        clientFiles.getItems().clear();
        String path = pathDir.getText();
        if(path.equals("")) {
            return;
        }

        ArrayList<String> listFiles = null;
        try {
            listFiles = FileUtility.getListFiles(Paths.get(path));
            for(String fileName : listFiles) {
                clientFiles.getItems().add(fileName);
            }
        } catch (IOException e) {
            showAlertWindow(e.getMessage());
        }
    }

    public void sendFileListRequest() {
        FileListRequest fileListRequest = new FileListRequest();
        severListener.sendMessage(fileListRequest);

    }

    public void directoryChoose(ActionEvent actionEvent) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directories");
        // Set Initial Directory
        //directoryChooser.setInitialDirectory();
        File dir = directoryChooser.showDialog(pathDir.getScene().getWindow());
        if (dir != null) {
            pathDir.setText(dir.getAbsolutePath());
        } else {
            pathDir.setText(null);
        }
        updateClientFileList();
    }

    public void buttonUploadOnAction(ActionEvent actionEvent) {
        String fileName = clientFiles.getSelectionModel().getSelectedItem();

            Path pathToFile = Paths.get(pathDir.getText(), fileName);
            severListener.sendFile(pathToFile);
            // FileResponse fileResponse = new FileResponse(pathToFile);
           // severListener.sendMessage(fileResponse);
//            try (FileChannel channel = new RandomAccessFile(pathToFile.toFile(), "rw").getChannel()) {
//                ByteBuffer buffer = ByteBuffer.allocate(1);
//                while (channel.read(buffer) > 0) {
//                    buffer.flip();
//                    byte[] data = buffer.array();
//                    severListener.sendMessage(new FileResponse(pathToFile, data));
//                    buffer.rewind();
//                    buffer.clear();
//                }
//            } catch (Exception e) {
//                showAlertWindow(e.getMessage());
//            }

    }

    public void buttonDownloadOnAction(ActionEvent actionEvent) {

    }


    private void showAlertWindow(String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }
}
