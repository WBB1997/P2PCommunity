package Client;

import Bean.Host;
import com.alibaba.fastjson.JSONObject;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main extends Application {
    // key = ipAddress , value = ipAddress + port
    private static final String groupAddres = "230.0.0.1";
    private static final int Port = 1234;
    private MulticastSocket receiver;
    private MulticastSocket sender;
    private ObservableList<DatagramPacket> datagramPacketObservableList = FXCollections.observableArrayList();
    private ObservableSet<Host> hostSet = FXCollections.observableSet();
    private VBox right_root;

    // 个人信息
    private String name = "吴贝贝";
    private String imgFile = "res/user.png";


    private static final int ON_LINE = 1;           // 上线通知
    private static final int OFF_LINE = 2;         // 下线
    private static final int UPDATE_USER_INFO = 3; // 更新个人信息
    private static final int GET_USER_LIST = 4;    // 获取用户列表
    private static final int GROUP_SENDING = 5;    // 群聊
    private static final int PRIVATE_CHAT = 6;     // 私聊


    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        // right_root
        right_root = new VBox();
        right_root.setId("right_root");
        ScrollPane right_root_scro = new ScrollPane();
        right_root_scro.setId("right_root_scro");
        right_root_scro.setContent(right_root);
        root.setRight(right_root_scro);

        // left_center
        ScrollPane left_center_scro = new ScrollPane();
        left_center_scro.setId("left_center_scro");
        VBox left_center = new VBox();
        left_center.getStyleClass().add("vbox");
        left_center.setAlignment(Pos.TOP_CENTER);
        left_center.getChildren().addListener((ListChangeListener<Node>) c -> {
            if (c.next() && c.wasAdded())
                left_center_scro.setVvalue(1.0);
        });
        left_center_scro.setContent(left_center);

        // left_bottom_top
        HBox left_bottom_top = new HBox();
        left_bottom_top.setId("left_bottom_top");
        Button expression = new Button("表情");
        left_bottom_top.getChildren().add(expression);

        // inputArea
        TextArea inputArea = new TextArea();
        inputArea.setWrapText(true);
        inputArea.requestFocus();
        DoubleBinding height = primaryStage.heightProperty().divide(4);
        inputArea.minHeightProperty().bind(height);
        inputArea.setPromptText("在这里输入消息");

        // left_bottom_bottom
        HBox left_bottom_bottom = new HBox();
        left_bottom_bottom.setId("left_bottom_bottom");
        Button close = new Button("关闭");
        Button send = new Button("发送");
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

        primaryStage.setTitle("p2pCommunity");
        Scene scene = new Scene(root, 800, 600);

//        ScenicView.show(scene);

        primaryStage.setScene(scene);
        primaryStage.getScene().getStylesheets().add("style.css");
        primaryStage.show();
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
        close.setOnAction(event -> {
            receiver.close();
            sender.close();
            System.exit(0);
        });
        send.setOnAction(event -> {
            String message = inputArea.getText();
            send(message.getBytes());
            left_center.getChildren().add(getMessagePane(name, message, new Image("file:res/user.png", 32, 32, true, true), Pos.CENTER_RIGHT));
        });
        datagramPacketObservableList.addListener((ListChangeListener<DatagramPacket>) c -> {
            if (c.next() && c.wasAdded()) {
                for (DatagramPacket datagramPacket : c.getAddedSubList()) {
                    Platform.runLater(() -> {
                        left_center.getChildren().add(new Text(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
                        left_center.getChildren().add(getMessagePane(datagramPacket.getSocketAddress().toString(), new String(datagramPacket.getData()), new Image("file:res/user.png", 32, 32, true, true), Pos.CENTER_LEFT));
                    });
                }
            }
        });
        hostSet.addListener((SetChangeListener<Host>) change -> {
            if (change.wasAdded()) {
                for (Host host : change.getSet()) {
                    Platform.runLater(() -> {
                        Text prompt = new Text(host.getName() + "加入聊天室！");
                        prompt.setId("prompt");
                        left_center.getChildren().add(prompt);
                        right_root.getChildren().add(getUserPane(host));
                    });
                }
            } else if (change.wasRemoved()) {
                Platform.runLater(() -> {
                    for (Host host : change.getSet()) {
                        for (Node node : right_root.getChildren()) {
                            if (host == node.getUserData()) {
                                right_root.getChildren().remove(node);
                                break;
                            }
                        }
                    }
                });
            }
        });
        primaryStage.setOnCloseRequest(event -> {
            receiver.close();
            sender.close();
            System.exit(0);
        });
        initSocket();
    }

    private void initSocket() {
        try {
            InetAddress ip = InetAddress.getByName(groupAddres);
            receiver = new MulticastSocket(Port);
            receiver.setLoopbackMode(false);
            receiver.joinGroup(ip);
            sender = new MulticastSocket();
            sender.setLoopbackMode(false);
            hostSet.add(new Host("吴贝贝", "127.0.0.1", 4444, GetImageStr("res/user.png")));
            hostSet.add(new Host("吴贝贝1", "127.0.0.1", 4444, GetImageStr("res/user.png")));
            new Thread(this::receive).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receive() {
        byte[] data = new byte[4096];
        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(data, data.length);
                //receive()是阻塞方法，等待其他人发来消息
                receiver.receive(packet);
                dealWithJson(new String(packet.getData()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            receiver.close();
        }
    }

    private void send(byte[] data) {
        try {
            InetAddress ip = InetAddress.getByName(groupAddres);
            DatagramPacket packet = new DatagramPacket(data, data.length, ip, Port);
            sender.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Pane getMessagePane(String name, String message, Image buddha, Pos value) {
        VBox nameText = new VBox();
        nameText.setAlignment(value);
        nameText.getChildren().add(new Text(name));
        Text messageText = new Text(message);
        messageText.setWrappingWidth(300);
        ImageView imageView = new ImageView(buddha);
        GridPane gridPane = new GridPane();
        gridPane.setAlignment(value);
        HBox root = new HBox();
        root.setAlignment(value);
        if (value == Pos.CENTER_RIGHT) {
            messageText.setTextAlignment(TextAlignment.RIGHT);
            gridPane.add(nameText, 0, 0, 3, 1);
            gridPane.add(messageText, 0, 1, 3, 1);
            gridPane.add(imageView, 3, 0, 1, 2);
        } else if (value == Pos.CENTER_LEFT) {
            messageText.setTextAlignment(TextAlignment.LEFT);
            gridPane.add(nameText, 1, 0, 3, 1);
            gridPane.add(messageText, 1, 1, 3, 1);
            gridPane.add(imageView, 0, 0, 1, 2);
        }
        gridPane.setHgap(10);
        root.setPadding(new Insets(5, 5, 5, 5));
        root.getChildren().add(gridPane);
        return root;
    }

    private Pane getUserPane(Host host) {
        GridPane gridPane = new GridPane();
        gridPane.setUserData(host);
        Text nameText = new Text(host.getName());
        nameText.setTextAlignment(TextAlignment.LEFT);
        ImageView imageView = new ImageView(new Image(GenerateImage(host.getImg()) , 32,32,true,true));
        gridPane.add(imageView, 0, 0,1, 1);
        gridPane.add(nameText, 1, 0, 3, 1);
        Tooltip tooltip = new Tooltip(host.getIp());
        Tooltip.install(gridPane, tooltip);
//        gridPane.setGridLinesVisible(true);
//        gridPane.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
        return gridPane;
    }

    private void dealWithJson(String jsonString) {
        JSONObject json = JSONObject.parseObject(jsonString);
        int code = json.getInteger("code");
        Host host;
        switch (code) {
            case ON_LINE:
                host = json.getObject("head", Host.class);
                hostSet.add(host);
                break;
            case OFF_LINE:
                host = json.getObject("head", Host.class);
                hostSet.remove(host);
                break;
            case UPDATE_USER_INFO:
                host = json.getObject("head", Host.class);
                for (Node node : right_root.getChildren()) {
                    if (((Host) node.getUserData()).getIp().equals(host.getIp()) && ((Host) node.getUserData()).getPort() == host.getPort()) {
                        GridPane gridPane = (GridPane) node;
                        Platform.runLater(() -> {
                            Text nameText = (Text) gridPane.getChildren().get(0);
                            nameText.setText(host.getName());
                            ImageView imageView = (ImageView) gridPane.getChildren().get(1);
                            imageView.setImage(new Image(GenerateImage(host.getImg()) , 32,32,true,true));
                        });
                    }
                }
                break;
            case GET_USER_LIST:
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("code", GET_USER_LIST);
                JSONObject head = new JSONObject();
                head.put("name", name);
                try {
                    InetAddress localhost = InetAddress.getLocalHost();
                    head.put("ip", localhost.getHostAddress());
                    head.put("port", Port);
                    head.put("img", GetImageStr(imgFile));
                }catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                jsonObject.put("head", head);
                send(jsonObject.toString().getBytes());
                break;
            case GROUP_SENDING:

                break;
            case PRIVATE_CHAT:

                break;
        }
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

    //图片转化成base64字符串
    private static String GetImageStr(String imgFile) {
        //将图片文件转化为字节数组字符串，并对其进行Base64编码处理
        InputStream in;
        byte[] data;
        //读取图片字节数组
        try {
            in = new FileInputStream(imgFile);
            data = new byte[in.available()];
            in.read(data);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        //对字节数组Base64编码
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(data);//返回Base64编码过的字节数组字符串
    }

    public static void main(String[] args) {
        launch(args);
    }

}
