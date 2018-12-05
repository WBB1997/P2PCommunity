package Client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static javafx.application.Platform.exit;

public class Main extends Application {
    // key = ipAddress , value = ipAddress + port
    private static final String  groupAddres = "230.0.0.1";
    private static final int  port = 6666;
    private static Set<InetSocketAddress> hostMap = new HashSet<>();
    private MulticastSocket receiver;
    private MulticastSocket sender;
    private ObservableList<DatagramPacket> datagramPacketObservableList = FXCollections.observableArrayList();
    private String name = "吴贝贝";

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        // right_root
        ScrollPane right_root_scro = new ScrollPane();
        VBox right_root = new VBox();
        right_root.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
        right_root_scro.setContent(right_root);
        right_root_scro.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        right_root_scro.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        right_root_scro.setFitToWidth(true);
        right_root_scro.setFitToHeight(true);
        root.setRight(right_root_scro);

        // left_center
        ScrollPane left_center_scro = new ScrollPane();
        VBox left_center = new VBox();
        left_center.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
        left_center.setAlignment(Pos.TOP_CENTER);
        left_center.setSpacing(5);
        left_center.setPadding(new Insets(5, 5, 5, 5));
        left_center.getChildren().addListener((ListChangeListener<Node>) c -> {
            if (c.next() && c.wasAdded())
                left_center_scro.setVvalue(1.0);
        });
        left_center_scro.setContent(left_center);
        left_center_scro.setFitToWidth(true);
        left_center_scro.setFitToHeight(true);
        left_center_scro.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        left_center_scro.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // left_bottom_top
        HBox left_bottom_top = new HBox();
        left_bottom_top.setAlignment(Pos.BOTTOM_LEFT);
        Button expression = new Button("表情");
        left_bottom_top.getChildren().add(expression);

        // inputArea
        TextArea inputArea = new TextArea();
        inputArea.setWrapText(true);
        DoubleBinding binding = primaryStage.heightProperty().divide(4);
        inputArea.minHeightProperty().bind(binding);
        inputArea.setPromptText("在这里输入消息");

        // left_bottom_bottom
        HBox left_bottom_bottom = new HBox();
        left_bottom_bottom.setAlignment(Pos.BOTTOM_RIGHT);
        left_bottom_bottom.setPadding(new Insets(5, 10, 5, 0));
        left_bottom_bottom.setSpacing(5);
        Button close = new Button("关闭");
        Button send = new Button("发送");
        left_bottom_bottom.getChildren().addAll(close, send);

        // left_bottom
        VBox left_bottom = new VBox();
        left_bottom.getChildren().addAll(left_bottom_top, inputArea, left_bottom_bottom);

        // left_root
        VBox left_root = new VBox();
        left_root.getChildren().addAll(left_center_scro, left_bottom);
        VBox.setVgrow(left_center_scro, Priority.ALWAYS);
        root.setCenter(left_root);

        primaryStage.setTitle("p2pCommunity");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.getScene().getStylesheets().add("style.css");
        primaryStage.show();
        initSocket();
        // event
//        left_center_scro.setOnScroll(event -> {
//            System.out.println("width->" + left_center_scro.getViewportBounds().getWidth());
//            System.out.println("height->" + left_center_scro.getViewportBounds().getHeight());
//            System.out.println("MinY->" + left_center_scro.getViewportBounds().getMinY());
//            System.out.println("MaxY->" + left_center_scro.getViewportBounds().getMaxY());
//            System.out.println("height->" + left_center_scro.getHeight());
//            System.out.println("width->" + left_center_scro.getWidth());
//            System.out.println("==============================================");
//        });
        close.setOnAction(event -> System.exit(0));
        send.setOnAction(event -> {
            String message = inputArea.getText();
            send(message.getBytes());
            left_center.getChildren().add(getMessagePane(name, message, new Image("file:res/user.png", 32, 32, true, true), 1));
        });
        datagramPacketObservableList.addListener((ListChangeListener<DatagramPacket>) c -> {
            System.out.println(Thread.currentThread().getName());
            if (c.next() && c.wasAdded()) {
                for (DatagramPacket datagramPacket : c.getAddedSubList()) {
                    Platform.runLater(() -> left_center.getChildren().add(getMessagePane(datagramPacket.getSocketAddress().toString(), datagramPacket.getData().toString(), new Image("file:res/user.png", 32, 32, true, true), -1)));
                }
            }
        });
        primaryStage.setOnCloseRequest(event -> System.exit(0));
    }

    private void initSocket(){
        try {
            InetAddress ip = InetAddress.getByName(groupAddres);
            receiver = new MulticastSocket(port);
            sender = new MulticastSocket();
            sender.setLoopbackMode(true);
            receiver.setLoopbackMode(true);
            receiver.joinGroup(ip);
            sender.joinGroup(ip);
            new Thread(this::listen).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listen() {
        byte[] data = new byte[1024];
        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(data, data.length);
                System.out.println("1");
                //receive()是阻塞方法，等待其他人发来消息
                receiver.receive(packet);
                InetSocketAddress inetSocketAddress = (InetSocketAddress) packet.getSocketAddress();
                hostMap.add(inetSocketAddress);
                //如果是第一次上线的人，那么弹出提示窗口
                //...
                // 将信息加入待发送列表，更新在窗口
                datagramPacketObservableList.add(packet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            receiver.close();
        }
    }

    private void send(byte[] data){
        try {
            InetAddress ip = InetAddress.getByName(groupAddres);
            DatagramPacket packet = new DatagramPacket(data, data.length, ip, port);
            sender.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Pane getMessagePane(String name, String message, Image buddha, int flag){
        VBox nameText = new VBox();
        nameText.getChildren().add(new Text(name));
        Text messageText = new Text(message);
        ImageView imageView = new ImageView(buddha);
        GridPane gridPane = new GridPane();
        VBox root = new VBox();
        if(flag == 1){
            nameText.setAlignment(Pos.CENTER_RIGHT);
            root.setAlignment(Pos.CENTER_RIGHT);
            gridPane.setAlignment(Pos.CENTER_RIGHT);
            gridPane.add(nameText, 0,0,3,1);
            gridPane.add(messageText, 0,1,3,1);
            gridPane.add(imageView, 3,0,1,2);
        }else if (flag == -1){
            nameText.setAlignment(Pos.CENTER_LEFT);
            root.setAlignment(Pos.CENTER_LEFT);
            gridPane.setAlignment(Pos.CENTER_LEFT);
            gridPane.add(nameText, 1,0,3,1);
            gridPane.add(messageText, 1,1,3,1);
            gridPane.add(imageView, 0,0,1,2);
        }
        gridPane.setHgap(10);
        root.setFillWidth(true);
        root.getChildren().add(gridPane);
        return root;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
