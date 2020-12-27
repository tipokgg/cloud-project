import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

public class ClientApp extends Application {


    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("layout.fxml"));
        primaryStage.setScene(new LoginScene(primaryStage, root).getScene());
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}

// TODO рейнейм файла на сервере
// TODO авторизация по логину паролю
// TODO проверять есть ли папка пользователя на сервере и если нет то создавать её
// TODO проверка наличия файла с таким именем в папке при загрузке на сервер или при скачивании на клиенте