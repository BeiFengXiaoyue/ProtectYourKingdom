package com.kingdom.game.util.editor;

import com.kingdom.game.util.balance.BalanceLibrary;
import com.kingdom.game.util.balance.BalanceTable;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * BalanceEditorTool —— 玩法数值可视化编辑器【启动入口】（本类不继承 Application）。
 *
 * 为什么这样设计：若主类直接继承 Application，在 IntelliJ 普通 Run 配置下会报
 * "缺少 JavaFX 运行时组件"。因此把真正窗口逻辑放在嵌套的 {@link BalanceEditorApp}，
 * 本类 main 只做一次 Application.launch 转发即可直接运行。
 *
 * 当前纳入 JSON 化的字段（15 个，分 5 组）：
 * - ① 玩家开局资源：initialGold / initialLives / totalWaves
 * - ② 普通敌人属性：normalHp / normalSpeed / normalGoldReward
 * - ③ 快速敌人属性：fastHp / fastSpeed / fastGoldReward
 * - ④ 重甲敌人属性：tankHp / tankSpeed / tankGoldReward
 * - ⑤ Boss 属性：bossHp / bossSpeed / bossGoldReward
 * 波次节奏字段不纳入 JSON 化（仍由 GameConfig 字面量默认值管理），避免合并冲突。
 *
 * 功能：
 * - 分五组展示 15 个固定玩法数值；
 * - 第六组「自定义变量」支持新增/删除扩展字段；
 * - 「载入」从 src/main/resources/config/balance.json 读取当前值；
 * - 「保存」校验范围后经 BalanceLibrary.write() 写入 balance.json，提示"重启游戏生效"；
 * - 「重置默认」恢复 BalanceTable.defaults() 的内置默认值。
 */
public final class BalanceEditorTool {

    private BalanceEditorTool() {
    }

    public static void main(String[] args) {
        Application.launch(BalanceEditorApp.class, args);
    }

    /** 实际的 JavaFX 应用（public static，供 Application.launch 反射实例化） */
    public static final class BalanceEditorApp extends Application {

        // ===== 固定字段键名集合（用于校验自定义变量不重名）=====
        private static final Set<String> FIXED_KEYS = Set.of(
                "initialGold", "initialLives", "totalWaves",
                "normalHp", "normalSpeed", "normalGoldReward",
                "fastHp", "fastSpeed", "fastGoldReward",
                "tankHp", "tankSpeed", "tankGoldReward",
                "bossHp", "bossSpeed", "bossGoldReward");

        // ===== ① 玩家开局 =====
        private final TextField initialGoldField = new TextField();
        private final TextField initialLivesField = new TextField();
        private final TextField totalWavesField = new TextField();

        // ===== ② 普通敌人 =====
        private final TextField normalHpField = new TextField();
        private final TextField normalSpeedField = new TextField();
        private final TextField normalGoldRewardField = new TextField();

        // ===== ③ 快速敌人 =====
        private final TextField fastHpField = new TextField();
        private final TextField fastSpeedField = new TextField();
        private final TextField fastGoldRewardField = new TextField();

        // ===== ④ 重甲敌人 =====
        private final TextField tankHpField = new TextField();
        private final TextField tankSpeedField = new TextField();
        private final TextField tankGoldRewardField = new TextField();

        // ===== ⑤ Boss =====
        private final TextField bossHpField = new TextField();
        private final TextField bossSpeedField = new TextField();
        private final TextField bossGoldRewardField = new TextField();

        // ===== 自定义变量（动态扩展）=====
        private final Map<String, TextField> customFields = new LinkedHashMap<>();
        private final VBox customFieldsBox = new VBox(6);
        private final Label customEmptyLabel = new Label("暂无自定义变量，点击下方「+ 新增变量」添加。\n"
                + "⚠ 注意：自定义变量只写入 balance.json，运行期无人读取、不影响游戏。");

        private final Label statusLabel = new Label("就绪：点击「载入」读取当前 balance.json，或直接填写后「保存」。");

