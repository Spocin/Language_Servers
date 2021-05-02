import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainClient extends Application {

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ControllerFXML.fxml"));
        Parent root = loader.load();

        ClientWindowController controller = loader.getController();
        stage.setOnCloseRequest(event -> controller.stop(0));

        stage.setTitle("Language client");
        stage.setScene(new Scene(root,500,200));
        stage.setResizable(false);
        stage.show();
    }
}
