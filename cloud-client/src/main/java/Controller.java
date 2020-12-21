import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private ObjectEncoderOutputStream os;
    private ObjectDecoderInputStream is;
    public TextArea txt;


    // очень много харкода!
    // потом всё будет переработано, разнесено по методам и т.д. Делалось чтобы реализвать передачу файла от клиента на сервер
    public void send(ActionEvent actionEvent) throws IOException, ClassNotFoundException {

        File file = new File("/home/tipokgg/Downloads/discord-0.0.10.deb"); // файл который передаём
        os.writeObject(new InitFileTransferMessage(file)); // передаем сериализованный объект с информацией о файле
        os.flush();
        // получаем от сервера сериализованный объект, который сообщает о готовности сервера принять файл
        // в текущей реализации пока бесполезен
        ReadyForUploadMessage ready = (ReadyForUploadMessage) is.readObject();
        // получаем информацию о файле, который сервер котов принять
        File fileForUpload = ready.file;
        System.out.println(fileForUpload.getName());
        System.out.println("Длина файла в байтах - " + fileForUpload.length());
        // считаем сколько потребуется чанков по 1024 байта для передачи всего файла
        long countOfChunks = fileForUpload.length()/1024 + 1;
        System.out.println("Будет передано чанков - " + countOfChunks);
        // устанавливаем начальную позичию, откуда читать файл
        int currentPosition = 0;

        // в цикле начинаем генерировать чанки, которые являются сериализованными объектами и отправлять на сервер
        // в чанках есть буффер с данными, вычитанными из файла, что это за файл, какой размер чанка и из какой позиции читались байты
        for (int i = 0; i < countOfChunks; i++) {
            if (i != countOfChunks -1) {
                os.writeObject(new ChunkFileMessage(fileForUpload, currentPosition, 1024));
                currentPosition += 1024; // после итерации увеличиваем позицию откуда читать байты на размер чанка
                // в последней итерации создаем чанк, равный количеству байт, который осталось считать из файла до конца,
                // чтобы не делать полный чанк длиной 1024 байта
            } else os.writeObject(new ChunkFileMessage(fileForUpload, currentPosition,fileForUpload.length() - currentPosition));
        }

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            Socket socket = new Socket("localhost", 8189);
            os = new ObjectEncoderOutputStream(socket.getOutputStream());
            is = new ObjectDecoderInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}