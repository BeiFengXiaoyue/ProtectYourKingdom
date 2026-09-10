package com.kingdom.game.util.editor;

import com.kingdom.game.util.map.LevelWaves;
import com.kingdom.game.util.map.LevelWaves.SpawnGroup;
import com.kingdom.game.util.map.LevelWaves.Wave;
import com.kingdom.game.util.map.MapLibrary;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * WaveEditorTool —— 可视化关卡波次编辑器的【启动入口】（本类不继承 Application）。
 *
 * 为什么这样设计：若主类直接继承 Application，在 IntelliJ 普通 Run 配置（JavaFX 走 classpath
 * 而非 module-path）下运行会报“错误: 缺少 JavaFX 运行时组件”。因此把真正窗口逻辑放在嵌套的
 * {@link WaveEditorApp}，本类 main 只做一次 Application.launch 转发即可直接运行。
 *
 * 使用：把 pom 的 javafx mainClass 改为 com.kingdom.game.util.editor.WaveEditorTool 后 mvn javafx:run；
 * IDE 直接运行不可用（JavaFX 模块检查）——或直接运行 WaveEditorLauncher。
 *
 * 操作：
 * 1. 顶栏选「地图」（maps/index.json 已有地图）或「新建地图…」；每关一份 maps/&lt;key&gt;/waves.json；
 * 2. 左栏波次列表：新增/删除/整波复制/上移/下移（数组顺序即游戏内波次顺序）；
 *    选中波后填 maxPrepMs（留空=跟随全局默认；第 1 波开局直接开始，本字段无效）；
 * 3. 中栏出怪组：类型下拉/数量/间隔，「应用修改」写回选中组（保存前也会自动应用）；
 *    底部时间轴按规范 §3.3 公式实时演算（每只 = 上一只出生时刻 + 其所在组 intervalMs）；
 * 4. 「保存到该地图」写 maps/&lt;key&gt;/waves.json；保存前按规范 §3.4 校验，非法禁止写出；
 *    游戏重启后经 GameConfig/MapLibrary 读取生效。
 *
 * 数据契约见 docs/关卡波次配置规范.md；本工具不感知任何敌人数值（数值归 GameConfig）。
 */
public final class WaveEditorTool {

    private WaveEditorTool() {
    }

    public static void main(String[] args) {
        Application.launch(WaveEditorApp.class, args);
    }

    /**
     * 实际的 JavaFX 应用（public static，供 Application.launch 反射实例化）。
     */
    public static final class WaveEditorApp extends Application {

        // ===== 数据（编辑中模型；save 时经 §3.4 校验后落盘）=====
        private LevelWaves waves = LevelWaves.template("");
        private String currentMapKey = "default";
        private int selectedWaveIndex = -1;
        private int selectedGroupIndex = -1;
        private boolean uiBusy = false;   // 程序化刷新列表/回填字段时防回调重入

        // 地图分包（maps/index.json + maps/<key>/）
        private final ChoiceBox<String> mapChoice = new ChoiceBox<>();
        private final Map<String, String> mapLabelToKey = new LinkedHashMap<>();

        // 波次列表 + 单波字段
        private final ListView<String> waveList = new ListView<>();
        private final TextField maxPrepField = new TextField();

        // 出怪组列表 + 行编辑区
        private final ListView<String> groupList = new ListView<>();
        private final ChoiceBox<String> groupTypeChoice = new ChoiceBox<>();
        private final Map<String, String> groupTypeToId = new LinkedHashMap<>();
        private final TextField groupCountField = new TextField("1");
        private final TextField groupIntervalField = new TextField("1000");

        // 时间轴预览 + 状态栏
        private final TextArea timelineArea = new TextArea();
        private final Label statusLabel = new Label("提示：选择地图后编辑波次，保存写 maps/<key>/waves.json。");

