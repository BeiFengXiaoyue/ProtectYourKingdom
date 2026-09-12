package com.kingdom.game.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * StartScreen —— 开始界面（标题 + 「开始游戏」/「退出游戏」）。
 *
 * 启动后的第一屏；「开始游戏」只回调注入的 {@code onStart}，不直接碰选关或关卡逻辑
 * （关卡列表与切关动作在 {@link LevelSelectScreen}，屏幕切换在装配层 Main）。
 * 本类只依赖 JavaFX，不持有任何控制器接口，故不涉及"UI 只经接口"的红线。
 *
 * 视觉沿用项目既有口径（无 CSS 文件、全部内联样式）：卡片底 {@code rgba(30,34,40,0.96)}、
 * 描边 {@code #9aa3ad}、主色 {@code #ffd700}、次要文字 {@code #bbbbbb}，与 GameView 结束遮罩一致。
 */
public class StartScreen {

    private final StackPane root;

    public StartScreen(Runnable onStart) {
        Label title = new Label("王国保卫战：前线哨站");
        title.setFont(Font.font(34));
        title.setTextFill(Color.web("#ffd700"));

        Label subtitle = new Label("点击「开始游戏」选择关卡");
        subtitle.setFont(Font.font(14));
        subtitle.setTextFill(Color.web("#bbbbbb"));

        Button startButton = new Button("开始游戏");
        startButton.setFont(Font.font(20));
        startButton.setPrefWidth(220);
        startButton.setOnAction(e -> onStart.run());

        Button quitButton = new Button("退出游戏");
        quitButton.setFont(Font.font(14));
        quitButton.setPrefWidth(220);
        quitButton.setOnAction(e -> Platform.exit());

        VBox card = new VBox(16, title, subtitle, startButton, quitButton);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36, 56, 36, 56));
        card.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);   // StackPane 里不被拉伸
        card.setStyle("-fx-background-color: rgba(30,34,40,0.96); -fx-background-radius: 12;"
                + "-fx-border-color: #9aa3ad; -fx-border-radius: 12;");

        root = new StackPane(card);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1b2026, #38414b);");
    }

    public Node getNode() { return root; }
}
