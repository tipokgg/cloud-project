import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

public class ClientApp extends Application {


    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("layout.fxml"));
        Image e = new Image("cloud-icon.png");
        Parent root = fxmlLoader.load();
        Scene mainScene = new Scene(root);
        Controller controller = fxmlLoader.getController();
        controller.setPrimaryStage(primaryStage);
        controller.setMainScene(mainScene);
        primaryStage.getIcons().add(e);
        primaryStage.setScene(controller.getLoginScene());
        primaryStage.setResizable(false);
        primaryStage.setTitle("GB Cloud Project");
        primaryStage.show();
    }
}

// TODO refactor refactor refactor... проверки на null. чтобы null не уходили на сервер от клиента и наоборот.
// TODO расставить логгер на ВСЕ события как так клиенте так и на севере