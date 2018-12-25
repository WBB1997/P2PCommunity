package Pane;

import Bean.Emoji;
import Bean.EmojiList;
import Util.ImageCache;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EmojiPane extends AnchorPane {
    private List<String> list = new ArrayList<>(Arrays.asList(
            "Frequently Used",
            "People",
            "Nature",
            "Food",
            "Activity",
            "Travel",
            "Objects",
            "Symbols",
            "Flags"
            ));
    private ScrollPane searchScrollPane;
    private FlowPane searchFlowPane;
    private TabPane tabPane;
    private TextField txtSearch;
    private ComboBox<Image> boxTone;

    private RichTextPane input;


    public EmojiPane(RichTextPane input){
        //emoji点击事件
        this.input = input;
        VBox root = new VBox();
        // top
        HBox top = new HBox();
        top.setSpacing(5.0);
        txtSearch = new TextField();
        txtSearch.setFocusTraversable(false);
        txtSearch.setPromptText("查找表情");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        boxTone = new ComboBox();
        top.getChildren().addAll(txtSearch, boxTone);
        // bottom
        AnchorPane bottom = new AnchorPane();
        VBox.setVgrow(bottom, Priority.ALWAYS);
        tabPane = new TabPane();
        tabPane.setPrefSize(200, 200);
        tabPane.setSide(Side.BOTTOM);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        AnchorPane.setBottomAnchor(tabPane, 0.0);
        AnchorPane.setTopAnchor(tabPane, 0.0);
        AnchorPane.setLeftAnchor(tabPane, 0.0);
        AnchorPane.setRightAnchor(tabPane, 0.0);
        for(String attr : list){
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setContent(new FlowPane());
            Tab tab = new Tab();
            tab.setContent(scrollPane);
            tab.setText(attr);
            tabPane.getTabs().add(tab);
        }
        searchScrollPane = new ScrollPane();
        searchScrollPane.setVisible(false);
        AnchorPane.setBottomAnchor(searchScrollPane, 0.0);
        AnchorPane.setTopAnchor(searchScrollPane, 0.0);
        AnchorPane.setLeftAnchor(searchScrollPane, 0.0);
        AnchorPane.setRightAnchor(searchScrollPane, 0.0);
        searchFlowPane = new FlowPane();
        searchScrollPane.setContent(searchFlowPane);
        bottom.getChildren().addAll(tabPane, searchScrollPane);
        root.getChildren().addAll(top,bottom);
        root.setMaxSize(392, 300);
        root.setMinSize(392, 300);
        AnchorPane.setBottomAnchor(root, 0.0);
        AnchorPane.setLeftAnchor(root, 0.0);
        this.getChildren().add(root);
        init();
    }

    void init() {
        ObservableList<Image> tonesList = FXCollections.observableArrayList();

        for(int i = 1; i <= 5; i++) {
            Emoji emoji = EmojiList.getInstance().getEmoji(":thumbsup_tone"+i+":");
            Image image = ImageCache.getInstance().getImage(getEmojiImagePath(emoji.getUnicode()));
            tonesList.add(image);
        }
        Emoji em = EmojiList.getInstance().getEmoji(":thumbsup:"); //default tone
        Image image = ImageCache.getInstance().getImage(getEmojiImagePath(em.getUnicode()));
        tonesList.add(image);
        boxTone.setItems(tonesList);
        boxTone.setCellFactory(e->new ToneCell());
        boxTone.setButtonCell(new ToneCell());
        boxTone.getSelectionModel().selectedItemProperty().addListener(e->refreshTabs());


        searchScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        searchFlowPane.prefWidthProperty().bind(searchScrollPane.widthProperty().subtract(5));
        searchFlowPane.setHgap(5);
        searchFlowPane.setVgap(5);

        txtSearch.textProperty().addListener(x-> {
            String text = txtSearch.getText();
            if(text.isEmpty() || text.length() < 2) {
                searchFlowPane.getChildren().clear();
                searchScrollPane.setVisible(false);
            } else {
                searchScrollPane.setVisible(true);
                List<Emoji> results = EmojiList.getInstance().search(text);
                searchFlowPane.getChildren().clear();
                results.forEach(emoji ->searchFlowPane.getChildren().add(createEmojiNode(emoji)));
            }
        });


        for(Tab tab : tabPane.getTabs()) {
            ScrollPane scrollPane = (ScrollPane) tab.getContent();
            FlowPane pane = (FlowPane) scrollPane.getContent();
            pane.setPadding(new Insets(5));
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            pane.prefWidthProperty().bind(scrollPane.widthProperty().subtract(5));
            pane.setHgap(5);
            pane.setVgap(5);

            tab.setId(tab.getText());
            ImageView icon = new ImageView();
            icon.setFitWidth(20);
            icon.setFitHeight(20);
            switch (tab.getText().toLowerCase()) {
                case "frequently used":
                    icon.setImage(ImageCache.getInstance().getImage(getEmojiImagePath(EmojiList.getInstance().getEmoji(":heart:").getUnicode())));
                    break;
                case "people":
                    icon.setImage(ImageCache.getInstance().getImage(getEmojiImagePath(EmojiList.getInstance().getEmoji(":smiley:").getUnicode())));
                    break;
                case "nature":
                    icon.setImage(ImageCache.getInstance().getImage(getEmojiImagePath(EmojiList.getInstance().getEmoji(":dog:").getUnicode())));
                    break;
                case "food":
                    icon.setImage(ImageCache.getInstance().getImage(getEmojiImagePath(EmojiList.getInstance().getEmoji(":apple:").getUnicode())));
                    break;
                case "activity":
                    icon.setImage(ImageCache.getInstance().getImage(getEmojiImagePath(EmojiList.getInstance().getEmoji(":soccer:").getUnicode())));
                    break;
                case "travel":
                    icon.setImage(ImageCache.getInstance().getImage(getEmojiImagePath(EmojiList.getInstance().getEmoji(":airplane:").getUnicode())));
                    break;
                case "objects":
                    icon.setImage(ImageCache.getInstance().getImage(getEmojiImagePath(EmojiList.getInstance().getEmoji(":bulb:").getUnicode())));
                    break;
                case "symbols":
                    icon.setImage(ImageCache.getInstance().getImage(getEmojiImagePath(EmojiList.getInstance().getEmoji(":atom:").getUnicode())));
                    break;
                case "flags":
                    icon.setImage(ImageCache.getInstance().getImage(getEmojiImagePath(EmojiList.getInstance().getEmoji(":flag_eg:").getUnicode())));
                    break;
            }

            if(icon.getImage() != null) {
                tab.setText("");
                tab.setGraphic(icon);
            }

            tab.setTooltip(new Tooltip(tab.getId()));
            tab.selectedProperty().addListener(ee-> {
                if(tab.getGraphic() == null) return;
                if(tab.isSelected()) {
                    tab.setText(tab.getId());
                } else {
                    tab.setText("");
                }
            });
        }

        boxTone.getSelectionModel().select(0);
        tabPane.getSelectionModel().select(1);
    }

    private void refreshTabs() {
        Map<String, List<Emoji>> map = EmojiList.getInstance().getCategorizedEmojis(boxTone.getSelectionModel().getSelectedIndex()+1);
        for(Tab tab : tabPane.getTabs()) {
            ScrollPane scrollPane = (ScrollPane) tab.getContent();
            FlowPane pane = (FlowPane) scrollPane.getContent();
            pane.getChildren().clear();
            String category = tab.getId().toLowerCase();
            if(map.get(category) == null) continue;
            map.get(category).forEach(emoji -> pane.getChildren().add(createEmojiNode(emoji)));
        }
    }

    private Node createEmojiNode(Emoji emoji) {
        StackPane stackPane = new StackPane();
        stackPane.setMaxSize(32, 32);
        stackPane.setPrefSize(32, 32);
        stackPane.setMinSize(32, 32);
        stackPane.setPadding(new Insets(3));
        ImageView imageView = new ImageView();
        imageView.setFitWidth(32);
        imageView.setFitHeight(32);
        imageView.setImage(ImageCache.getInstance().getImage(getEmojiImagePath(emoji.getUnicode())));
        stackPane.getChildren().add(imageView);

        Tooltip tooltip = new Tooltip(emoji.getShortname());
        Tooltip.install(stackPane, tooltip);
        stackPane.setCursor(Cursor.HAND);
        ScaleTransition st = new ScaleTransition(Duration.millis(90), imageView);

        stackPane.setOnMouseEntered(e-> {
            imageView.setEffect(new DropShadow());
            st.setToX(1.2);
            st.setToY(1.2);
            st.playFromStart();
            if(txtSearch.getText().isEmpty())
                txtSearch.setPromptText(emoji.getShortname());
        });
        stackPane.setOnMouseExited(e-> {
            imageView.setEffect(null);
            st.setToX(1.);
            st.setToY(1.);
            st.playFromStart();
        });
        stackPane.setOnMouseClicked(event -> {
            input.addImg(getEmojiImagePath(emoji.getUnicode()), emoji.getShortname());
            EmojiPane.this.setVisible(false);
        });
        return stackPane;
    }

    public static String getEmojiImagePath(String inner_path) {
        File file = new File("./res/png_40/" + inner_path + ".png");
        String path = file.getAbsolutePath();
        return "file:" + path;
    }

    class ToneCell extends ListCell<Image> {
        private final ImageView imageView;
        ToneCell() {
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            imageView = new ImageView();
            imageView.setFitWidth(20);
            imageView.setFitHeight(20);
        }
        @Override
        protected void updateItem(Image item, boolean empty) {
            super.updateItem(item, empty);

            if(item == null || empty) {
                setText(null);
                setGraphic(null);
            } else {
                imageView.setImage(item);
                setGraphic(imageView);
            }
        }
    }
}
