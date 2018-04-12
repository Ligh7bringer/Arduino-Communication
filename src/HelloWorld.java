import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class HelloWorld extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hello World!");
        SerialComm.updateServer();
        if(SerialComm.isValidResponse()){
            //play door animation
        }
        else{
            //display error
        }

        //primaryStage.setScene(new Scene(root, 300, 250));
        primaryStage.show();
    }
}