        @Override
        public void start(Stage stage) {
            BorderPane root = new BorderPane();
            root.setTop(buildHeader());
            root.setCenter(buildScrollForm());
            root.setBottom(buildFooter());

            Scene scene = new Scene(root, 540, 780);
            stage.setTitle("王国保卫战 · 数值配置编辑器");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

            loadFromFile();
        }

        // ================= 顶部标题 =================
        private VBox buildHeader() {
            Label title = new Label("玩法数值配置编辑器");
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            Label desc = new Label("修改数值后点击「保存」写入 config/balance.json，重启游戏生效。（波次节奏不纳入此编辑器）");
            desc.setWrapText(true);
            desc.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
            VBox box = new VBox(4, title, desc);
            box.setPadding(new Insets(12, 16, 8, 16));
            box.setStyle("-fx-background-color: #f0f0f0;");
            return box;
        }

        // ================= 中间表单（可滚动，六组）=================
        private ScrollPane buildScrollForm() {
            VBox form = new VBox(12);
            form.setPadding(new Insets(12, 16, 12, 16));

            // ① 玩家开局
            TitledPane group1 = new TitledPane("① 玩家开局资源",
                    new VBox(8,
                            fieldRow("初始金币", "开局拥有的金币数", initialGoldField),
                            fieldRow("初始生命", "敌人到达终点扣 1，归零失败", initialLivesField),
                            fieldRow("总波数", "打完这么多波敌人即胜利", totalWavesField)));
            group1.setCollapsible(false);

            // ② 普通敌人
            TitledPane group2 = new TitledPane("② 普通敌人属性",
                    new VBox(8,
                            fieldRow("敌人血量", "普通敌人 HP", normalHpField),
                            fieldRow("敌人速度", "移动速度（像素/秒）", normalSpeedField),
                            fieldRow("击杀赏金", "击杀普通敌人获得金币", normalGoldRewardField)));
            group2.setCollapsible(false);

            // ③ 快速敌人
            TitledPane group3 = new TitledPane("③ 快速敌人属性",
                    new VBox(8,
                            fieldRow("快速血量", "快速敌人 HP（约普通 0.6 倍）", fastHpField),
                            fieldRow("快速速度", "快速敌人速度（约普通 2 倍）", fastSpeedField),
                            fieldRow("快速赏金", "击杀快速敌人获得金币", fastGoldRewardField)));
            group3.setCollapsible(false);

            // ④ 重甲敌人
            TitledPane group4 = new TitledPane("④ 重甲敌人属性",
                    new VBox(8,
                            fieldRow("重甲血量", "重甲敌人 HP（约普通 3 倍）", tankHpField),
                            fieldRow("重甲速度", "重甲敌人速度（约普通 0.6 倍）", tankSpeedField),
                            fieldRow("重甲赏金", "击杀重甲敌人获得金币", tankGoldRewardField)));
            group4.setCollapsible(false);

            // ⑤ Boss
            TitledPane group5 = new TitledPane("⑤ Boss 属性",
                    new VBox(8,
                            fieldRow("Boss 血量", "Boss HP（最终波首领）", bossHpField),
                            fieldRow("Boss 速度", "Boss 移动速度", bossSpeedField),
                            fieldRow("Boss 赏金", "击杀 Boss 获得金币", bossGoldRewardField)));
            group5.setCollapsible(false);

            // ⑥ 自定义变量
            customEmptyLabel.setWrapText(true);
            customEmptyLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
            // 醒目提示：这些字段运行期无人读取，避免"配了不生效却无提示"（见 BalanceTable.fromJson 告警）
            Label customWarnLabel = new Label("⚠ 这些字段运行期无人读取，仅随文件保存——"
                    + "在此新增的变量不会影响游戏（启动时会打印 [BalanceTable] 未知字段告警）。");
            customWarnLabel.setWrapText(true);
            customWarnLabel.setStyle("-fx-text-fill: #d9534f; -fx-font-size: 12px; -fx-font-weight: bold;");
            Button addCustomBtn = new Button("+ 新增变量");
            addCustomBtn.setStyle("-fx-background-color: #5cb85c; -fx-text-fill: white;");
            addCustomBtn.setOnAction(e -> addCustomVariable());

            VBox customContent = new VBox(8, customWarnLabel, customEmptyLabel, customFieldsBox, addCustomBtn);
            customContent.setAlignment(Pos.CENTER_LEFT);
            TitledPane group6 = new TitledPane("⑥ 自定义变量（未来扩展用 · 运行期不生效）", customContent);
            group6.setCollapsible(false);

            form.getChildren().addAll(group1, group2, group3, group4, group5, group6);

            ScrollPane scroll = new ScrollPane(form);
            scroll.setFitToWidth(true);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            return scroll;
        }