        @Override
        public void start(Stage stage) {
            populateGroupTypes();

            BorderPane root = new BorderPane();
            root.setTop(buildTopBar());
            root.setLeft(buildWavePanel());
            root.setCenter(buildGroupPanel());
            VBox bottom = new VBox(2, buildTimelineArea(), statusLabel);
            root.setBottom(bottom);

            Scene scene = new Scene(root, 1100, 700);
            stage.setTitle("王国保卫战 · 波次编辑器");
            stage.setScene(scene);
            stage.show();

            statusLabel.setWrapText(true);
            statusLabel.setStyle("-fx-text-fill: #444; -fx-padding: 0 10 8 10;");

            mapChoice.setOnAction(e -> onMapSelected());
            waveList.getSelectionModel().selectedItemProperty()
                    .addListener((obs, o, n) -> onWaveSelected());
            groupList.getSelectionModel().selectedItemProperty()
                    .addListener((obs, o, n) -> onGroupSelected());
            maxPrepField.textProperty().addListener((obs, o, n) -> onMaxPrepEdited());

            initMaps();
        }

        // ================= 地图选择 / 新建 =================
        private void initMaps() {
            populateMaps();
            currentMapKey = MapLibrary.defaultKey();
            if (MapLibrary.exists(currentMapKey)) {
                mapChoice.setValue(MapLibrary.entryLabel(MapLibrary.getEntry(currentMapKey)));
                loadMapByKey(currentMapKey);
            } else {
                statusLabel.setText("暂无可用地图，请「新建地图」或先在 maps/ 建立 index");
            }
        }

        private void populateMaps() {
            mapLabelToKey.clear();
            mapChoice.getItems().clear();
            for (MapLibrary.MapEntry e : MapLibrary.listMaps()) {
                String label = MapLibrary.entryLabel(e);
                mapLabelToKey.put(label, e.getKey());
                mapChoice.getItems().add(label);
            }
        }

        private void onMapSelected() {
            String label = mapChoice.getValue();
            String key = mapLabelToKey.get(label);
            if (key != null && !key.equals(currentMapKey)) {
                currentMapKey = key;
                loadMapByKey(key);
            }
        }

        private void newMap() {
            TextInputDialog k = new TextInputDialog("newmap");
            k.setTitle("新建地图");
            k.setHeaderText("地图 key（目录名，字母数字下划线）");
            k.showAndWait().ifPresent(keyRaw -> {
                String key = keyRaw.trim().replaceAll("[^A-Za-z0-9_\\-]", "_");
                if (key.isEmpty()) return;
                TextInputDialog n = new TextInputDialog(key);
                n.setTitle("新建地图");
                n.setHeaderText("显示名称");
                String name = n.showAndWait().orElse(key);
                FileChooser fc = new FileChooser();
                fc.setTitle("选择该地图的底图");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                        "图片", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
                File img = fc.showOpenDialog(null);
                MapLibrary.MapEntry entry = MapLibrary.createMap(key, name, img);
                if (entry == null) {
                    error("新建失败", "请以仓库根目录运行");
                    return;
                }
                populateMaps();
                mapChoice.setValue(MapLibrary.entryLabel(entry));
                currentMapKey = key;
                loadMapByKey(key);
                statusLabel.setText("已新建地图 " + MapLibrary.entryLabel(entry) + "（可开始配波次）");
            });
        }

        /** 加载某地图的波次表：readWaves（repo 优先退 classpath）；无则新建空表模板 */
        private void loadMapByKey(String key) {
            MapLibrary.MapEntry entry = MapLibrary.getEntry(key);
            if (entry == null) {
                statusLabel.setText("地图不存在：" + key);
                return;
            }
            LevelWaves loaded = MapLibrary.readWaves(key);
            if (loaded == null) {
                waves = LevelWaves.template(key + "_waves");
                statusLabel.setText("已载入地图 " + MapLibrary.entryLabel(entry)
                        + "：该关暂无 waves.json，已新建空表模板（1 波：普通敌人×1@1000ms）");
            } else {
                waves = loaded;
                statusLabel.setText("已载入 " + MapLibrary.entryLabel(entry) + " 的波次表（共 "
                        + waves.getWaves().size() + " 波）");
            }
            selectedWaveIndex = waves.getWaves().isEmpty() ? -1 : 0;
            selectedGroupIndex = -1;
            refreshWaveList();
        }

