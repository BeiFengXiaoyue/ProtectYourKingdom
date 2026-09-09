package com.kingdom.game.view;

import com.kingdom.game.controller.IGameStateReader;
import com.kingdom.game.model.TowerSpec;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * TowerBuildMenu —— 点空闲点位弹出的锚定建塔目录（纯表现层，D 分工）。
 *
 * 数据驱动：目录条目按 IGameStateReader.getTowerSpecs() 动态生成，每个条目由
 * TowerSpec 描述（内含 TowerType / 显示名 / 造价）——登记方扩展 TowerType 枚举并
 * addTowerSpec 后，本弹窗自动跟随，代码无需改动（不写任何 TowerType 分支）。
 * 造价高于当前金币的条目在弹出时置灰。
 *
 * 本类只依赖 IGameStateReader 与选择回调，不 import GameController 具体类；
 * 落点/结算由调用方（GameView）经 ITowerBuilder 完成。
 */
public class TowerBuildMenu {

    private static final int SLOT_OFFSET = 26;   // 弹窗相对点位的偏移
    private static final int EDGE_MARGIN = 6;    // 贴画布边缘时的最小边距

    private final IGameStateReader stateReader;
    private final Consumer<TowerSpec> onSelect;

    private final VBox box = new VBox(6);
    private boolean visible = false;

    /** 最近一次 rebuild() 生成的塔按钮（供 refresh() 按最新金币重算置灰，不重建布局） */
    private final List<Button> towerButtons = new ArrayList<>();

    public TowerBuildMenu(IGameStateReader stateReader, Consumer<TowerSpec> onSelect) {
        this.stateReader = stateReader;
        this.onSelect = onSelect;
        box.setStyle("-fx-background-color: rgba(64,69,77,0.95); -fx-background-radius: 8;"
                + "-fx-padding: 8 10 8 10; -fx-border-color: #9aa3ad; -fx-border-radius: 8;");
        box.setManaged(false);   // 作为画布叠层绝对定位，不参与布局
        box.setVisible(false);
    }

    public Node getNode() { return box; }

    public boolean isVisible() { return visible; }

    /** 在点位附近显示目录；viewW/viewH 用于把弹窗 clamp 在画布内 */
    public void show(double slotX, double slotY, double viewW, double viewH) {
        rebuild();
        // 本弹窗以 setManaged(false) 做绝对定位：父容器不会帮它 resize，
        // 必须先 applyCss + autosize 让 VBox 拿到实际尺寸并布局子按钮，否则不可见。
        box.applyCss();
        box.autosize();
        positionNear(slotX, slotY, viewW, viewH);
        box.setVisible(true);
        visible = true;
    }

    public void hide() {
        box.setVisible(false);
        visible = false;
    }

    /** 弹窗打开期间由 GameView 每帧调用：按最新金币重算按钮置灰，不重建布局 */
    public void refresh() {
        if (!visible || towerButtons.isEmpty()) return;
        int gold = stateReader.getCurrentGold();
        for (Button b : towerButtons) {
            TowerSpec spec = (TowerSpec) b.getUserData();
            if (spec != null) b.setDisable(gold < spec.getCost());
        }
    }

    /** 按当前目录与金币重建条目（每次弹出时调用，保证目录/余额变化后即时生效） */
    private void rebuild() {
        box.getChildren().clear();
        towerButtons.clear();
        List<TowerSpec> specs = stateReader.getTowerSpecs();
        if (specs.isEmpty()) {
            Label tip = new Label("暂无可建造的塔");
            tip.setTextFill(Color.web("#ffd9a0"));
            box.getChildren().add(tip);
            return;
        }
        int gold = stateReader.getCurrentGold();
        for (TowerSpec spec : specs) {
            Button b = new Button(spec.getDisplayName() + " (" + spec.getCost() + ")");
            b.setDisable(gold < spec.getCost());
            b.setUserData(spec);                 // refresh() 据此重算置灰
            b.setOnAction(e -> {
                hide();
                onSelect.accept(spec);
            });
            towerButtons.add(b);
            box.getChildren().add(b);
        }
    }

    /** 优先放点位右下方；放不下则翻到左侧/贴底，确保整块在画布内 */
    private void positionNear(double slotX, double slotY, double viewW, double viewH) {
        double prefW = box.prefWidth(-1) + EDGE_MARGIN;
        double prefH = box.prefHeight(-1) + EDGE_MARGIN;
        double x = slotX + SLOT_OFFSET;
        if (x + prefW > viewW) x = slotX - SLOT_OFFSET - prefW;
        x = Math.max(EDGE_MARGIN, Math.min(x, viewW - prefW - EDGE_MARGIN));
        double y = slotY + SLOT_OFFSET;
        if (y + prefH > viewH) y = slotY - SLOT_OFFSET - prefH;
        y = Math.max(EDGE_MARGIN, Math.min(y, viewH - prefH - EDGE_MARGIN));
        box.setLayoutX(x);
        box.setLayoutY(y);
    }
}
