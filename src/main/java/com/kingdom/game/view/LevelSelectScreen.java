package com.kingdom.game.view;

import com.kingdom.game.controller.IGameStateReader;
import com.kingdom.game.controller.ILevelSwitcher;
import com.kingdom.game.controller.LevelInfo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;

/**
 * LevelSelectScreen —— 选关界面（关卡列表 → 进入所选关卡）。
 *
 * 数据与动作**只走接口**（《多关卡与运行期切图-接口规范》§3.5-1）：查询用
 * {@link IGameStateReader#getLevels()}，切关用 {@link ILevelSwitcher#switchLevelAt(int)}；
 * 不 import 具体 {@code GameController}，也不直接读 {@code GameConfig} / {@code MapLibrary}。
 *
 * 进入关卡的时序：先切关，成功才回调 {@code onEntered} 交回装配层切屏；切关失败时接口保证
 * **状态零变化**（无非法 key 的部分应用），故本界面停在原地提示、可安全重试。
 *
 * 关卡列表只在 {@link #refresh()} 时取一次——{@code getLevels()} 每次调用都会重新解析
 * {@code maps/index.json}（无缓存），不可每帧调用。
 */
public class LevelSelectScreen {

    private final IGameStateReader stateReader;
    private final ILevelSwitcher levelSwitcher;
    private final Runnable onEntered;

    private final StackPane root;
    private final VBox list = new VBox(12);
    private final Label hintLabel = new Label();

    public LevelSelectScreen(IGameStateReader stateReader, ILevelSwitcher levelSwitcher, Runnable onEntered) {
        this.stateReader = stateReader;
        this.levelSwitcher = levelSwitcher;
        this.onEntered = onEntered;

        Label title = new Label("选择关卡");
        title.setFont(Font.font(28));
        title.setTextFill(Color.web("#ffd700"));

        hintLabel.setFont(Font.font(13));
        hintLabel.setTextFill(Color.web("#ff6b5e"));
        hintLabel.setWrapText(true);
        hintLabel.setMaxWidth(360);

        list.setAlignment(Pos.CENTER);

        VBox card = new VBox(18, title, list, hintLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(32, 48, 32, 48));
        card.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);   // StackPane 里不被拉伸
        card.setStyle("-fx-background-color: rgba(30,34,40,0.96); -fx-background-radius: 12;"
                + "-fx-border-color: #9aa3ad; -fx-border-radius: 12;");

        root = new StackPane(card);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1b2026, #38414b);");
    }

    public Node getNode() { return root; }

    /** 打开选关界面时调用一次：按当前关卡列表重建按钮（当前关高亮标出） */
    public void refresh() {
        list.getChildren().clear();
        hintLabel.setText("");

        List<LevelInfo> levels;
        try {
            levels = stateReader.getLevels();
        } catch (RuntimeException ex) {
            // maps/index.json 语法非法时 MiniJson 抛 IllegalArgumentException，而
            // MapLibrary.listMapsFromClasspath 只兜 IOException → 这里必须自行兜底，避免崩窗口
            hintLabel.setText("关卡列表读取失败：" + ex.getMessage());
            return;
        }
        if (levels == null || levels.isEmpty()) {
            hintLabel.setText("未找到任何关卡（maps/index.json 缺失或为空）");
            return;
        }

        int current = stateReader.getCurrentLevelIndex();
        for (LevelInfo info : levels) {
            boolean isCurrent = info.getIndex() == current;
            Button b = new Button("第 " + (info.getIndex() + 1) + " 关 · " + info.getName()
                    + (isCurrent ? "（当前）" : ""));
            b.setFont(Font.font(16));
            b.setPrefWidth(300);
            b.setUserData(info);
            if (isCurrent) {
                b.setStyle("-fx-border-color: #ffd700; -fx-border-width: 2; -fx-border-radius: 4;");
            }
            b.setOnAction(e -> enter(info));
            list.getChildren().add(b);
        }
    }

    /** 进入所选关卡：切关成功 → 交回装配层切屏；失败 → 留在本界面并提示（失败时状态零变化） */
    private void enter(LevelInfo info) {
        if (levelSwitcher.switchLevelAt(info.getIndex())) {
            onEntered.run();
        } else {
            hintLabel.setText("关卡数据缺失，无法进入：" + info.getKey());
        }
    }
}