        // ================= 波次操作 =================
        private void addWave() {
            Wave w = new Wave(waves.getWaves().size() + 1);
            w.getGroups().add(new SpawnGroup());
            waves.getWaves().add(w);
            selectedWaveIndex = waves.getWaves().size() - 1;
            refreshWaveList();
            statusLabel.setText("已新增第 " + waves.getWaves().size() + " 波（默认 1 组 普通敌人×1@1000ms）");
        }

        private void removeWave() {
            if (selectedWaveIndex < 0) {
                statusLabel.setText("请先选中要删除的波次");
                return;
            }
            waves.getWaves().remove(selectedWaveIndex);
            selectedWaveIndex = Math.min(selectedWaveIndex, waves.getWaves().size() - 1);
            selectedGroupIndex = -1;
            refreshWaveList();
            statusLabel.setText("已删除波次，剩 " + waves.getWaves().size() + " 波");
        }

        /** 整波复制（组逐个深拷贝），插到源波之后 */
        private void copyWave() {
            Wave src = currentWave();
            if (src == null) {
                statusLabel.setText("请先选中要复制的波次");
                return;
            }
            Wave copy = new Wave(waves.getWaves().size() + 1);
            copy.setMaxPrepMs(src.getMaxPrepMs());
            for (SpawnGroup g : src.getGroups()) {
                copy.getGroups().add(new SpawnGroup(g.getType(), g.getCount(), g.getIntervalMs()));
            }
            waves.getWaves().add(selectedWaveIndex + 1, copy);
            selectedWaveIndex = selectedWaveIndex + 1;
            refreshWaveList();
            statusLabel.setText("已复制为第 " + (selectedWaveIndex + 1) + " 波");
        }

        private void moveWave(int delta) {
            int from = selectedWaveIndex;
            int to = from + delta;
            if (from < 0 || to < 0 || to >= waves.getWaves().size()) return;
            Collections.swap(waves.getWaves(), from, to);
            selectedWaveIndex = to;
            refreshWaveList();
        }

        private void onWaveSelected() {
            if (uiBusy) return;
            selectedWaveIndex = waveList.getSelectionModel().getSelectedIndex();
            selectedGroupIndex = -1;
            refreshGroupPane();
        }

        // ================= 出怪组操作 =================
        private void addGroup() {
            Wave w = currentWave();
            if (w == null) {
                statusLabel.setText("请先选中一个波次");
                return;
            }
            w.getGroups().add(new SpawnGroup());
            selectedGroupIndex = w.getGroups().size() - 1;
            refreshGroupPane();
            statusLabel.setText("已添加出怪组 " + w.getGroups().size() + "（默认 普通敌人 ×1 @1000ms）");
        }

        private void removeGroup() {
            Wave w = currentWave();
            if (w == null || selectedGroupIndex < 0) {
                statusLabel.setText("请先选中要删除的出怪组");
                return;
            }
            w.getGroups().remove(selectedGroupIndex);
            selectedGroupIndex = Math.min(selectedGroupIndex, w.getGroups().size() - 1);
            refreshGroupPane();
            statusLabel.setText("已删除出怪组");
        }

        private void moveGroup(int delta) {
            Wave w = currentWave();
            if (w == null) return;
            int from = selectedGroupIndex;
            int to = from + delta;
            if (from < 0 || to < 0 || to >= w.getGroups().size()) return;
            Collections.swap(w.getGroups(), from, to);
            selectedGroupIndex = to;
            refreshGroupPane();
        }