        /** 一行：标签 + 输入框 */
        private HBox fieldRow(String label, String hint, TextField field) {
            Label nameLabel = new Label(label);
            nameLabel.setPrefWidth(110);
            nameLabel.setStyle("-fx-font-weight: bold;");
            field.setPromptText(hint);
            field.setPrefWidth(180);
            HBox row = new HBox(8, nameLabel, field);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(field, Priority.ALWAYS);
            return row;
        }

        // ================= 自定义变量：新增 =================
        private void addCustomVariable() {
            TextInputDialog dlg = new TextInputDialog("newVariable");
            dlg.setTitle("新增自定义变量");
            dlg.setHeaderText("输入变量名");
            dlg.setContentText("变量名（英文+数字+下划线，如 cannonDamage）：");
            dlg.showAndWait().ifPresent(rawName -> {
                String name = rawName.trim();
                if (name.isEmpty()) { error("新增失败", "变量名不能为空。"); return; }
                if (!name.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                    error("新增失败", "变量名只能包含字母、数字、下划线，且不能以数字开头。"); return;
                }
                if (FIXED_KEYS.contains(name)) {
                    error("新增失败", "变量名「" + name + "」是固定字段，请在上方对应分组中修改。"); return;
                }
                if (customFields.containsKey(name)) {
                    error("新增失败", "变量名「" + name + "」已存在。"); return;
                }
                TextField field = new TextField("0");
                field.setPromptText("数值");
                Button delBtn = new Button("删除");
                delBtn.setStyle("-fx-text-fill: #d9534f;");
                delBtn.setOnAction(e -> removeCustomVariable(name));

                Label nameLabel = new Label(name);
                nameLabel.setPrefWidth(140);
                nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #5cb85c;");

                HBox row = new HBox(8, nameLabel, field, delBtn);
                row.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(field, Priority.ALWAYS);

                customFields.put(name, field);
                customFieldsBox.getChildren().add(row);
                customEmptyLabel.setVisible(false);
                customEmptyLabel.setManaged(false);
                status("已添加自定义变量「" + name + "」，填写数值后点击「保存」写入。");
            });
        }

        // ================= 自定义变量：删除 =================
        private void removeCustomVariable(String name) {
            TextField field = customFields.remove(name);
            if (field != null) {
                customFieldsBox.getChildren().removeIf(node -> {
                    if (node instanceof HBox) {
                        HBox row = (HBox) node;
                        if (!row.getChildren().isEmpty() && row.getChildren().get(0) instanceof Label) {
                            return ((Label) row.getChildren().get(0)).getText().equals(name);
                        }
                    }
                    return false;
                });
            }
            if (customFields.isEmpty()) {
                customEmptyLabel.setVisible(true);
                customEmptyLabel.setManaged(true);
            }
            status("已删除自定义变量「" + name + "」，点击「保存」后从 JSON 中移除。");
        }

