import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class test extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Group group = new Group();
        TextArea textArea = new TextArea();
        textArea.setBackground(null);
        group.getChildren().add(textArea);


        Scene scene = new Scene(group, 800, 600);
        primaryStage.setScene(scene);
//        primaryStage.getScene().getStylesheets().add("style.css");
        primaryStage.show();
//        ScenicView.show(scene);
    }
}
