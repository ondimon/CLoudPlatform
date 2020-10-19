import filetransfer.FileLoader;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import messages.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.stage.DirectoryChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ControllerFileManager implements Initializable {
    private static final Logger logger = LogManager.getLogger(ControllerFileManager.class.getName());

    private SeverListener severListener;
    private ConcurrentHashMap<UUID, Node> fileProgressNodes = new ConcurrentHashMap<>();

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

    @FXML
    VBox clientRoot;

    @FXML
    VBox serverRoot;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        severListener = SeverListener.getInstance();
        severListener.setCallback(message -> {
            if(message instanceof FileListResponse ) {
                updateServerFileList((FileListResponse) message);
            }else if(message instanceof FileLoad) {

                sendFileListRequest();

                FileLoad fileLoad = (FileLoad) message;
                removeProgress(clientRoot, fileLoad.getFileHeader());
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

        FileHeader fileHeader = new FileHeader();
        fileHeader.setClientPath(pathToFile.toString());
        fileHeader.setLength(pathToFile.toFile().length());
        FileUploadRequest fileUploadRequest = new FileUploadRequest(fileHeader);
        severListener.sendMessage(fileUploadRequest);

        addProgress(clientRoot, "Upload: " + fileName, fileHeader);
    }

    public void buttonDownloadOnAction(ActionEvent actionEvent) {
        String fileName = serverFiles.getSelectionModel().getSelectedItem();
        Path pathToFile = Paths.get(pathDir.getText(), fileName);
        FileHeader fileHeader = new FileHeader();
        fileHeader.setClientPath(pathToFile.toString());

        addProgress(serverRoot, "Download: " + fileName, fileHeader);

        FileLoader fileLoader = null;
        try {
            fileLoader = new FileLoader(pathToFile, fileHeader);
            severListener.registerFileLoader(fileLoader);
            fileLoader.setCallback((message -> {
                if(message instanceof FileLoad) {
                    updateClientFileList();
                    FileLoad fileLoad = (FileLoad) message;
                    removeProgress(serverRoot, fileLoad.getFileHeader());
                }
            }));
        } catch (IOException e) {
            logger.error(e.getMessage());
            showAlertWindow(e.getMessage());
        }
        severListener.sendMessage(new FileDownloadRequest(fileHeader));

    }

    private void showAlertWindow(String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    private void addProgress(Pane root, String labelText, FileHeader fileHeader) {
        FlowPane flowPane = new FlowPane(10, 10);
        ProgressBar progressBar = new ProgressBar();
        Label label = new Label();
        label.setText(labelText);
        flowPane.getChildren().addAll(label, progressBar);
        root.getChildren().add(flowPane);
        fileProgressNodes.put(fileHeader.getUuid(), flowPane);
    }

    private void removeProgress(Pane root, FileHeader fileHeader) {
        UUID uuid = fileHeader.getUuid();
        Node node = fileProgressNodes.get(uuid);

        Duration displayDuration = Duration.millis(3000);
        KeyFrame displayDurationKeyFrame = new KeyFrame(displayDuration);

        Timeline timeline = new Timeline(displayDurationKeyFrame);
        timeline.setOnFinished(e -> {
            root.getChildren().remove(node);
            fileProgressNodes.remove(uuid);
        });
        timeline.play();
    }
}