        // ================= 底部按钮 + 状态栏 =================
        private VBox buildFooter() {
            Button loadBtn = new Button("载入");
            loadBtn.setOnAction(e -> loadFromFile());

            Button saveBtn = new Button("保存");
            saveBtn.setStyle("-fx-background-color: #4a90d9; -fx-text-fill: white; -fx-font-weight: bold;");
            saveBtn.setOnAction(e -> saveToFile());

            Button resetBtn = new Button("重置默认");
            resetBtn.setOnAction(e -> resetToDefaults());

            HBox buttons = new HBox(10, loadBtn, saveBtn, resetBtn);
            buttons.setAlignment(Pos.CENTER);
            buttons.setPadding(new Insets(8, 16, 4, 16));

            statusLabel.setWrapText(true);
            statusLabel.setPadding(new Insets(0, 16, 8, 16));
            statusLabel.setStyle("-fx-text-fill: #444; -fx-font-size: 12px;");

            VBox footer = new VBox(0, buttons, statusLabel);
            footer.setStyle("-fx-background-color: #f0f0f0;");
            return footer;
        }

        // ================= 载入 =================
        private void loadFromFile() {
            BalanceTable table = BalanceLibrary.read();
            if (table == null) {
                table = BalanceTable.defaults();
                status("未找到 balance.json，已填入内置默认值（保存后会创建该文件）。");
            } else {
                status("已从 balance.json 载入当前数值。");
            }
            fillFields(table);
        }

        private void fillFields(BalanceTable t) {
            // ① 玩家开局
            initialGoldField.setText(String.valueOf(t.getInitialGold()));
            initialLivesField.setText(String.valueOf(t.getInitialLives()));
            totalWavesField.setText(String.valueOf(t.getTotalWaves()));
            // ② 普通敌人
            normalHpField.setText(String.valueOf(t.getNormalHp()));
            normalSpeedField.setText(String.valueOf(t.getNormalSpeed()));
            normalGoldRewardField.setText(String.valueOf(t.getNormalGoldReward()));
            // ③ 快速敌人
            fastHpField.setText(String.valueOf(t.getFastHp()));
            fastSpeedField.setText(String.valueOf(t.getFastSpeed()));
            fastGoldRewardField.setText(String.valueOf(t.getFastGoldReward()));
            // ④ 重甲敌人
            tankHpField.setText(String.valueOf(t.getTankHp()));
            tankSpeedField.setText(String.valueOf(t.getTankSpeed()));
            tankGoldRewardField.setText(String.valueOf(t.getTankGoldReward()));
            // ⑤ Boss
            bossHpField.setText(String.valueOf(t.getBossHp()));
            bossSpeedField.setText(String.valueOf(t.getBossSpeed()));
            bossGoldRewardField.setText(String.valueOf(t.getBossGoldReward()));

            // 自定义变量：先清空再重建
            customFields.clear();
            customFieldsBox.getChildren().clear();
            for (String key : t.getExtraKeys()) {
                double val = t.getExtraDouble(key, 0);
                TextField field = new TextField(String.valueOf(val));
                field.setPromptText("数值");
                Button delBtn = new Button("删除");
                delBtn.setStyle("-fx-text-fill: #d9534f;");
                delBtn.setOnAction(e -> removeCustomVariable(key));

                Label nameLabel = new Label(key);
                nameLabel.setPrefWidth(140);
                nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #5cb85c;");

                HBox row = new HBox(8, nameLabel, field, delBtn);
                row.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(field, Priority.ALWAYS);

                customFields.put(key, field);
                customFieldsBox.getChildren().add(row);
            }
            if (customFields.isEmpty()) {
                customEmptyLabel.setVisible(true);
                customEmptyLabel.setManaged(true);
            } else {
                customEmptyLabel.setVisible(false);
                customEmptyLabel.setManaged(false);
            }
        }

