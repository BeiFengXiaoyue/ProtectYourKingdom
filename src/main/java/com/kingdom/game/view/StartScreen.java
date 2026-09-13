package com.kingdom.game.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * StartScreen —— 开始界面（木纹面板底 + 标题 + 「开始游戏」/「退出游戏」）。
 *
 * 启动后的第一屏；「开始游戏」只回调注入的 {@code onStart}，不直接碰选关或关卡逻辑
 * （关卡列表与切关动作在 {@link LevelSelectScreen}，屏幕切换在装配层 Main）。
 * 本类只依赖 JavaFX，不持有任何控制器接口，故不涉及"UI 只经接口"的红线。
 *
 * 视觉：底图 assets/ui_start_bg.png（KR 木纹面板，无 CSS 文件、全部内联样式）；
 * 缺图时回退为暗色渐变；标题带投影，主按钮金色渐变 + 悬停增亮。
 */
public class StartScreen {

    private final StackPane root;

    public StartScreen(Runnable onStart) {
        StackPane layer = new StackPane();

        // 底图：KR 木纹面板（缺图回退暗色渐变）
        Image bg = null;
        try (var in = StartScreen.class.getResourceAsStream("/assets/ui_start_bg.png")) {
            if (in != null) bg = new Image(in);
        } catch (Exception ignored) {
        }
        if (bg != null) {
            ImageView bgView = new ImageView(bg);
            // 铺满整个窗口（含顶部状态条区域），不留白边
            bgView.fitWidthProperty().bind(layer.widthProperty());
            bgView.fitHeightProperty().bind(layer.heightProperty());
            layer.getChildren().add(bgView);
            layer.setPrefSize(700, 665);   // 与游戏屏（画布+状态条）同高，防窗口上下露白
        } else {
            layer.setStyle("-fx-background-color: linear-gradient(to bottom, #1b2026, #38414b);");
            layer.setPrefSize(700, 665);
        }

        // 内容板：半透明深色圆角卡，保证文字在木纹上可读
        Label title = new Label("王国保卫战");
        title.setFont(Font.font("System", FontWeight.BOLD, 52));
        title.setTextFill(Color.web("#ffd76a"));
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#000000", 0.85));
        glow.setRadius(10);
        title.setEffect(glow);

        Label subtitle = new Label("—— 前 线 哨 站 ——");
        subtitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        subtitle.setTextFill(Color.web("#e8d9a0"));
        subtitle.setEffect(glow);

        Label hint = new Label("点击「开始游戏」选择关卡");
        hint.setFont(Font.font(13));
        hint.setTextFill(Color.web("#c9b98a"));

        Button startButton = new Button("开 始 游 戏");
        startButton.setFont(Font.font("System", FontWeight.BOLD, 22));
        startButton.setPrefSize(260, 56);
        startButton.setTextFill(Color.web("#4a3200"));
        String startStyle = "-fx-background-color: linear-gradient(to bottom, #ffe08a, #e0a800);"
                + "-fx-background-radius: 12; -fx-border-color: #8a6a00; -fx-border-radius: 12;"
                + "-fx-border-width: 2; -fx-cursor: hand;";
        String startHover = "-fx-background-color: linear-gradient(to bottom, #fff2b8, #ffc400);"
                + "-fx-background-radius: 12; -fx-border-color: #a8820a; -fx-border-radius: 12;"
                + "-fx-border-width: 2; -fx-cursor: hand;";
        startButton.setStyle(startStyle);
        startButton.setOnMouseEntered(e -> startButton.setStyle(startHover));
        startButton.setOnMouseExited(e -> startButton.setStyle(startStyle));
        startButton.setOnAction(e -> onStart.run());   // 进入选关界面

        Button quitButton = new Button("退 出 游 戏");
        quitButton.setFont(Font.font(15));
        quitButton.setPrefSize(260, 42);
        quitButton.setTextFill(Color.web("#e8e0c8"));
        String quitStyle = "-fx-background-color: rgba(40,32,20,0.85);"
                + "-fx-background-radius: 10; -fx-border-color: #a08a5a; -fx-border-radius: 10;"
                + "-fx-border-width: 1.5; -fx-cursor: hand;";
        String quitHover = "-fx-background-color: rgba(90,70,40,0.9);"
                + "-fx-background-radius: 10; -fx-border-color: #d9b380; -fx-border-radius: 10;"
                + "-fx-border-width: 1.5; -fx-cursor: hand;";
        quitButton.setStyle(quitStyle);
        quitButton.setOnMouseEntered(e -> quitButton.setStyle(quitHover));
        quitButton.setOnMouseExited(e -> quitButton.setStyle(quitStyle));
        quitButton.setOnAction(e -> Platform.exit());

        VBox card = new VBox(14, title, subtitle, hint, startButton, quitButton);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(34, 60, 34, 60));
        card.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        card.setStyle("-fx-background-color: rgba(16,14,10,0.72); -fx-background-radius: 18;"
                + "-fx-border-color: #d9b380; -fx-border-radius: 18; -fx-border-width: 2;");

        layer.getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);

        root = layer;
    }

    public Node getNode() { return root; }
}
