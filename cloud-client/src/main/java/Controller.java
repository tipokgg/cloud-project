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
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    @FXML
    private ListView<String> clientView;
    @FXML
    private ListView<String> serverView;
    private static final Logger LOG = LoggerFactory.getLogger(Controller.class);

    private final String userName = "user1";
    private final String currentDir = "cloud-client/files/";
    private ObjectEncoderOutputStream os;
    private ObjectDecoderInputStream is;
    private final int chunkSize = 1024;

    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            Socket socket = new Socket("localhost", 8189);
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

            clientView.getItems().addAll(getClientFiles());
            os.writeObject(new RequestAuthToServerMessage(userName, "123123"));

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
            System.out.println("checksum: " + md5);
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
        Path clientPath = Paths.get(currentDir);
        return Files.list(clientPath)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
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
            Platform.exit();
        }
    }

    public void connect(ActionEvent actionEvent) throws IOException {



        // New window (Stage)
        Stage newWindow = new Stage();
        newWindow.setTitle("Second Stage");
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Scene scene = new Scene(grid, 682, 700);
        newWindow.setScene(scene);

        Text scenetitle = new Text("Welcome");
        scenetitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.add(scenetitle, 0, 0, 2, 1);

        Label userName = new Label("User Name:");
        grid.add(userName, 0, 1);

        TextField userTextField = new TextField();
        grid.add(userTextField, 1, 1);

        Label pw = new Label("Password:");
        grid.add(pw, 0, 2);

        PasswordField pwBox = new PasswordField();
        grid.add(pwBox, 1, 2);

        Button btn = new Button("Sign in");
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(btn);
        grid.add(hbBtn, 1, 4);

        final Text actiontarget = new Text();
        grid.add(actiontarget, 1, 6);
        newWindow.show();

        btn.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent e) {
                actiontarget.setFill(Color.FIREBRICK);
                actiontarget.setText(userTextField.getText() + " " + pwBox.getText());

            }


        });



    }
}