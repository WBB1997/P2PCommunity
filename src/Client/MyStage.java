package Client;

import Bean.Host;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import sun.misc.BASE64Decoder;

import java.io.ByteArrayInputStream;

class MyStage extends Stage {
    private VBox left_center;
    private Button send;
    private TextArea inputArea;
    MyStage(){
        super();
        BorderPane root = new BorderPane();
        // left_center
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

        // left_bottom_top
        HBox left_bottom_top = new HBox();
        left_bottom_top.setId("left_bottom_top");
        Button expression = new Button("表情");
        left_bottom_top.getChildren().add(expression);

        // inputArea
        inputArea = new TextArea();
        inputArea.setWrapText(true);
        inputArea.requestFocus();
        DoubleBinding height = this.heightProperty().divide(4);
        inputArea.minHeightProperty().bind(height);
        inputArea.setPromptText("在这里输入消息");

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
        left_root.getChildren().addAll(left_center_scro, left_bottom);
        VBox.setVgrow(left_center_scro, Priority.ALWAYS);
        root.setCenter(left_root);

        Scene scene = new Scene(root, 800, 600);

        this.setScene(scene);
        this.getScene().getStylesheets().add("style.css");
    }

    Button getSend() {
        return send;
    }

    TextArea getInputArea() {
        return inputArea;
    }

    VBox getLeft_center() {
        return left_center;
    }

    //base64字符串转化成图片
    private static ByteArrayInputStream GenerateImage(String imgStr) {   //对字节数组字符串进行Base64解码并生成图片
        if (imgStr == null) //图像数据为空
            return null;
        BASE64Decoder decoder = new BASE64Decoder();
        try {
            //Base64解码
            byte[] b = decoder.decodeBuffer(imgStr);
            for (int i = 0; i < b.length; ++i) {
                if (b[i] < 0) {//调整异常数据
                    b[i] += 256;
                }
            }
            return new ByteArrayInputStream(b);
        } catch (Exception e) {
            return null;
        }
    }
}
