package com.kingdom.game.view;

import com.kingdom.game.controller.IGameStateReader;
import com.kingdom.game.controller.ILevelSwitcher;
import com.kingdom.game.controller.LevelInfo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LevelSelectScreen —— 选关界面（缩略图卡片，点击直接进入）。
 *
 * 数据与动作**只走接口**（《多关卡与运行期切图-接口规范》§3.5-1）：查询用
 * {@link IGameStateReader#getLevels()}，切关用 {@link ILevelSwitcher#switchLevelAt(int)}；
 * 不 import 具体 {@code GameController}，也不直接读 {@code GameConfig} / {@code MapLibrary}。
 *
 * 进入关卡的时序：先切关，成功才回调 {@code onEntered} 交回装配层切屏；切关失败时接口保证
 * **状态零变化**（无非法 key 的部分应用），故本界面停在原地提示、可安全重试。
 *
 * 视觉：与开始界面同底（KR 木纹面板）；每关一张卡片 = 地图缩略图 + 关名，
 * 点击卡片直接进关（无“先选中再确认”流程，故无“当前关”标记）。
 * 关卡列表只在 {@link #refresh()} 时取一次——index 解析不可每帧调用。
 */
public class LevelSelectScreen {

    private final IGameStateReader stateReader;
    private final ILevelSwitcher levelSwitcher;
    private final Runnable onEntered;

    private final StackPane root;
    private final TilePane cards = new TilePane();
    private final Label hintLabel = new Label();
    private final Map<String, Image> thumbCache = new HashMap<>();

    private static final String CARD_BASE = "-fx-background-color: rgba(24,20,14,0.82);"
            + "-fx-background-radius: 10; -fx-border-color: #a08a5a; -fx-border-radius: 10;"
            + "-fx-border-width: 2; -fx-cursor: hand;";
    private static final String CARD_HOVER = "-fx-background-color: rgba(60,48,26,0.9);"
            + "-fx-background-radius: 10; -fx-border-color: #ffd76a; -fx-border-radius: 10;"
            + "-fx-border-width: 2; -fx-cursor: hand;";

    public LevelSelectScreen(IGameStateReader stateReader, ILevelSwitcher levelSwitcher, Runnable onEntered) {
        this.stateReader = stateReader;
        this.levelSwitcher = levelSwitcher;
        this.onEntered = onEntered;

        Label title = new Label("选 择 关 卡");
        title.setFont(Font.font("System", FontWeight.BOLD, 34));
        title.setTextFill(Color.web("#ffd76a"));
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#000000", 0.8));
        shadow.setRadius(8);
        title.setEffect(shadow);

        hintLabel.setFont(Font.font(13));
        hintLabel.setTextFill(Color.web("#ff9c8a"));
        hintLabel.setWrapText(true);
        hintLabel.setMaxWidth(560);
        hintLabel.setVisible(false);
        hintLabel.setManaged(false);

        cards.setHgap(16);
        cards.setVgap(18);
        cards.setAlignment(Pos.CENTER);
        cards.setPrefTileWidth(150);
        cards.setPrefTileHeight(150);

        VBox plate = new VBox(22, title, cards, hintLabel);
        plate.setAlignment(Pos.CENTER);
        plate.setPadding(new Insets(26, 30, 26, 30));
        plate.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        plate.setStyle("-fx-background-color: rgba(16,14,10,0.72); -fx-background-radius: 18;"
                + "-fx-border-color: #d9b380; -fx-border-radius: 18; -fx-border-width: 2;");

        // 底图：与开始界面同款木纹面板（缺图回退暗色渐变）
        StackPane layer = new StackPane();
        Image bg = null;
        try (InputStream in = LevelSelectScreen.class.getResourceAsStream("/assets/ui_start_bg.png")) {
            if (in != null) bg = new Image(in);
        } catch (Exception ignored) {
        }
        if (bg != null) {
            ImageView bgView = new ImageView(bg);
            // 铺满整个窗口（含顶部状态条区域），不留白边
            bgView.fitWidthProperty().bind(layer.widthProperty());
            bgView.fitHeightProperty().bind(layer.heightProperty());
            layer.getChildren().add(bgView);
            layer.setPrefSize(700, 665);   // 与游戏屏同高，防窗口上下露白
        } else {
            layer.setStyle("-fx-background-color: linear-gradient(to bottom, #1b2026, #38414b);");
            layer.setPrefSize(700, 665);
        }
        layer.getChildren().add(plate);
        StackPane.setAlignment(plate, Pos.CENTER);

        root = layer;
    }

    public Node getNode() { return root; }

    /** 打开选关界面时调用一次：按当前关卡列表重建缩略图卡片 */
    public void refresh() {
        cards.getChildren().clear();
        hideHint();

        List<LevelInfo> levels;
        try {
            levels = stateReader.getLevels();
        } catch (RuntimeException ex) {
            // maps/index.json 语法非法时 MiniJson 抛 IllegalArgumentException，这里必须自行兜底，避免崩窗口
            showHint("关卡列表读取失败：" + ex.getMessage());
            return;
        }
        if (levels == null || levels.isEmpty()) {
            showHint("未找到任何关卡（maps/index.json 缺失或为空）");
            return;
        }

        for (LevelInfo info : levels) {
            cards.getChildren().add(levelCard(info));
        }
    }

    /** 单张关卡卡片：缩略图 + 关名；点击直接切关进入 */
    private Node levelCard(LevelInfo info) {
        ImageView thumb = new ImageView(thumbnail(info.getKey()));
        thumb.setFitWidth(126);
        thumb.setFitHeight(96);
        thumb.setPreserveRatio(false);
        thumb.setSmooth(true);
        thumb.setMouseTransparent(true);

        Label name = new Label("第 " + (info.getIndex() + 1) + " 关 · " + info.getName());
        name.setFont(Font.font("System", FontWeight.BOLD, 12));
        name.setTextFill(Color.web("#f0e6c8"));
        name.setMouseTransparent(true);

        VBox card = new VBox(6, thumb, name);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(8));
        card.setStyle(CARD_BASE);
        card.setOnMouseEntered(e -> card.setStyle(CARD_HOVER));
        card.setOnMouseExited(e -> card.setStyle(CARD_BASE));
        card.setOnMouseClicked(e -> enter(info));
        return card;
    }

    /** 关卡缩略图：读取该图 map.png（classpath），带缓存；缺失回退灰色占位图 */
    private Image thumbnail(String key) {
        Image cached = thumbCache.get(key);
        if (cached != null) return cached;
        Image img = null;
        try (InputStream in = LevelSelectScreen.class.getResourceAsStream("/maps/" + key + "/map.png")) {
            if (in != null) img = new Image(in);
        } catch (Exception ignored) {
        }
        if (img == null) img = placeholder();
        thumbCache.put(key, img);
        return img;
    }

    /** 灰色占位图（126×96） */
    private Image placeholder() {
        javafx.scene.image.WritableImage ph = new javafx.scene.image.WritableImage(126, 96);
        javafx.scene.canvas.Canvas c = new javafx.scene.canvas.Canvas(126, 96);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(Color.web("#4a4a4a"));
        g.fillRect(0, 0, 126, 96);
        g.setFill(Color.web("#777777"));
        g.fillText("No Image", 34, 52);
        javafx.scene.SnapshotParameters sp = new javafx.scene.SnapshotParameters();
        ph = c.snapshot(sp, ph);
        return ph;
    }

    /** 进入所选关卡：切关成功 → 交回装配层切屏；失败 → 留在本界面并提示（失败时状态零变化） */
    private void enter(LevelInfo info) {
        if (levelSwitcher.switchLevelAt(info.getIndex())) {
            onEntered.run();
        } else {
            showHint("关卡数据缺失，无法进入：" + info.getKey());
        }
    }

    private void showHint(String text) {
        hintLabel.setText(text);
        hintLabel.setVisible(true);
        hintLabel.setManaged(true);
    }

    private void hideHint() {
        hintLabel.setText("");
        hintLabel.setVisible(false);
        hintLabel.setManaged(false);
    }
}
