package Pane;

import javafx.scene.text.FontSmoothingType;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.List;
import java.util.Objects;

public class RichTextPane extends HTMLEditor {
    private final static String fontfamily = "Microsoft YaHei UI";
    private final static String sourceText = "<html dir=\"ltr\"><head></head><body contenteditable=\"false\" ></body></html>";
    private WebEngine webEngine;
    private StringBuilder messageText = new StringBuilder();

    private final static String jsCodeInsertHtml = "function insertHtmlAtCursor(html) {\n" +
            "    var range, node;\n" +
            "    if (window.getSelection && window.getSelection().getRangeAt) {\n" +
            "        range = window.getSelection().getRangeAt(0);\n" +
            "        node = range.createContextualFragment(html);\n" +
            "        range.insertNode(node);\n" +
            "    } else if (document.selection && document.selection.createRange) {\n" +
            "        document.selection.createRange().pasteHTML(html);\n" +
            "    }\n" +
            "}insertHtmlAtCursor('####html####')";

    public RichTextPane() {
        super();
        this.setId("richTextPane");
        this.setHtmlText(sourceText);

        this.setStyle(
                "-fx-font: 12 cambria;"
        );
        ((WebView) this.lookup("WebView")).setFontSmoothingType(FontSmoothingType.LCD);
        webEngine = ((WebView) this.lookup("WebView")).getEngine();
    }

    // 在光标处添加img
    public void addImg(String file, String shortname) {
        String img = "<img src=\"" + file + "\" width=\"32\" height=\"32\" alt = '" + shortname + "'/>";
        webEngine.executeScript(jsCodeInsertHtml.replace("####html####", Objects.requireNonNull(escapeJavaStyleString(img, true, true))));
        System.out.println(this.getHtmlText());
    }
    // 代理方法
    public StringBuilder getStringText() {
        return getStringText(Jsoup.parse(this.getHtmlText()).getElementsByTag("body").last().childNodes());
    }

    // 取出html中的字符串
    private StringBuilder getStringText(List<Node> elements){
        for(Node element: elements) {
            if (element instanceof TextNode && !((TextNode) element).isBlank())
                    messageText.append(((TextNode) element).getWholeText());
            else if (((Element) element).tagName().equals("img"))
                messageText.append(element.attributes().get("alt"));
            if (element.childNodeSize() > 0)
                getStringText(element.childNodes());
        }
        return messageText;
    }

    // 清空文本框
    public void clearText(){
        this.setHtmlText(sourceText);
        messageText = new StringBuilder();
    }

    private static String escapeJavaStyleString(String str, boolean escapeSingleQuote, boolean escapeForwardSlash) {
        StringBuilder out = new StringBuilder("");
        if (str == null) {
            return null;
        }
        int sz;
        sz = str.length();
        for (int i = 0; i < sz; i++) {
            char ch = str.charAt(i);

            // handle unicode
            if (ch > 0xfff) {
                out.append("\\u").append(hex(ch));
            } else if (ch > 0xff) {
                out.append("\\u0").append(hex(ch));
            } else if (ch > 0x7f) {
                out.append("\\u00").append(hex(ch));
            } else if (ch < 32) {
                switch (ch) {
                    case '\b':
                        out.append('\\');
                        out.append('b');
                        break;
                    case '\n':
                        out.append('\\');
                        out.append('n');
                        break;
                    case '\t':
                        out.append('\\');
                        out.append('t');
                        break;
                    case '\f':
                        out.append('\\');
                        out.append('f');
                        break;
                    case '\r':
                        out.append('\\');
                        out.append('r');
                        break;
                    default:
                        if (ch > 0xf) {
                            out.append("\\u00").append(hex(ch));
                        } else {
                            out.append("\\u000").append(hex(ch));
                        }
                        break;
                }
            } else {
                switch (ch) {
                    case '\'':
                        if (escapeSingleQuote) {
                            out.append('\\');
                        }
                        out.append('\'');
                        break;
                    case '"':
                        out.append('\\');
                        out.append('"');
                        break;
                    case '\\':
                        out.append('\\');
                        out.append('\\');
                        break;
                    case '/':
                        if (escapeForwardSlash) {
                            out.append('\\');
                        }
                        out.append('/');
                        break;
                    default:
                        out.append(ch);
                        break;
                }
            }
        }
        return out.toString();
    }

    private static String hex(int i) {
        return Integer.toHexString(i);
    }
}