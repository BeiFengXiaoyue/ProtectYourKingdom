package com.kingdom.game.view;

import com.kingdom.game.controller.IGameStateReader;
import com.kingdom.game.controller.ITowerBuilder;
import com.kingdom.game.controller.ITowerSelectionNotifier;
import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.tower.ITowerUpgrade;
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
 * 等级（经 ITowerUpgrade.getLevel()）/攻击/射程/出售返还；「出售」经 ITowerBuilder.sellTower 结算（后端负责退款/移除）；
 * 「升级」只经 ITowerUpgrade 预览（可升级 → 「升级为 <显示名>（-<造价>）」，满级隐藏，
 * 金币不足实时置灰），点击经 ITowerBuilder.upgradeTower 由后端原位替换（§11/§13）。
 * 全程不 import 具体塔类、不做塔类 instanceof 分派。
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

        upgradeButton.setVisible(false);   // 由 upgradeEntry 按升级链状态显隐
        upgradeButton.setOnAction(e -> {
            if (current != null) builder.upgradeTower(current);   // 校验/扣费/替换由后端负责
        });
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
        // 标题暂显示英文类名（开发期/演示自检够用：零映射表、永不失配，由 C 重命名类时文案随之变）；
        // TODO 待 model 侧有正式塔名通道（塔类常量 / TowerSpec 注入）后替换为中文名
        titleLabel.setText(tower.getClass().getSimpleName());
        // 等级经 ITowerUpgrade.getLevel() 取：由类身份决定（1/2/3 级各是独立塔类，见《防御塔子系统说明》§8）；
        // Tower 基类已无 level 字段，非可升级塔兜底 1
        int level = tower instanceof ITowerUpgrade ? ((ITowerUpgrade) tower).getLevel() : 1;
        infoLabel.setText("等级 Lv." + level
                + "\n攻击 " + tower.getBaseAttackDamage()
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
     * 升级入口（经 ITowerUpgrade 预览，不 import 具体塔类）：
     * 满级/不支持升级 → 隐藏按钮；可升级 → 「升级为 <下一级显示名>（-<造价>）」，
     * 金币不足实时置灰（本方法经 refresh() 每帧调用，与建塔菜单金币置灰同款先例）。
     */
    private void upgradeEntry(Tower tower) {
        if (!(tower instanceof ITowerUpgrade)) {
            upgradeButton.setVisible(false);
            return;
        }
        TowerSpec next = ((ITowerUpgrade) tower).getNextLevelSpec();
        if (next == null) {
            upgradeButton.setVisible(false);   // 满级：隐藏
            return;
        }
        upgradeButton.setVisible(true);
        upgradeButton.setText("升级为 " + next.getDisplayName() + "（-" + next.getCost() + "）");
        upgradeButton.setDisable(stateReader.getCurrentGold() < next.getCost());
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
