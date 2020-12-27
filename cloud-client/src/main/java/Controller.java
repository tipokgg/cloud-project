import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    @FXML
    private ListView<String> clientView;
    @FXML
    private ListView<String> serverView;
    private static final Logger LOG = LoggerFactory.getLogger(Controller.class);

    private final String userName = "user1";
    private String currentDir = "cloud-client/files/";
    private ObjectEncoderOutputStream os;
    private ObjectDecoderInputStream is;
    private final int chunkSize = 1024;
    private Stage primaryStage;
    private Scene mainScene;
    private final String defaultServer = "localhost";
    private final int defaultPort = 8189;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            Socket socket = new Socket(defaultServer, defaultPort);
            os = new ObjectEncoderOutputStream(socket.getOutputStream());
            is = new ObjectDecoderInputStream(socket.getInputStream());

            Thread readThread = new Thread(() -> {
                while (true) {
                    try {
                        AbstractMessage msg = (AbstractMessage) is.readObject();
                        Platform.runLater(() -> { // чтобы избавиться от "Not on FX application thread"
                            // плюс некорректно обновлялся ListView при удалении файлов из папки сервера
                            try {
                                processServerMessage(msg);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                     } catch (EOFException e) {
                        e.printStackTrace();
                        LOG.error("Disconnected from server!");
                        break;
                    } catch (ClassNotFoundException | IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            readThread.setDaemon(true);
            readThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processServerMessage(AbstractMessage msg) throws IOException {
        if (msg instanceof FileListMessage) {
            LOG.info("Received list of files from server");
            serverView.getItems().clear();
            serverView.getItems().addAll(((FileListMessage) msg).getFilesFromServer());
        } else if (msg instanceof ReadyForUploadMessage) {
            LOG.info("Uploading file " + ((ReadyForUploadMessage) msg).getClientSideFile().getName() + " to server");
            uploadFileToServer((ReadyForUploadMessage) msg);
        } else if (msg instanceof ChunkFileMessage) { //
            ChunkFileMessage chunkFileMessage = (ChunkFileMessage) msg;
            try (RandomAccessFile raf = new RandomAccessFile(chunkFileMessage.getReceiverFile(), "rw")) {
                raf.seek(chunkFileMessage.getPosition());
                raf.write(chunkFileMessage.getData());
            }
        } else if (msg instanceof SuccessAuthMessage) {
            primaryStage.setScene(mainScene);
            serverView.getItems().clear();
            serverView.getItems().addAll(((SuccessAuthMessage) msg).getFilesFromServer());
            clientView.getItems().addAll(getClientFiles());
            clientView.setOnMouseClicked(new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent click) {

                    if (click.getClickCount() == 2) {
                        //Use ListView's getSelected Item
                        String s = clientView.getSelectionModel()
                                .getSelectedItem();
                        if (s.startsWith("<DIR> ")) {
                            currentDir += s.substring(6) + "/";
                            try {
                                clientView.getItems().clear();
                                clientView.getItems().addAll(getClientFiles()); // TODO refactor в отдельный метод (обновление списка файлов клиента)
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (s.equals(". .")) {
                            String targetDir = currentDir.substring(0, currentDir.length() - 1);
                            targetDir = targetDir.substring(0, targetDir.lastIndexOf("/") + 1);
                            currentDir = targetDir;
                            try {
                                clientView.getItems().clear(); // TODO refactor в отдельный метод (обновление списка файлов клиента)
                                clientView.getItems().addAll(getClientFiles());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }

                    }
                }
            });

        } else if (msg instanceof UpdateClientSideMessage) {
            LOG.info("Received message for update client side list of files");
            clientView.getItems().clear();
            clientView.getItems().addAll(getClientFiles());
        } else if (msg instanceof FailAuthMessage) {
            LOG.info("Received fail auth message");
            String userName = ((FailAuthMessage) msg).getUserName();

            Alert alert = new Alert(Alert.AlertType.WARNING, "Failed to log in.\n" +
                    "Please make sure that you have entered your login (" + userName + ") and password correctly.",
                    ButtonType.OK);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.showAndWait();
        } else if (msg instanceof ExceptionMessage) {
            Alert alert = new Alert(Alert.AlertType.WARNING, ((ExceptionMessage) msg).getExceptionText(),
                    ButtonType.OK);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.setTitle("Error from server");
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }

    private void requestForUpload(File file) throws IOException {
        os.writeObject(new InitFileTransferMessage(file));
        os.flush();
    }


    private void uploadFileToServer(ReadyForUploadMessage msg) throws IOException {
        File clientSideFile = msg.getClientSideFile();
        File serverSideFile = msg.getServerSideFile(); // для того, чтобы сервер знал в какую папку писать
        long fileLength = clientSideFile.length();

        // md5 пока не используется
        try (InputStream is = Files.newInputStream(Paths.get(clientSideFile.toURI()))) {
            String md5 = DigestUtils.md5Hex(is);
        }

        long countOfChunks = fileLength/chunkSize + 1;

        LOG.info("Start uploading " + clientSideFile.getName() + " to server (" + countOfChunks + " chunks)");

        new Thread(() -> {
            try {
                // устанавливаем начальную позичию, откуда читать файл
                int currentPosition = 0;
                for (int i = 0; i < countOfChunks; i++) {
                    if (i != countOfChunks-1) {
                        os.writeObject(new ChunkFileMessage(serverSideFile, clientSideFile, currentPosition, chunkSize));
                        os.flush();
                        currentPosition += chunkSize; //смещение
                    } else {
                        os.writeObject(new ChunkFileMessage(serverSideFile, clientSideFile, currentPosition,(int) fileLength - currentPosition));
                        os.flush();
                    }
                }

                os.writeObject(new FileListRequestMessage(userName)); // обновляем список файлов на сервере
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

    }

    private List<String> getClientFiles() throws IOException {

        File[] files = Objects.requireNonNull(new File(currentDir).listFiles());
        List<String> filesString = new ArrayList<>();
        filesString.add(". .");

        for (File file : files) {
            if (file.isDirectory()) {
                filesString.add("<DIR> " + file.getName());
            } else {
                filesString.add(file.getName());
            }
        }

        return filesString;
    }

    public void upload(ActionEvent actionEvent) throws IOException {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        File file = new File(currentDir + "/" + fileName);
        LOG.info("Sent request for upload " + fileName + " to server");
        requestForUpload(file);
    }

    public void download(ActionEvent actionEvent) throws IOException {
        String fileName = serverView.getSelectionModel().getSelectedItem();
        LOG.info("Sent request for download " + fileName + " from server");
        File fileForWrite = new File(currentDir + fileName);
        os.writeObject(new RequestFileDownloadMessage(fileName, fileForWrite));
    }

    public void delete(ActionEvent actionEvent) throws IOException {

        String fileName = serverView.getSelectionModel().getSelectedItem();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you really want delete \""
                + fileName + "\" from cloud?",
                ButtonType.YES, ButtonType.NO);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setTitle("Delete file");
        alert.setHeaderText(null);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            LOG.info("Sent request for delete " + fileName + " from server");
            os.writeObject(new RequestFileDeleteMessage(fileName));
            os.flush();
        }
    }

    public void exit(ActionEvent actionEvent) throws IOException {

        Alert alert = new Alert(Alert.AlertType.WARNING, "Exit from application?",
                ButtonType.YES, ButtonType.NO);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setTitle(null);
        alert.setHeaderText(null);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            LOG.info("Exit from application");
            os.writeObject(new DisconnectMessage());
            Platform.exit();
        }
    }

    public Scene getLoginScene() throws IOException {

        Stage newWindow = new Stage();
        newWindow.setTitle("Login");
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Scene scene = new Scene(grid, 682, 700);
        newWindow.setScene(scene);

        Text scenetitle = new Text("Enter your Username and Password");
        scenetitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.add(scenetitle, 0, 0, 2, 1);

        Label userName = new Label("Username:");
        grid.add(userName, 0, 1);

        TextField userTextField = new TextField();
        grid.add(userTextField, 1, 1);

        Label pw = new Label("Password:");
        grid.add(pw, 0, 2);

        PasswordField pwBox = new PasswordField();
        grid.add(pwBox, 1, 2);

        Label server = new Label("Server:");
        grid.add(server, 0, 4);

        Label serverAddr = new Label(defaultServer + ":" + defaultPort);
        grid.add(serverAddr, 1, 4);


        Button btn = new Button("Sign in");
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(btn);
        grid.add(hbBtn, 1, 6);


        btn.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent e) {
                try {
                    os.writeObject(new RequestAuthToServerMessage(userTextField.getText(), pwBox.getText()));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

        });

        return scene;

    } // логика сцены для авторизации

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setMainScene(Scene mainScene) {
        this.mainScene = mainScene;
    }

    public void disconnect(ActionEvent actionEvent) throws IOException {

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Disconnect from the server?",
                ButtonType.YES, ButtonType.NO);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setTitle(null);
        alert.setHeaderText(null);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            clientView.getItems().clear();
            serverView.getItems().clear();
            os.writeObject(new DisconnectMessage());
            primaryStage.setScene(getLoginScene());
        }
    }
}