package Stage;

import Bean.Emoji;
import Bean.EmojiList;
import Bean.Host;
import Pane.EmojiPane;
import Pane.RichTextPane;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import static Pane.EmojiPane.getEmojiImagePath;

public class Main extends Application {
    private static final String groupAddres = "230.0.0.1";
    private static final int Port = 1234;
    private MulticastSocket receiver;
    private MulticastSocket sender;
    private ObservableList<Pair<Host, String>> DataObservableList = FXCollections.observableArrayList();
    private Map<Host, MyStage> privateChatStage = new HashMap<>();
    private Set<Host> hostSet = new HashSet<>();
    private ListView<Pane> right_root;
    private VBox left_center;
    private boolean flag = true;
    private RichTextPane inputArea = new RichTextPane();
    private ObservableList<Pane> paneObservableList = FXCollections.observableArrayList();

    // 个人信息
    private String name = "默认用户";
    private String imgFile = "res/default.png";
    private Pane Local_Pane;
    // 文件路径
    private File file = new File("res/config");


    private static final int ON_LINE = 1;           // 上线
    private static final int OFF_LINE = 2;         // 下线
    private static final int UPDATE_USER_INFO = 3; // 更新个人信息
    private static final int GET_USER_LIST = 4;    // 获取用户列表
    private static final int RETURNED_USER_LIST = 5; // 返回用户列表
    private static final int GROUP_SENDING = 6;    // 群聊
    private static final int PRIVATE_CHAT = 7;     // 私聊


    @Override
    public void start(Stage primaryStage) {
        Reading();
        BorderPane root = new BorderPane();
        // 菜单
        MenuBar menuBar = new MenuBar();
        Menu menuSetting = new Menu("设置");
        MenuItem menuName = new MenuItem("设置姓名");
        MenuItem menuImg = new MenuItem("设置头像");
        menuSetting.getItems().addAll(menuName, menuImg);
        menuBar.getMenus().addAll(menuSetting);
        root.setTop(menuBar);

        // right_root
        right_root = new ListView<>();
        right_root.setId("right_root");
        right_root.setItems(paneObservableList);
        root.setRight(right_root);

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
        DoubleBinding height = primaryStage.heightProperty().divide(4);
        inputArea.maxHeightProperty().bind(height);
        inputArea.minHeightProperty().bind(height);

        // left_bottom_bottom
        HBox left_bottom_bottom = new HBox();
        left_bottom_bottom.setId("left_bottom_bottom");
        Button close = new Button("关闭");
        Button send = new Button("发送");
        left_bottom_bottom.getChildren().addAll(close, send);

        // left_bottom
        VBox left_bottom = new VBox();
        left_bottom.setId("left_bottom");
        left_bottom.getChildren().addAll(left_bottom_top, inputArea, left_bottom_bottom);

        // left_root
        VBox left_root = new VBox();
        left_root.setFillWidth(true);
        left_root.getChildren().addAll(stackPane, left_bottom);
        VBox.setVgrow(stackPane, Priority.ALWAYS);
        root.setCenter(left_root);

        primaryStage.setTitle("当前登录用户名：" + name);
        Scene scene = new Scene(root, 800, 600);

        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image("file:res/chat.png"));
        primaryStage.getScene().getStylesheets().add("style.css");
        primaryStage.show();
        // event
        // 表情包
        expression.setOnAction(event -> emojiPane.setVisible(!emojiPane.isVisible()));
        // 改名
        menuName.setOnAction(event -> {
            TextInputDialog dialog = new TextInputDialog(name);
            dialog.setTitle("修改姓名");
            dialog.setHeaderText(null);
            dialog.setContentText("请输入您新的姓名:");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                if (!result.get().isEmpty()) {
                    if(hostSet.contains(new Host(result.get(), null, -1, null))){
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("错误");
                        alert.setHeaderText(null);
                        alert.setContentText("用户名重复");
                        alert.showAndWait();
                        return;
                    }
                    hostSet.remove(new Host(name, null, -1, null));
                    name = result.get();
                    ((Text) Local_Pane.getChildren().get(1)).setText(name);
                    send_update_user_info();
                    primaryStage.setTitle("当前登录用户名：" + name);
                }
            }
        });
        // 关闭聊天窗口
        close.setOnAction(event -> {
            send_off_line();
            receiver.close();
            sender.close();
            Saving();
            System.exit(0);
        });
        // 发送消息
        send.setOnAction(event -> {
            String String_message = inputArea.getStringText().toString();
            inputArea.clearText();
            if (String_message.length() > 0) {
                send_group_sending(String_message);
                left_center.getChildren().add(getMessagePane(name, String_message, new Image("file:" + imgFile, 32, 32, true, true), Pos.CENTER_RIGHT));
            }
            inputArea.requestFocus();
        });
        // 回车发送消息
        inputArea.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.ENTER) {
                send.fire();
                event.consume();
            }
        });
        // 修改头像
        menuImg.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("修改头像");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("PNG", "*.png"),
                    new FileChooser.ExtensionFilter("JPG", "*.jpg")
            );
            File file = fileChooser.showOpenDialog(primaryStage);
            while (file != null && file.length() > 10240) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("警告");
                alert.setHeaderText(null);
                alert.setContentText("文件需要小于10KB");
                alert.showAndWait();
                file = fileChooser.showOpenDialog(primaryStage);
            }
            if (file != null) {
                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(new Image("file:" + file.getAbsolutePath()), null), "png", new File("res/" + file.getName()));
                    imgFile = "res/" + file.getName();
                    send_update_user_info();
                    ((ImageView) Local_Pane.getChildren().get(0)).setImage(new Image("file:" + imgFile, 32, 32, true, true));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        // 消息记录
        DataObservableList.addListener((ListChangeListener<Pair<Host, String>>) c -> {
            if (c.next() && c.wasAdded()) {
                for (Pair<Host, String> pair : c.getAddedSubList()) {
                    Platform.runLater(() -> {
                        if (flag) {
                            left_center.getChildren().add(new Text(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
                            flag = false;
                        }
                        left_center.getChildren().add(getMessagePane(pair.getKey().getName(), pair.getValue(), new Image(GenerateImage(GetImageStr(pair.getKey())), 32, 32, true, true), Pos.CENTER_LEFT));
                    });
                }
            }
        });
        // 关闭窗口事件
        primaryStage.setOnCloseRequest(event -> {
            send_off_line();
            receiver.close();
            sender.close();
            Saving();
            System.exit(0);
        });

        //  窗口失去焦点
        primaryStage.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue && newValue)
                flag = true;
        });
        Init();
