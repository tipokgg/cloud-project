import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

public class ClientApp extends Application {


    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("layout.fxml"));
        Parent root = fxmlLoader.load();
        Scene mainScene = new Scene(root);
        Controller controller = fxmlLoader.getController();
        controller.setPrimaryStage(primaryStage);
        controller.setMainScene(mainScene);
        primaryStage.setScene(controller.getLoginScene());
        primaryStage.setResizable(false);
        primaryStage.setTitle("GB Cloud Project");
        primaryStage.show();
    }
}

// TODO перемещение по каталогам (частично сделано)
// TODO сделать возможность вернуться на каталог выше (доделать возмонжные ошибки, типа выход за пределы пути)
// TODO Добавлять папки наверх списка файлов
// TODO сделать выбор стартовой пааки клиента?
// TODO рейнейм файла на сервере
// TODO проверка наличия файла с таким именем в папке при загрузке на сервер или при скачивании на клиенте