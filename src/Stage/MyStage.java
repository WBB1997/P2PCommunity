package Stage;

import Pane.EmojiPane;
import Pane.RichTextPane;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;

class MyStage extends Stage {
    private VBox left_center;
    private Button send;
    private RichTextPane inputArea;
    private boolean flag = true;

    MyStage(){
        super();
        BorderPane root = new BorderPane();
        // left_center
        StackPane stackPane = new StackPane();
        ScrollPane left_center_scro = new ScrollPane();
        left_center_scro.setId("left_center_scro");
        left_center = new VBox();
        left_center.getStyleClass().add("vbox");
        left_center.setAlignment(Pos.TOP_CENTER);
        left_center.getChildren().addListener((ListChangeListener<Node>) c -> {
            if (c.next() && c.wasAdded())
                left_center_scro.setVvalue(1.0);
        });
        left_center.requestFocus();
        left_center_scro.setContent(left_center);
        EmojiPane emojiPane = new EmojiPane(inputArea);
        emojiPane.setVisible(false);
        emojiPane.setOnMouseClicked(event -> emojiPane.setVisible(false));
        stackPane.getChildren().addAll(left_center_scro, emojiPane);

        // left_bottom_top
        HBox left_bottom_top = new HBox();
        left_bottom_top.setId("left_bottom_top");
        Button expression = new Button("表情");
        left_bottom_top.getChildren().add(expression);

        // inputArea
        inputArea = new RichTextPane();
        inputArea.requestFocus();
        DoubleBinding height = this.heightProperty().divide(4);
        inputArea.minHeightProperty().bind(height);
        inputArea.maxHeightProperty().bind(height);

        // left_bottom_bottom
        HBox left_bottom_bottom = new HBox();
        left_bottom_bottom.setId("left_bottom_bottom");
        Button close = new Button("关闭");
        send = new Button("发送");
        left_bottom_bottom.getChildren().addAll(close, send);

        // left_bottom
        VBox left_bottom = new VBox();
        left_bottom.getChildren().addAll(left_bottom_top, inputArea, left_bottom_bottom);

        // left_root
        VBox left_root = new VBox();
        left_root.setFillWidth(true);
        left_root.getChildren().addAll(stackPane, left_bottom);
        VBox.setVgrow(stackPane, Priority.ALWAYS);
        root.setCenter(left_root);

        Scene scene = new Scene(root, 800, 600);

        this.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(!oldValue && newValue)
                flag = true;
        });
        this.setScene(scene);
        this.getScene().getStylesheets().add("style.css");

        // 表情包
        expression.setOnAction(event -> emojiPane.setVisible(!emojiPane.isVisible()));
        close.setOnAction(event -> this.hide());
        inputArea.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.ENTER) {
                send.fire();
                event.consume();
            }
        });
    }

    Button getSend() {
        return send;
    }

    RichTextPane getRichTextPane() {
        return inputArea;
    }

    VBox getLeft_center() {
        return left_center;
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }
}
