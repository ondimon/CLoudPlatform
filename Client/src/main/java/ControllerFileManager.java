import filetransfer.FileLoader;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ControllerFileManager implements Initializable {
    private static final Logger logger = LogManager.getLogger(ControllerFileManager.class.getName());
    private static final String NEW_FOLDER_NAME = "New folder";

    private SeverListener severListener;
    private ConcurrentHashMap<UUID, Node> fileProgressNodes = new ConcurrentHashMap<>();

    @FXML
    ListView<FileHeader> serverFiles;
    final ObservableList<FileHeader> serverFilesList = FXCollections.observableArrayList();

    @FXML
    ListView<FileHeader> clientFiles;
    final ObservableList<FileHeader> clientFilesList = FXCollections.observableArrayList();

    @FXML
    TextField clientPathDir;

    @FXML
    TextField serverPathDir;

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
        clientPathDir.setText(System.getProperty("user.home", ""));
        serverPathDir.setText("");

        initializeClientFilesList();
        initializeServerFilesList();

        severListener = SeverListener.getInstance();
        severListener.setCallback(message -> {
            if(message instanceof FileListResponse ) {
                updateServerFileList((FileListResponse) message);
            }else if(message instanceof FileLoad) {
                FileLoad fileLoad = (FileLoad) message;
                removeProgress(clientRoot, fileLoad.getFileHeader());
            }
        });

        if(!clientPathDir.getText().equals("")) {
            updateClientFileList();
        }

        sendFileListRequest();

    }

    public void updateServerFileList(FileListResponse message) {
        Platform.runLater(() -> {
            serverFilesList.clear();
            serverFilesList.addAll(message.getFileList());
            FileHeader fileHeader = new FileHeader("...", true, 0);
            serverFilesList.add(0, fileHeader);

            serverPathDir.setText(message.getDirPath());
        });
    }

    public void updateClientFileList() {
        Platform.runLater(() -> {
            clientFilesList.clear();
            String path = clientPathDir.getText();

            if(path.equals("")) {
                return;
            }

            try {

                ArrayList<FileHeader> fileList = FileUtility.getListFilesHeader(Paths.get(path));
                clientFilesList.addAll(fileList);
                FileHeader fileHeader = new FileHeader("...", true, 0);
                clientFilesList.add(0, fileHeader);


            } catch (IOException e) {
                showAlertWindow(e.getMessage());
            }
        });
    }

    public void sendFileListRequest() {
        FileListRequest fileListRequest = new FileListRequest(serverPathDir.getText());
        severListener.sendMessage(fileListRequest);

    }

    public void directoryChoose(ActionEvent actionEvent) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directories");

        String curDir = clientPathDir.getText();
        if(! curDir.equals("")) {
            directoryChooser.setInitialDirectory(new File(curDir));
        }


        File dir = directoryChooser.showDialog(clientPathDir.getScene().getWindow());
        if (dir != null) {
            clientPathDir.setText(dir.getAbsolutePath());
        } else {
            clientPathDir.setText(null);
        }
        updateClientFileList();
    }

    public void buttonUploadOnAction(ActionEvent actionEvent) {
        FileHeader fileName = clientFiles.getSelectionModel().getSelectedItem();
        if(fileName.isFolder()) {
            return;
        }
        Path pathToFile = Paths.get(clientPathDir.getText(), fileName.getFileName());

        FileHeader fileHeader = new FileHeader();
        fileHeader.setClientPath(pathToFile.toString());
        fileHeader.setLength(pathToFile.toFile().length());
        FileUploadRequest fileUploadRequest = new FileUploadRequest(fileHeader);
        severListener.sendMessage(fileUploadRequest);

        addProgress(clientRoot, "Upload: " + fileHeader.getFileName(), fileHeader);
    }

    public void buttonDownloadOnAction(ActionEvent actionEvent) {
        FileHeader fileName = serverFiles.getSelectionModel().getSelectedItem();
        Path pathToFile = Paths.get(clientPathDir.getText(), fileName.getFileName());
        FileHeader fileHeader = new FileHeader();
        fileHeader.setClientPath(pathToFile.toString());

        addProgress(serverRoot, "Download: " + fileName.getFileName(), fileHeader);

        FileLoader fileLoader = null;
        try {
            fileLoader = new FileLoader(pathToFile, fileHeader);
            severListener.registerFileLoader(fileLoader);
            fileLoader.setCallback((message -> {
                if(message instanceof FileLoad) {
                    updateClientFileList();
                    FileLoad fileLoad = (FileLoad) message;
                    FileHeader fileHeaderLoad  = fileLoad.getFileHeader();
                    removeProgress(serverRoot, fileHeaderLoad);
                    severListener.unRegisterFileLoader(fileHeaderLoad);
                }
            }));
        } catch (IOException e) {
            logger.error(e.getMessage());
            showAlertWindow(e.getMessage());
        }
        severListener.sendMessage(new FileDownloadRequest(fileHeader));

    }

    private void initializeClientFilesList() {
        clientFiles.setItems(clientFilesList);
        clientFiles.setCellFactory(param -> new FileClientCell(clientFiles));

        clientFiles.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                FileHeader fileHeader = clientFiles.getSelectionModel().getSelectedItem();
                if(fileHeader.isFolder()) {
                    if(fileHeader.getFileName().equals("...")) {
                        clientPathDir.setText(Paths.get(clientPathDir.getText()).getParent().toString());
                    }else {
                        clientPathDir.setText(Paths.get(clientPathDir.getText(), fileHeader.getFileName()).toString());
                    }
                    updateClientFileList();
                }
            }
        });

        ContextMenu contextMenu = new ContextMenu();
        MenuItem createDirectory = new MenuItem("Create directory");
        MenuItem renameFile = new MenuItem("Rename file");
        MenuItem deleteFile = new MenuItem("Delete file");

        createDirectory.setOnAction((event) -> {
            try {
                Files.createDirectory(Paths.get(clientPathDir.getText(), NEW_FOLDER_NAME));
                FileHeader fileHeader = new FileHeader(NEW_FOLDER_NAME, true, 0);
                Platform.runLater(() -> {
                    clientFilesList.add(fileHeader);
                    clientFiles.layout();
                    clientFiles.scrollTo(fileHeader);
                    clientFiles.setEditable(true);
                    clientFiles.edit(clientFiles.getItems().indexOf(fileHeader));
                });
            } catch (IOException e) {
                logger.error(e);
                e.printStackTrace();
            }

        });

        renameFile.setOnAction((event) -> {
            Platform.runLater(() -> {
                clientFiles.setEditable(true);
                clientFiles.edit(clientFiles.getSelectionModel().getSelectedIndex());
            });
        });

        deleteFile.setOnAction((event) -> {
            FileHeader fileName = clientFiles.getSelectionModel().getSelectedItem();
            if(fileName == null) {
                return;
            }
            try {
                Files.deleteIfExists(Paths.get(clientPathDir.getText(), fileName.getFileName()));
                updateClientFileList();
            } catch (IOException e) {
                showAlertWindow(e.getMessage());
                logger.error(e);
            }
        });

        contextMenu.getItems().addAll(createDirectory, renameFile, deleteFile);
        clientFiles.setContextMenu(contextMenu);
    }

    private void initializeServerFilesList() {
        serverFiles.setItems(serverFilesList);
        serverFiles.setCellFactory(param -> new FileServerCell(serverFiles));

        serverFiles.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                FileHeader fileHeader = serverFiles.getSelectionModel().getSelectedItem();
                if(fileHeader.isFolder()) {
//                    if(fileHeader.getFileName().equals("...")) {
//                        serverPathDir.setText(Paths.get(clientPathDir.getText()).getParent().toString());
//                    }else {
//                        serverPathDir.setText(Paths.get(clientPathDir.getText(), fileHeader.getFileName()).toString());
//                    }
                    severListener.sendMessage(new FileListRequest(fileHeader.getFileName()));
                }
            }
        });

        ContextMenu contextMenu = new ContextMenu();
        MenuItem createDirectory = new MenuItem("Create directory");
        MenuItem renameFile = new MenuItem("Rename file");
        MenuItem deleteFile = new MenuItem("Delete file");

        createDirectory.setOnAction((event) -> {
                Platform.runLater(() -> {
                    FileHeader fileHeader = new FileHeader(NEW_FOLDER_NAME, true, 0);
                    serverFilesList.add(fileHeader);
                    serverFiles.layout();
                    serverFiles.scrollTo(fileHeader);
                    serverFiles.setEditable(true);
                    serverFiles.edit(serverFiles.getItems().indexOf(fileHeader));
                });
        });

        renameFile.setOnAction((event) -> {
            Platform.runLater(() -> {
                serverFiles.setEditable(true);
                serverFiles.edit(serverFiles.getSelectionModel().getSelectedIndex());
            });
        });

        deleteFile.setOnAction((event) -> {
            String fileName = serverFiles.getSelectionModel().getSelectedItem().getFileName();
            severListener.sendMessage(new FileDeleteRequest(fileName));
        });

        contextMenu.getItems().addAll(createDirectory, renameFile, deleteFile);
        serverFiles.setContextMenu(contextMenu);
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

    private class FileCell extends ListCell<FileHeader> {
        private ImageView imageView = new ImageView();
        private Image folder = new Image(ControllerFileManager.class.getResourceAsStream("folder.png"));
        private Image up = new Image(ControllerFileManager.class.getResourceAsStream("up.png"));
        protected final TextField textField = new TextField();
        private ListView<FileHeader> filesList;

        public FileCell(ListView<FileHeader> filesList) {
            imageView.setFitHeight(16);
            imageView.setFitWidth(16);

            textField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });

            this.filesList = filesList;
        }

        @Override
        public void startEdit() {
            super.startEdit();
            setGraphic(textField);
            textField.setText(getItem().getFileName());
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            textField.requestFocus();
            textField.selectAll();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setContentDisplay(ContentDisplay.LEFT);
            updateItem(getItem(), false);
            filesList.setEditable(false);
            layout();
        }

        @Override
        protected void updateItem(FileHeader item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null ) {
                imageView.setImage(null);
                setGraphic(imageView);
                setText(null);
            } else {

                if (item.isFolder() ) {
                    if(item.getFileName().equals("...")) {
                        imageView.setImage(up);
                    }else{
                        imageView.setImage(folder);
                    }

                    setText(String.format("[%s]", item.getFileName()));
                }else{
                    imageView.setImage(null);
                    setText(String.format("%s", item.getFileName()));
                }
                setGraphic(imageView);
            }
        }

    }

    private class FileClientCell extends FileCell {
        public FileClientCell(ListView<FileHeader> filesList) {
            super(filesList);
            textField.setOnAction(e -> {
                FileHeader fileHeader = getItem();
                File oldFile = Paths.get(clientPathDir.getText(), fileHeader.getFileName()).toFile();
                File newFile = Paths.get(clientPathDir.getText(), textField.getText()).toFile();
                oldFile.renameTo(newFile);
                fileHeader.setFileName(textField.getText());
                setContentDisplay(ContentDisplay.LEFT);
                updateItem(fileHeader, false);
                clientFiles.setEditable(false);
                layout();
            });
        }
    }

    private class FileServerCell extends FileCell {
        public FileServerCell(ListView<FileHeader> filesList) {
            super(filesList);
            textField.setOnAction(e -> {
                FileHeader fileHeader = getItem();
                String oldFile = fileHeader.getFileName();
                String newFile = textField.getText();
                if(oldFile.equals(NEW_FOLDER_NAME)) {
                    severListener.sendMessage(new CreateDirectoryRequest(newFile));
                }else{
                    severListener.sendMessage(new FileRenameRequest(oldFile, newFile));
                }

            });
        }
    }
}
