import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
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
                            } catch (IOException | ClassNotFoundException e) {
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
            os.writeObject(new FileListRequestMessage(userName));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void requestForUpload(File file) throws IOException {
        os.writeObject(new InitFileTransferMessage(file));
        os.flush();
    }

    private void uploadFileToServer(ReadyForUploadMessage msg) throws IOException {
        File fileForUpload = msg.getFile();

        try (InputStream is = Files.newInputStream(Paths.get(fileForUpload.toURI()))) {
            String md5 = DigestUtils.md5Hex(is);
            System.out.println("checksum: " + md5);
        }

        System.out.println("Длина файла в байтах - " + fileForUpload.length());
        long countOfChunks = fileForUpload.length()/1024 + 1;
        System.out.println("Будет передано чанков - " + countOfChunks);


        new Thread(() -> {
            try {
                // устанавливаем начальную позичию, откуда читать файл
                int currentPosition = 0;
                // в цикле начинаем генерировать чанки, которые являются сериализованными объектами и отправлять на сервер
                // в чанках есть буффер с данными, вычитанными из файла, что это за файл, какой размер чанка и из какой позиции читались байты
                for (int i = 0; i < countOfChunks; i++) {
                    if (i != countOfChunks -1) {
                        os.writeObject(new ChunkFileMessage(fileForUpload, currentPosition, 1024));
                        os.flush();
                        currentPosition += 1024; // после итерации увеличиваем позицию откуда читать байты на размер чанка
                        // в последней итерации создаем чанк, равный количеству байт, который осталось считать из файла до конца,
                        // чтобы не делать полный чанк длиной 1024 байта
                    } else {
                        os.writeObject(new ChunkFileMessage(fileForUpload, currentPosition,fileForUpload.length() - currentPosition));
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

    private void processServerMessage(AbstractMessage msg) throws IOException, ClassNotFoundException {
        if (msg instanceof FileListMessage) {
            serverView.getItems().clear();
            serverView.getItems().addAll(((FileListMessage) msg).getFilesFromServer());
        } else if (msg instanceof ReadyForUploadMessage) {
            uploadFileToServer((ReadyForUploadMessage) msg);
        } else if (msg instanceof ChunkFileMessage) {
            ChunkFileMessage chunkFileMessage = (ChunkFileMessage) msg;
            // выводы в консоль для проверок, что происходит
            String filePath = clientDir + "/" + ((ChunkFileMessage) msg).getFile().getName(); // //TODO
            // записываем через RandomAccessFile, предвариьельно выбрав позицию, в какое место файла писать
            // информацию о позиции получаем из сериализованного объекта ChunkFileMessage
            try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
                raf.seek(chunkFileMessage.getPosition());
                raf.write(chunkFileMessage.getData());
            }
        } else if (msg instanceof UpdateClientSideMessage) {
            clientView.getItems().clear();
            clientView.getItems().addAll(getClientFiles());
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
            //TODO
    }
}