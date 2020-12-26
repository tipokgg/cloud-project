import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
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

    private final String clientDir = "cloud-client/files";
    private final String userName = "user1";
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
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
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

    private void requestForUpload(File file) throws IOException {
        os.writeObject(new InitFileTransferMessage(file));
        os.flush();
    }

    private void uploadFileToServer(ReadyForUploadMessage msg) throws IOException {
        File clientSideFile = msg.getClientSideFile();
        File serverSideFile = msg.getServerSideFile(); // для того, чтобы сервер знал в какую папаку писать
        long fileLength = clientSideFile.length();

        // TODO 26.12.20 получение хэша, прикрутить для проверки целостности переданного файла
        try (InputStream is = Files.newInputStream(Paths.get(clientSideFile.toURI()))) {
            String md5 = DigestUtils.md5Hex(is);
            System.out.println("checksum: " + md5);
        }

        long countOfChunks = fileLength/chunkSize + 1;

        new Thread(() -> {
            try {
                // устанавливаем начальную позичию, откуда читать файл
                int currentPosition = 0;
                for (int i = 0; i < countOfChunks; i++) {
                    if (i != countOfChunks -1) {
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

    private void processServerMessage(AbstractMessage msg) throws IOException, InterruptedException {
        if (msg instanceof FileListMessage) {
            serverView.getItems().clear();
            serverView.getItems().addAll(((FileListMessage) msg).getFilesFromServer());
        } else if (msg instanceof ReadyForUploadMessage) {
            uploadFileToServer((ReadyForUploadMessage) msg);
        } else if (msg instanceof ChunkFileMessage) { // TODO вынести в отдельный метод. переделать getServerSideFile и т.д.
            ChunkFileMessage chunkFileMessage = (ChunkFileMessage) msg;
            // выводы в консоль для проверок, что происходит
            String filePath = clientDir + "/" + ((ChunkFileMessage) msg).getSeverSideFile().getName(); // //TODO
            // записываем через RandomAccessFile, предвариьельно выбрав позицию, в какое место файла писать
            // информацию о позиции получаем из сериализованного объекта ChunkFileMessage
            try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
                raf.seek(chunkFileMessage.getPosition());
                raf.write(chunkFileMessage.getData());
            }
        } else if (msg instanceof UpdateClientSideMessage) {
            clientView.getItems().clear();
            clientView.getItems().addAll(getClientFiles());
        } else if (msg instanceof FailAuthMessage) {
            String userName = ((FailAuthMessage) msg).getUserName();

            Alert alert = new Alert(Alert.AlertType.WARNING, "Failed to log in.\n" +
                    "Please make sure that you have entered your login (" + userName + ") and password correctly.",
                    ButtonType.OK);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.showAndWait();

        }
    }
    
    private List<String> getClientFiles() throws IOException {
        Path clientPath = Paths.get(clientDir);
        return Files.list(clientPath)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
    }

    public void upload(ActionEvent actionEvent) throws IOException {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        File file = new File(clientDir + "/" + fileName);
        requestForUpload(file);
    }

    public void download(ActionEvent actionEvent) throws IOException {
        os.writeObject(new RequestFileDownloadMessage(serverView.getSelectionModel().getSelectedItem(), userName));
    }

    public void delete(ActionEvent actionEvent) throws IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you really want delete \""
                + serverView.getSelectionModel().getSelectedItem() + "\" from cloud?",
                ButtonType.YES, ButtonType.NO);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            os.writeObject(new RequestFileDeleteMessage(serverView.getSelectionModel().getSelectedItem()));
            os.flush();
        }

    }

    public void exit(ActionEvent actionEvent) throws IOException {

        Alert alert = new Alert(Alert.AlertType.WARNING, "Exit from application?",
                ButtonType.YES, ButtonType.NO);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            Platform.exit();
        }

    }
}