        /** 把行编辑区（类型/数量/间隔）写回选中组；非法弹窗拦截。保存前也会自动调用 */
        private boolean applyGroupEdits() {
            SpawnGroup g = currentGroup();
            if (g == null) {
                statusLabel.setText("请先选中波次和出怪组再应用修改");
                return false;
            }
            String type = groupTypeToId.get(groupTypeChoice.getValue());
            Integer count = parseInt(groupCountField.getText());
            Long interval = parseLong(groupIntervalField.getText());
            if (type == null || !LevelWaves.VOCABULARY.containsKey(type)) {
                error("应用失败", "敌人类型未知或未选择，请从下拉选择");
                return false;
            }
            if (count == null || count < 1) {
                error("应用失败", "数量须为 ≥ 1 的整数");
                return false;
            }
            if (interval == null || interval <= 0) {
                error("应用失败", "出生间隔须为 > 0 的整数（毫秒）");
                return false;
            }
            g.setType(type);
            g.setCount(count);
            g.setIntervalMs(interval);
            refreshGroupPane();
            statusLabel.setText("已修改出怪组 " + (selectedGroupIndex + 1) + "（记得保存）");
            return true;
        }

        private void onGroupSelected() {
            if (uiBusy) return;
            selectedGroupIndex = groupList.getSelectionModel().getSelectedIndex();
            loadGroupIntoFields();
        }

        // ================= UI 构建 =================
        private HBox buildTopBar() {
            Button newMapBtn = new Button("新建地图…");
            newMapBtn.setOnAction(e -> newMap());

            Button saveGame = new Button("保存到该地图");
            saveGame.setOnAction(e -> saveToGameResources());

            Button saveAs = new Button("另存为 JSON");
            saveAs.setOnAction(e -> saveAsJson());

            mapChoice.setPrefWidth(230);

            HBox bar = new HBox(8, new Label("地图:"), mapChoice, newMapBtn,
                    new SeparatorV(), saveGame, saveAs);
            bar.setPadding(new Insets(8));
            bar.setAlignment(Pos.CENTER_LEFT);
            bar.setStyle("-fx-background-color: #e0e0e0;");
            return bar;
        }

        private static final class SeparatorV extends Region {
            SeparatorV() { setPrefWidth(2); setStyle("-fx-background-color: #bbb;"); }
        }

        private VBox buildWavePanel() {
            Label title = new Label("波次列表（顺序 = 游戏内波次顺序）");
            title.setStyle("-fx-font-weight: bold;");

            waveList.setPrefWidth(240);
            waveList.setPrefHeight(330);

            Button add = new Button("+ 新增波");
            add.setOnAction(e -> addWave());
            Button copy = new Button("整波复制");
            copy.setOnAction(e -> copyWave());
            Button up = new Button("上移");
            up.setOnAction(e -> moveWave(-1));
            Button down = new Button("下移");
            down.setOnAction(e -> moveWave(1));
            Button del = new Button("- 删除波");
            del.setOnAction(e -> removeWave());

            Label prepHint = new Label("本波 maxPrepMs（毫秒，回车生效）：\n"
                    + "留空=跟随全局默认；第 1 波开局直接开始，本字段无效");
            prepHint.setWrapText(true);

            VBox panel = new VBox(8, title, waveList,
                    new HBox(6, add, copy), new HBox(6, up, down, del),
                    prepHint, maxPrepField);
            panel.setPadding(new Insets(10));
            panel.setPrefWidth(290);
            return panel;
        }

