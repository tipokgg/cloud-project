import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

public class ClientApp extends Application {


    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("layout.fxml"));
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}

// TODO 26/11/2020 доделать логику передачи файла с сервера клиенту, после изменения ChunkFileMessage
// TODO теперь клиент и сервер при передаче файлов используют File со стороны сервера (откуда качают) и со стороны клиента (куда качают)