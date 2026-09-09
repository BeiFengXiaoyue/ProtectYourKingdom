package com.kingdom.game.view;

import com.kingdom.game.controller.IGameStateReader;
import com.kingdom.game.controller.ITowerBuilder;
import com.kingdom.game.controller.ITowerSelectionNotifier;
import com.kingdom.game.model.tower.Tower;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * TowerDetailPanel —— 选中塔的锚定详情面板（D 分工，实现 ITowerSelectionNotifier）。
 *
 * 交互（由 GameView 驱动）：点已占用塔位 → onTowerSelected(tower) → 面板锚定塔旁展示
 * 攻击/射程/出售返还；「出售」经 ITowerBuilder.sellTower 结算（后端负责退款/移除）；
 * 「升级」当前置灰（仅文字「升级」，无说明）——换类升级机制（ITowerUpgrade）属 A 的交付，
 * 本类不 import 尚未存在的接口，待其落地后在 upgradeEntry 单点点亮。
 *
 * 本类只依赖 IGameStateReader / ITowerBuilder / 接口定义，不 import GameController 具体类。
 */
public class TowerDetailPanel implements ITowerSelectionNotifier {

    private static final int TOWER_OFFSET = 30;   // 面板相对选中塔的偏移
    private static final int EDGE_MARGIN = 8;     // 贴画布边缘时的最小边距

    private final IGameStateReader stateReader;
    private final ITowerBuilder builder;
    private final double viewW;
    private final double viewH;

    private final VBox box = new VBox(8);
    private final Label titleLabel = new Label("已选塔");
    private final Label infoLabel = new Label();
    private final Button upgradeButton = new Button("升级");
    private final Button sellButton = new Button("出售");

    private Tower current;
    private boolean visible = false;

    public TowerDetailPanel(IGameStateReader stateReader, ITowerBuilder builder,
                            double viewW, double viewH) {
        this.stateReader = stateReader;
        this.builder = builder;
        this.viewW = viewW;
        this.viewH = viewH;

        box.setStyle("-fx-background-color: rgba(40,45,52,0.95); -fx-background-radius: 8;"
                + "-fx-padding: 10 12 10 12; -fx-border-color: #9aa3ad; -fx-border-radius: 8;");
        box.setManaged(false);   // 作为画布叠层绝对定位，不参与布局
        box.setVisible(false);

        titleLabel.setFont(Font.font(14));
        titleLabel.setTextFill(Color.WHITE);
        infoLabel.setFont(Font.font(13));
        infoLabel.setTextFill(Color.web("#dddddd"));

        upgradeButton.setDisable(true);   // 置灰：换类升级机制（A 的 ITowerUpgrade）落地后由 upgradeEntry 点亮
        sellButton.setOnAction(e -> {
            if (current == null) return;
            builder.sellTower(current);
            onTowerDeselected();          // 出售后自隐藏（GameView 的高亮由"选中塔已不在列表"自检清除）
        });

        HBox buttons = new HBox(10, upgradeButton, sellButton);
        buttons.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().addAll(titleLabel, infoLabel, buttons);
    }

    public Node getNode() { return box; }

    public boolean isVisible() { return visible; }

    /** ITowerSelectionNotifier：选中塔 → 填充并锚定显示在塔旁 */
    @Override
    public void onTowerSelected(Tower tower) {
        if (tower == null) return;
        current = tower;
        infoLabel.setText("攻击 " + tower.getBaseAttackDamage()
                + "\n射程 " + Math.round(tower.getAttackRange())
                + "\n出售返还 " + (tower.getTotalCost() / 2));
        refresh();
        positionNear(tower.getX(), tower.getY());
        box.setVisible(true);
        visible = true;
    }

    /** ITowerSelectionNotifier：取消选中 → 隐藏清空 */
    @Override
    public void onTowerDeselected() {
        current = null;
        box.setVisible(false);
        visible = false;
    }

    /** 面板可见时由 GameView 每帧调用，保持按钮状态一致（当前仅升级入口判定） */
    public void refresh() {
        if (current == null) return;
        upgradeEntry(current);
        sellButton.setDisable(false);
    }

    /**
     * 升级入口（单点切换）：
     * 当前无"下一级"数据 → 按钮恒置灰；文字仅「升级」、无说明。
     * TODO 等 A 交付 model.tower.ITowerUpgrade 后，此处改为经
     * getNextLevelSpec()/isMaxLevel() 点亮——可升级时按钮文字
     * 「升级为 <下一级显示名>（-<造价>）」并启用，满级时隐藏该按钮。
     */
    private void upgradeEntry(Tower tower) {
        upgradeButton.setDisable(true);
    }

    /** 优先放选中塔右下方；放不下则翻到左侧/贴底，确保整块在画布内 */
    private void positionNear(double towerX, double towerY) {
        box.applyCss();
        box.autosize();
        double prefW = box.prefWidth(-1) + EDGE_MARGIN;
        double prefH = box.prefHeight(-1) + EDGE_MARGIN;
        double x = towerX + TOWER_OFFSET;
        if (x + prefW > viewW) x = towerX - TOWER_OFFSET - prefW;
        x = Math.max(EDGE_MARGIN, Math.min(x, viewW - prefW - EDGE_MARGIN));
        double y = towerY + TOWER_OFFSET;
        if (y + prefH > viewH) y = towerY - TOWER_OFFSET - prefH;
        y = Math.max(EDGE_MARGIN, Math.min(y, viewH - prefH - EDGE_MARGIN));
        box.setLayoutX(x);
        box.setLayoutY(y);
    }
}