        private VBox buildGroupPanel() {
            Label title = new Label("当前波的出怪组（组内相邻两只间隔 = intervalMs）");
            title.setStyle("-fx-font-weight: bold;");

            groupList.setPrefHeight(250);

            Button add = new Button("+ 添加组");
            add.setOnAction(e -> addGroup());
            Button del = new Button("- 删除组");
            del.setOnAction(e -> removeGroup());
            Button up = new Button("上移");
            up.setOnAction(e -> moveGroup(-1));
            Button down = new Button("下移");
            down.setOnAction(e -> moveGroup(1));
            Button apply = new Button("应用修改");
            apply.setOnAction(e -> applyGroupEdits());

            groupTypeChoice.setPrefWidth(190);

            VBox panel = new VBox(8, title, groupList,
                    new HBox(6, add, up, down, del),
                    new HBox(6, new Label("类型:"), groupTypeChoice),
                    new HBox(6, new Label("数量:"), groupCountField,
                            new Label("间隔ms:"), groupIntervalField, apply));
            panel.setPadding(new Insets(10));
            return panel;
        }

        private VBox buildTimelineArea() {
            Label title = new Label("出怪时间轴预览");
            title.setStyle("-fx-font-weight: bold;");
            timelineArea.setEditable(false);
            timelineArea.setPrefRowCount(9);
            timelineArea.setStyle("-fx-font-family: monospace;");
            VBox box = new VBox(4, title, timelineArea);
            box.setPadding(new Insets(6, 10, 4, 10));
            return box;
        }

        // ================= 刷新（界面 ⇄ 模型同步）=================
        private Wave currentWave() {
            if (selectedWaveIndex < 0 || selectedWaveIndex >= waves.getWaves().size()) return null;
            return waves.getWaves().get(selectedWaveIndex);
        }

        private SpawnGroup currentGroup() {
            Wave w = currentWave();
            if (w == null || selectedGroupIndex < 0 || selectedGroupIndex >= w.getGroups().size()) return null;
            return w.getGroups().get(selectedGroupIndex);
        }

        private void refreshWaveList() {
            uiBusy = true;   // setAll 会同步触发选中监听，全程屏蔽防 selectedWaveIndex 被冲掉
            try {
                List<String> items = new ArrayList<>();
                for (int i = 0; i < waves.getWaves().size(); i++) {
                    items.add("第 " + (i + 1) + " 波（id=" + waves.getWaves().get(i).getId() + "）");
                }
                waveList.getItems().setAll(items);
                if (selectedWaveIndex >= items.size()) selectedWaveIndex = items.size() - 1;
                if (selectedWaveIndex >= 0) waveList.getSelectionModel().select(selectedWaveIndex);
                else waveList.getSelectionModel().clearSelection();
            } finally {
                uiBusy = false;
            }
            refreshGroupPane();
        }

        /** 按选中波重建：maxPrep 字段、出怪组列表与行编辑区（uiBusy 全程防重入） */
        private void refreshGroupPane() {
            uiBusy = true;
            try {
                Wave w = currentWave();
                maxPrepField.setText(w == null || w.getMaxPrepMs() < 0
                        ? "" : String.valueOf(w.getMaxPrepMs()));

                List<String> items = new ArrayList<>();
                if (w != null) {
                    int cumulative = 0;
                    for (int i = 0; i < w.getGroups().size(); i++) {
                        SpawnGroup g = w.getGroups().get(i);
                        cumulative += g.getCount();
                        items.add(groupRowText(i, g, cumulative));
                    }
                }
                groupList.getItems().setAll(items);

                int size = w == null ? 0 : w.getGroups().size();
                if (selectedGroupIndex >= size) selectedGroupIndex = size - 1;
                if (selectedGroupIndex >= 0) groupList.getSelectionModel().select(selectedGroupIndex);
                else groupList.getSelectionModel().clearSelection();
                loadGroupIntoFields();
            } finally {
                uiBusy = false;
            }
            refreshTimeline();
        }

        /** 行文本自带规范 #5 的行间提示：该组预计耗时 count×intervalMs 与累计出怪数 */
        private static String groupRowText(int index, SpawnGroup g, int cumulative) {
            return (index + 1) + ". " + LevelWaves.labelOf(g.getType()) + " ×" + g.getCount()
                    + " @" + g.getIntervalMs() + "ms（组耗时 " + ((long) g.getCount() * g.getIntervalMs())
                    + "ms，累计 " + cumulative + " 只）";
        }