//        ScenicView.show(scene);
    }

    private void Init() {
        try {
            InetAddress ip = InetAddress.getByName(groupAddres);
            receiver = new MulticastSocket(Port);
            receiver.setLoopbackMode(true);
            receiver.joinGroup(ip);
            receiver.setTimeToLive(128);
            sender = new MulticastSocket();
            sender.setLoopbackMode(true);
            sender.joinGroup(ip);
            sender.setTimeToLive(128);
            new Thread(this::receive).start();
            Host host = new Host(name, InetAddress.getLocalHost().getHostAddress(), Port, GetImageStr(imgFile));
//            Host host1 = new Host("2", InetAddress.getLocalHost().getHostAddress(), 6666, GetImageStr(imgFile));
//            Host host2 = new Host("3", InetAddress.getLocalHost().getHostAddress(), 6668, GetImageStr(imgFile));
//            Host host3 = new Host("4", InetAddress.getLocalHost().getHostAddress(), 6636, GetImageStr(imgFile));
//            Host host4 = new Host("5", InetAddress.getLocalHost().getHostAddress(), 6676, GetImageStr(imgFile));
//            Host host5 = new Host("6", InetAddress.getLocalHost().getHostAddress(), 6606, GetImageStr(imgFile));
//            Host host6 = new Host("7", InetAddress.getLocalHost().getHostAddress(), 6616, GetImageStr(imgFile));
//            Host host7 = new Host("8", InetAddress.getLocalHost().getHostAddress(), 6626, GetImageStr(imgFile));
//            Host host8 = new Host("9", InetAddress.getLocalHost().getHostAddress(), 6186, GetImageStr(imgFile));
//            Host host9 = new Host("10", InetAddress.getLocalHost().getHostAddress(), 6996, GetImageStr(imgFile));
//            Host host10 = new Host("11", InetAddress.getLocalHost().getHostAddress(), 6466, GetImageStr(imgFile));
//            Host host11 = new Host("12", InetAddress.getLocalHost().getHostAddress(), 7866, GetImageStr(imgFile));
            hostSet.add(host);
//            hostSet.add(host1);
//            hostSet.add(host2);
//            hostSet.add(host3);
//            hostSet.add(host4);
//            hostSet.add(host5);
//            hostSet.add(host6);
//            hostSet.add(host7);
//            hostSet.add(host8);
//            hostSet.add(host9);
//            hostSet.add(host10);
//            hostSet.add(host11);
            Local_Pane = getUserPane(host);
            Platform.runLater(() -> paneObservableList.add(Local_Pane));
//            Platform.runLater(() -> paneObservableList.add(getUserPane(host1)));
//            Platform.runLater(() -> paneObservableList.add(getUserPane(host2)));
//            Platform.runLater(() -> paneObservableList.add(getUserPane(host3)));
//            Platform.runLater(() -> paneObservableList.add(getUserPane(host4)));
//            Platform.runLater(() -> paneObservableList.add(getUserPane(host5)));
//            Platform.runLater(() -> paneObservableList.add(getUserPane(host6)));
//            Platform.runLater(() -> paneObservableList.add(getUserPane(host7)));
//            Platform.runLater(() -> paneObservableList.add(getUserPane(host8)));
//            Platform.runLater(() -> paneObservableList.add(getUserPane(host9)));
//            Platform.runLater(() -> paneObservableList.add(getUserPane(host10)));
//            Platform.runLater(() -> paneObservableList.add(getUserPane(host11)));
            send_get_user_list();
            send_on_line();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receive() {
        byte[] data = new byte[65536];
        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(data, data.length);
                //receive()是阻塞方法，等待其他人发来消息
                data = new byte[65536];
                receiver.receive(packet);
                System.out.println("receive " + new String(packet.getData()));
                try {
                    dealWithJson(new String(packet.getData()));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            receiver.close();
        }
    }

    private void send(byte[] data) {
        if (data.length > 65536) {
            System.err.println("数据包长度过长");
            return;
        }
        try {
            InetAddress ip = InetAddress.getByName(groupAddres);
            DatagramPacket packet = new DatagramPacket(data, data.length, ip, Port);
            System.out.println("send " + new String(packet.getData()));
            sender.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Pane getMessagePane(String name, String message, Image buddha, Pos value) {
        VBox nameText = new VBox();
        nameText.setAlignment(value);
        nameText.getChildren().add(new Text(name));
        TextFlow messageTextFlow = getTextFlow(message);
        ImageView imageView = new ImageView(buddha);
        GridPane gridPane = new GridPane();
        gridPane.setAlignment(value);
        HBox root = new HBox();
        root.setAlignment(value);
        if (value == Pos.CENTER_RIGHT) {
            messageTextFlow.setTextAlignment(TextAlignment.RIGHT);
            gridPane.add(nameText, 0, 0, 3, 1);
            gridPane.add(messageTextFlow, 0, 1, 3, 1);
            gridPane.add(imageView, 3, 0, 1, 1);
        } else if (value == Pos.CENTER_LEFT) {
            messageTextFlow.setTextAlignment(TextAlignment.LEFT);
//            gridPane.add(nameText, 1, 0, 3, 1);
//            gridPane.add(messageTextFlow, 1, 1, 3, 1);
//            gridPane.add(imageView, 0, 0, 1, 2);
            gridPane.add(nameText, 1, 0, 3, 1);
            gridPane.add(messageTextFlow, 1, 1, 3, 1);
            gridPane.add(imageView, 0, 0, 1, 1);
        }
        gridPane.setHgap(10);
//        gridPane.setGridLinesVisible(true);
//        gridPane.setBackground(new Background(new BackgroundFill(Color.GREY, null, null)));
        root.setPadding(new Insets(5, 5, 5, 5));
        root.getChildren().add(gridPane);
        return root;
    }

    private Pane getUserPane(Host host) {
        GridPane gridPane = new GridPane();
        gridPane.setUserData(host);
        Text nameText = new Text(host.getName());
        nameText.setTextAlignment(TextAlignment.LEFT);
        ImageView imageView = new ImageView(new Image(GenerateImage(host.getImg()), 32, 32, true, true));
        gridPane.add(imageView, 0, 0, 1, 1);
        gridPane.add(nameText, 1, 0, 3, 1);
        Tooltip tooltip = new Tooltip(host.getIp());
        Tooltip.install(gridPane, tooltip);
//        gridPane.setGridLinesVisible(true);
//        gridPane.setBackground(new Background(new BackgroundFill(Color.GREY, null, null)));
        gridPane.setHgap(10);
        return gridPane;
    }

    //构建发送的JSON
    private JSONObject constructLocalJson(String str_data, int code, Host target) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", code);
        JSONObject head = new JSONObject();
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            head.put("name", name);
            head.put("ip", localhost.getHostAddress());
            head.put("port", Port);
            if (code == UPDATE_USER_INFO || code == ON_LINE || code == RETURNED_USER_LIST)
                head.put("img", GetImageStr(imgFile));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        jsonObject.put("head", head);
        if (str_data != null)
            jsonObject.put("data", str_data);
        if (target != null)
            jsonObject.put("target", new Host(target.getName(), target.getIp(), target.getPort(), null));
        return jsonObject;
    }

    // 上线消息
    private void send_on_line() {
        send(constructLocalJson(null, ON_LINE, null).toString().getBytes());
    }

    // 下线消息
    private void send_off_line() {
        send(constructLocalJson(null, OFF_LINE, null).toString().getBytes());
    }

    // 更新用户消息
    private void send_update_user_info() {
        send(constructLocalJson(null, UPDATE_USER_INFO, null).toString().getBytes());
    }

    // 获取用户列表
    private void send_get_user_list() {
        send(constructLocalJson(null, GET_USER_LIST, null).toString().getBytes());
    }

    // 群聊
    private void send_group_sending(String str_data) {
        send(constructLocalJson(str_data, GROUP_SENDING, null).toString().getBytes());
    }

    // 私聊
    private void send_private_chat(String str_data, Host target) {
        send(constructLocalJson(str_data, PRIVATE_CHAT, target).toString().getBytes());
    }

    //处理接收消息
    private void dealWithJson(String jsonString) throws UnknownHostException {
        JSONObject json = JSON.parseObject(jsonString);
        int code = json.getInteger("code");
        Host host;
        String data;
        switch (code) {
            case ON_LINE:
                host = json.getObject("head", Host.class);
                if(hostSet.contains(host))
                    return;
                Platform.runLater(() -> {
                    MyStage myStage;
                    Text prompt = new Text(host.getName() + " 上线了！");
                    prompt.setId("prompt");
                    left_center.getChildren().add(prompt);
                    Pane userPane = getUserPane(host);
                    userPane.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                        if (event.getClickCount() == 2) {
                            MyStage myStage1 = privateChatStage.get(host);
                            if (!myStage1.isShowing())
                                myStage1.show();
                            else
                                myStage1.requestFocus();
                        }
                    });
                    paneObservableList.add(userPane);
                    if (!privateChatStage.containsKey(host)) {
                        myStage = new MyStage();
                        Button button = myStage.getSend();
                        RichTextPane textArea = myStage.getRichTextPane();
                        VBox left_center = myStage.getLeft_center();
                        button.setOnAction(event -> {
                            String message = textArea.getStringText().toString();
                            textArea.clearText();
                            send_private_chat(message, host);
                            left_center.getChildren().add(getMessagePane(name, message, new Image("file:" + imgFile, 32, 32, true, true), Pos.CENTER_RIGHT));
                            textArea.requestFocus();
                        });
                        myStage.setTitle("正在与 " + host.getName() + " 私聊");
                        myStage.getIcons().add(new Image(GenerateImage(host.getImg()), 32, 32, true, true));
                        privateChatStage.put(host, myStage);
                    }
                });
                hostSet.add(host);
                break;
            case OFF_LINE:
                host = json.getObject("head", Host.class);
                Platform.runLater(() -> {
                    for (Pane pane : paneObservableList) {
                        Host lhost = (Host) pane.getUserData();
                        if (host.equals(lhost)) {
                            paneObservableList.remove(pane);
                            hostSet.remove(host);
                            privateChatStage.remove(host);
                            left_center.getChildren().add(new Text(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
                            Text prompt = new Text(host.getName() + " 下线了！");
                            prompt.setId("prompt");
                            left_center.getChildren().add(prompt);
                            break;
                        }
                    }
                });
                break;
            case UPDATE_USER_INFO:
                host = json.getObject("head", Host.class);
                for (Pane pane : paneObservableList) {
                    Host host1 = (Host) pane.getUserData();
                    if (host1.getIp().equals(host.getIp()) && host1.getPort() == host.getPort()) {
                        GridPane gridPane = (GridPane) pane;
                        Platform.runLater(() -> {
                            Text nameText = (Text) gridPane.getChildren().get(1);
                            nameText.setText(host.getName());
                            ImageView imageView = (ImageView) gridPane.getChildren().get(0);
                            imageView.setImage(new Image(GenerateImage(host.getImg()), 32, 32, true, true));
                            hostSet.remove(host1);
                            gridPane.setUserData(host);
                            hostSet.add(host);
                        });
                    }
                }
                break;
            case GET_USER_LIST:
                send(constructLocalJson(null, RETURNED_USER_LIST, null).toString().getBytes());
                break;
            case RETURNED_USER_LIST:
                host = json.getObject("head", Host.class);
                if(hostSet.contains(host))
                    return;
                Platform.runLater(() -> {
                    MyStage myStage;
                    Pane userPane = getUserPane(host);
                    userPane.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                        if (event.getClickCount() == 2) {
                            MyStage myStage1 = privateChatStage.get(host);
                            if (!myStage1.isShowing())
                                myStage1.show();
                            else
                                myStage1.requestFocus();
                        }
                    });
                    paneObservableList.add(userPane);
                    if (!privateChatStage.containsKey(host)) {
                        myStage = new MyStage();
                        Button button = myStage.getSend();
                        RichTextPane textArea = myStage.getRichTextPane();
                        VBox left_center = myStage.getLeft_center();
                        button.setOnAction(event -> {
                            String message = textArea.getStringText().toString();
                            textArea.clearText();
                            send_private_chat(message, host);
                            left_center.getChildren().add(getMessagePane(name, message, new Image("file:" + imgFile, 32, 32, true, true), Pos.CENTER_RIGHT));
                            textArea.requestFocus();
                        });
                        myStage.setTitle("正在与 " + host.getName() + " 私聊");
                        myStage.getIcons().add(new Image(GenerateImage(host.getImg()), 32, 32, true, true));
                        privateChatStage.put(host, myStage);
                    }
                });
                hostSet.add(host);
                break;
            case GROUP_SENDING:
                host = json.getObject("head", Host.class);
                data = json.getString("data");
                DataObservableList.add(new Pair<>(host, data));
                break;
            case PRIVATE_CHAT:
                // 如果私聊目标不是自己，则丢弃这个消息
                Host target = json.getObject("target", Host.class);
                if ((!target.getIp().equals(InetAddress.getLocalHost().getHostAddress())) || target.getPort() != Port)
                    break;
                host = json.getObject("head", Host.class);
                // 创建私聊窗口
                MyStage myStage;
                // 获取私聊消息
                data = json.getString("data");
                myStage = privateChatStage.get(host);
                if (myStage != null) {
                    Platform.runLater(() -> {
                        myStage.setTitle("正在与 " + host.getName() + " 私聊");
                        if (data != null) {
                            if (myStage.isFlag()) {
                                myStage.getLeft_center().getChildren().add(new Text(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
                                myStage.setFlag(false);
                            }
                            myStage.getLeft_center().getChildren().add(getMessagePane(host.getName(), data, new Image(GenerateImage(GetImageStr(host)), 32, 32, true, true), Pos.CENTER_LEFT));
                        }
                        myStage.show();
                    });
                }
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

    // 保存用户信息
    private void Saving() {
        try {
            JSONObject jsonObject = new JSONObject();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8));
            // 添加用户配置保存
            jsonObject.put("name", name);
            jsonObject.put("imgFile", imgFile);
            writer.write(jsonObject.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 读取用户信息
    private void Reading() {
        try {
            //读取配置信息
            StringBuilder jsonString = new StringBuilder();
            if (file.exists()) {
                String line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
                while ((line = reader.readLine()) != null)
                    jsonString.append(line);
                reader.close();
            } else {
                return;
            }
            JSONObject jsonObject = JSON.parseObject(jsonString.toString());
            name = jsonObject.getString("name");
            imgFile = jsonObject.getString("imgFile");
            if (!(new File(imgFile).exists()))
                imgFile = "res/default.png";
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 通过Host解析图片信息
    private String GetImageStr(Host host) {
        for (Host localHost : hostSet)
            if (localHost.equals(host))
                return localHost.getImg();
        System.out.println("未找到用户");
        return null;
    }

    //得到textflow
    private TextFlow getTextFlow(String message){
        Queue<Object> queue = EmojiList.getInstance().toEmojiAndText(message);
        Object o;
        TextFlow textFlow = new TextFlow();
        textFlow.setLineSpacing(0);
        while((o = queue.poll()) != null){
            if(o instanceof String) {
                Text text = new Text((String) o);
                text.setLineSpacing(0);
                text.setTextAlignment(TextAlignment.CENTER);
                textFlow.getChildren().add(text);
            }
            else {
                Emoji emoji = (Emoji) o;
                textFlow.getChildren().add(new ImageView(new Image(getEmojiImagePath(emoji.getUnicode()), 20,20,true,true)));
            }
        }
        return textFlow;
    }

    // 启动线程
    public static void main(String[] args) {
        launch(args);
    }
}
