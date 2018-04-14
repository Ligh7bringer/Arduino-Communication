import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.json.JSONException;

import java.io.IOException;

public class Main extends Application {
    private static final int COLUMNS  =   4;
    private static final int ROWS = 1;
    private static final int COUNT    =  4;
    private static final int WIDTH    = 228;
    private static final int HEIGHT   = 303;

    private static final Image door = new Image( "file:res/lock-door.png" );
    private static ImageView imageView = new ImageView(door);
    Button btnReset;
    Label lblInfo;

    public static void main(String[] args) throws IOException, JSONException {
        launch(args);
    }

    ImageViewSprite animation = new ImageViewSprite(imageView,
            new Image("file:res/lock-door.png"),
            COLUMNS,
            ROWS,
            COUNT,
            WIDTH,
            HEIGHT,
            24);

    @Override
    public void start(Stage theStage)
    {
        animation.stop();

        BorderPane border = new BorderPane();
        border.setTop(addButton());
        border.setCenter(addDoor());
        border.setBottom(addLabel());

        try {
            SerialComm.initialise();
        } catch (Exception e) {
            System.out.println("No Arduino detected!");
            return;
        }

        theStage.setTitle( "Door Lock" );

        theStage.setScene(new Scene(new Group(border)));
        theStage.setWidth(800);
        theStage.setHeight(600);

        new AnimationTimer()
        {
            @Override
            public void handle(long l) {
                if(SerialComm.isValidResponse()) {
                    animation.start();
                    animation.setPlay(true);
                    lblInfo.setText("Valid key.");
                }
            }
        }.start();

        theStage.setOnCloseRequest(windowEvent -> SerialComm.shutDown());

        theStage.show();
    }

    private HBox addButton() {
        HBox hbox = new HBox();
        hbox.setPadding(new Insets(15, 12, 15, 350));

        btnReset = new Button();
        btnReset.setText("Close door");
        btnReset.setOnAction(actionEvent -> {
            animation.resetAnimation();
            animation.stop();
            lblInfo.setText("Door locked.");
        });

        hbox.getChildren().addAll(btnReset);

        return hbox;
    }

    private VBox addDoor() {
        VBox vbox = new VBox();
        vbox.setPadding(new Insets(100, 0, 0, 280));

        vbox.getChildren().add(imageView);

        return vbox;
    }

    private HBox addLabel() {
        HBox hbox = new HBox();
        hbox.setPadding(new Insets(15, 12, 15, 350));

        lblInfo = new Label("Door locked.");
        hbox.getChildren().add(lblInfo);

        return hbox;
    }

}