        /** 把选中组回填到行编辑区（类型/数量/间隔）；无选中组时回填默认值 */
        private void loadGroupIntoFields() {
            SpawnGroup g = currentGroup();
            if (g == null) {
                groupTypeChoice.getSelectionModel().clearSelection();
                groupCountField.setText("1");
                groupIntervalField.setText("1000");
                return;
            }
            String disp = displayOfType(g.getType());
            if (disp != null) groupTypeChoice.getSelectionModel().select(disp);
            groupCountField.setText(String.valueOf(g.getCount()));
            groupIntervalField.setText(String.valueOf(g.getIntervalMs()));
        }

        /** maxPrep 输入即时写回选中波（留空=−1 跟随全局；非法仅提示不写回） */
        private void onMaxPrepEdited() {
            if (uiBusy) return;
            Wave w = currentWave();
            if (w == null) return;
            String t = maxPrepField.getText() == null ? "" : maxPrepField.getText().trim();
            if (t.isEmpty()) {
                w.setMaxPrepMs(-1);
                return;
            }
            try {
                long v = Long.parseLong(t);
                if (v < 0) {
                    statusLabel.setText("maxPrepMs 需 ≥ 0，或留空跟随全局默认");
                    return;
                }
                w.setMaxPrepMs(v);
                statusLabel.setText("第 " + (selectedWaveIndex + 1) + " 波 maxPrepMs = " + v + "ms（记得保存）");
            } catch (NumberFormatException ex) {
                statusLabel.setText("maxPrepMs 需为非负整数，或留空跟随全局默认");
            }
        }

        // ================= 校验与时间轴 =================
        /** 规范 §3.4 全规则收集错误明细（空=通过）；maxPrepMs 由模型保证 −1 或 ≥0，无需再查 */
        private List<String> validate() {
            List<String> errors = new ArrayList<>();
            if (waves.getWaves().isEmpty()) {
                errors.add("至少需要 1 个波次");
                return errors;
            }
            for (int i = 0; i < waves.getWaves().size(); i++) {
                Wave w = waves.getWaves().get(i);
                String at = "第 " + (i + 1) + " 波";
                if (w.getGroups().isEmpty()) errors.add(at + "：至少需要 1 个出怪组");
                for (int j = 0; j < w.getGroups().size(); j++) {
                    SpawnGroup g = w.getGroups().get(j);
                    String at2 = at + " 出怪组 " + (j + 1);
                    if (g.getType() == null || !LevelWaves.VOCABULARY.containsKey(g.getType())) {
                        errors.add(at2 + "：敌人类型未知");
                    }
                    if (g.getCount() < 1) errors.add(at2 + "：数量须 ≥ 1");
                    if (g.getIntervalMs() <= 0) errors.add(at2 + "：出生间隔须 > 0（毫秒）");
                }
            }
            return errors;
        }