        // ================= 保存 =================
        private void saveToFile() {
            BalanceTable table = BalanceTable.defaults();
            try {
                // ① 玩家开局
                table.setInitialGold(readInt(initialGoldField, "初始金币", 0, true));
                table.setInitialLives(readInt(initialLivesField, "初始生命", 1, false));
                table.setTotalWaves(readInt(totalWavesField, "总波数", 1, false));
                // ② 普通敌人
                table.setNormalHp(readInt(normalHpField, "普通血量", 1, false));
                table.setNormalSpeed(readDouble(normalSpeedField, "普通速度", 0, false));
                table.setNormalGoldReward(readInt(normalGoldRewardField, "普通赏金", 0, true));
                // ③ 快速敌人
                table.setFastHp(readInt(fastHpField, "快速血量", 1, false));
                table.setFastSpeed(readDouble(fastSpeedField, "快速速度", 0, false));
                table.setFastGoldReward(readInt(fastGoldRewardField, "快速赏金", 0, true));
                // ④ 重甲敌人
                table.setTankHp(readInt(tankHpField, "重甲血量", 1, false));
                table.setTankSpeed(readDouble(tankSpeedField, "重甲速度", 0, false));
                table.setTankGoldReward(readInt(tankGoldRewardField, "重甲赏金", 0, true));
                // ⑤ Boss
                table.setBossHp(readInt(bossHpField, "Boss 血量", 1, false));
                table.setBossSpeed(readDouble(bossSpeedField, "Boss 速度", 0, false));
                table.setBossGoldReward(readInt(bossGoldRewardField, "Boss 赏金", 0, true));

                // 自定义变量
                for (Map.Entry<String, TextField> e : customFields.entrySet()) {
                    String key = e.getKey();
                    String text = e.getValue().getText() == null ? "" : e.getValue().getText().trim();
                    try {
                        double val = Double.parseDouble(text);
                        table.setExtra(key, val);
                    } catch (NumberFormatException ex) {
                        error("校验失败", "自定义变量「" + key + "」必须是数字，当前: \"" + text + "\"");
                        return;
                    }
                }
            } catch (ValidationException ex) {
                error("校验失败", ex.getMessage());
                return;
            }

            boolean ok = BalanceLibrary.write(table);
            if (ok) {
                int customCount = customFields.size();
                String customMsg = customCount > 0 ? "（含 " + customCount + " 个自定义变量）" : "";
                status("保存成功！已写入 config/balance.json" + customMsg + "。重启游戏后生效。");
            } else {
                error("保存失败", "未找到 src/main/resources 目录，请确认以仓库根目录运行。");
            }
        }

        // ================= 重置默认 =================
        private void resetToDefaults() {
            fillFields(BalanceTable.defaults());
            status("已恢复内置默认值（自定义变量已清空），如需生效请点击「保存」。");
        }

        // ================= 校验工具 =================
        private int readInt(TextField field, String name, int min, boolean allowEq) throws ValidationException {
            String text = field.getText() == null ? "" : field.getText().trim();
            try {
                int v = Integer.parseInt(text);
                if (allowEq ? v >= min : v > min) return v;
                throw new ValidationException(name + " 必须" + (allowEq ? "≥" : ">") + " " + min + "，当前: " + v);
            } catch (NumberFormatException e) {
                throw new ValidationException(name + " 必须是整数，当前: \"" + text + "\"");
            }
        }

        private double readDouble(TextField field, String name, double min, boolean allowEq) throws ValidationException {
            String text = field.getText() == null ? "" : field.getText().trim();
            try {
                double v = Double.parseDouble(text);
                if (allowEq ? v >= min : v > min) return v;
                throw new ValidationException(name + " 必须" + (allowEq ? "≥" : ">") + " " + min + "，当前: " + v);
            } catch (NumberFormatException e) {
                throw new ValidationException(name + " 必须是数字，当前: \"" + text + "\"");
            }
        }

        /** 校验异常（仅用于编辑器内部提前中断） */
        private static final class ValidationException extends Exception {
            ValidationException(String msg) { super(msg); }
        }

        // ================= UI 工具 =================
        private void status(String msg) {
            statusLabel.setText(msg);
        }

        private void error(String title, String msg) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg);
            a.showAndWait();
        }
    }
}