        /** 规范 §3.3 时间轴：每只 = 上一只出生时刻 + 其所在组 intervalMs（无缝接力，组内组间同一条公式） */
        private void refreshTimeline() {
            Wave w = currentWave();
            if (w == null || w.getGroups().isEmpty()) {
                timelineArea.setText("（选中波次并添加出怪组后，这里按规范 §3.3 显示「时刻 → 敌人」时间轴）");
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("第 ").append(selectedWaveIndex + 1)
              .append(" 波时间轴（相对本波开始 T；每只 = 上一只出生时刻 + 其所在组 intervalMs）\n");
            long lastSpawn = -1;
            int seq = 0;
            int total = 0;
            for (SpawnGroup g : w.getGroups()) {
                String label = LevelWaves.labelOf(g.getType());
                for (int c = 0; c < g.getCount(); c++) {
                    long t = lastSpawn < 0 ? 0 : lastSpawn + g.getIntervalMs();
                    seq++;
                    sb.append("  +").append(t).append(" ms   ").append(label)
                      .append(" #").append(seq).append("\n");
                    lastSpawn = t;
                }
                total += g.getCount();
            }
            sb.append("出怪完毕：共 ").append(total).append(" 只，末只 +").append(lastSpawn).append(" ms");
            timelineArea.setText(sb.toString());
        }

        /** 保存/导出前：应用编辑区 → §3.4 校验 → 规范化（name 缺省、id=下标+1）；失败弹窗返回 null */
        private LevelWaves toLevelWaves() {
            if (currentGroup() != null && !applyGroupEdits()) return null;
            List<String> errors = validate();
            if (!errors.isEmpty()) {
                error("校验失败（未保存）", "共 " + errors.size() + " 处：\n" + String.join("\n", errors));
                return null;
            }
            if (waves.getName() == null || waves.getName().isBlank()) {
                waves.setName(currentMapKey + "_waves");
            }
            for (int i = 0; i < waves.getWaves().size(); i++) {
                waves.getWaves().get(i).setId(i + 1);   // 数组顺序即波次顺序，id 规范化为 k
            }
            return waves;
        }

        // ================= 载入 / 保存 JSON =================
        private void saveToGameResources() {
            LevelWaves out = toLevelWaves();
            if (out == null) return;
            if (!MapLibrary.writeWaves(currentMapKey, out)) {
                statusLabel.setText("未找到 src/main/resources（请以仓库根目录运行），已改为“另存为”");
                saveAsJson();
                return;
            }
            File f = MapLibrary.wavesFile(currentMapKey);
            int totalGroups = 0;
            for (Wave w : out.getWaves()) totalGroups += w.getGroups().size();
            statusLabel.setText("已保存到 " + (f == null ? currentMapKey : f.getPath())
                    + "（" + out.getWaves().size() + " 波 / " + totalGroups + " 组，校验通过；重启游戏生效）");
        }

        private void saveAsJson() {
            LevelWaves out = toLevelWaves();
            if (out == null) return;
            FileChooser fc = jsonChooser("另存为波次 JSON", true);
            File file = fc.showSaveDialog(null);
            if (file == null) return;
            if (!file.getName().toLowerCase().endsWith(".json")) {
                file = new File(file.getParentFile(), file.getName() + ".json");
            }
            try {
                Files.writeString(file.toPath(), out.toJson(), StandardCharsets.UTF_8);
                statusLabel.setText("已另存为 " + file.getPath() + "（游戏读取的是 maps/<key>/waves.json，此文件仅备份）");
            } catch (IOException ex) {
                error("保存失败", ex.getMessage());
            }
        }

        // ================= 工具 =================
        /** 类型下拉：词汇表 4 项 →「显示名（id）」；新敌人=LevelWaves 词汇表加一行后自动出现 */
        private void populateGroupTypes() {
            groupTypeToId.clear();
            groupTypeChoice.getItems().clear();
            for (Map.Entry<String, String> e : LevelWaves.VOCABULARY.entrySet()) {
                String disp = e.getValue() + "（" + e.getKey() + "）";
                groupTypeToId.put(disp, e.getKey());
                groupTypeChoice.getItems().add(disp);
            }
            if (!groupTypeChoice.getItems().isEmpty()) {
                groupTypeChoice.getSelectionModel().select(0);
            }
        }

        private String displayOfType(String id) {
            for (Map.Entry<String, String> e : groupTypeToId.entrySet()) {
                if (e.getValue().equals(id)) return e.getKey();
            }
            return null;
        }

        private static Integer parseInt(String text) {
            try {
                return Integer.parseInt(text == null ? "" : text.trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        private static Long parseLong(String text) {
            try {
                return Long.parseLong(text == null ? "" : text.trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        private FileChooser jsonChooser(String title, boolean save) {
            FileChooser fc = new FileChooser();
            fc.setTitle(title);
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("波次 JSON", "*.json"));
            return fc;
        }

        private void error(String title, String msg) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        }
    }
}
