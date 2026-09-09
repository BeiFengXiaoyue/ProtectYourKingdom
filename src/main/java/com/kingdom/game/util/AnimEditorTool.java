package com.kingdom.game.util;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AnimEditorTool —— 敌人行为动画 JSON 编辑器（启动入口，不继承 Application）。
 * 真正的窗口在嵌套类 {@link AnimEditorApp}；入口经 Application.launch 转发，
 * 避免 IntelliJ 普通 Run 配置下“缺少 JavaFX 运行时组件”报错。
 *
 * 功能（经 AnimLibrary 按敌人名管理，同名去重）：
 * - 按敌人名载入（存在=继续修改，不存在=新建模板 idle/walk/attack）；
 * - 行为模式可添加/删除；每个模式可逐张“选图添加关键帧”（自动拷入 assets/enemies/&lt;名&gt;/），支持上移/下移/删除；
 * - interval（每帧停留 tick）、轮播预览；
 * - 保存 → assets/animations/&lt;敌人名&gt;.json（同名覆盖）；删除该敌人。
 */
public final class AnimEditorTool {

    private AnimEditorTool() {
    }

    public static void main(String[] args) {
        Application.launch(AnimEditorApp.class, args);
    }

    /** 真正的 JavaFX 应用（public static，供 launch 反射） */
    public static final class AnimEditorApp extends Application {

        /** 项目中已存在的敌人 Java 类名（便于“按已有敌人类名新建”动画表） */
        private static final String[] KNOWN_ENEMY_CLASSES = {"NormalEnemy", "FastEnemy", "TankEnemy", "BossEnemy"};
        private static final String CUSTOM_ITEM = "…手动输入其他名字";

        private AnimTable table = AnimTable.template("");

        private final TextField enemyNameField = new TextField("enemy");
        private final ChoiceBox<String> enemyChoice = new ChoiceBox<>();
        private final Map<String, String> choiceToName = new LinkedHashMap<>();
        private final TextField intervalField = new TextField("6");
        private final ListView<String> modeList = new ListView<>();
        private final ListView<String> frameList = new ListView<>();
        private final ImageView previewView = new ImageView();
        private final Label statusLabel = new Label("输入敌人名后「按名载入」或新建即可开始编辑。");
        private final Label previewLabel = new Label("（无预览素材）");

        // 轮播预览
        private AnimationTimer playTimer;
        private final List<Image> playImages = new ArrayList<>();
        private int playIndex = 0;
        private long playTick = 0;
        private boolean playing = false;

        @Override
        public void start(Stage stage) {
            BorderPane root = new BorderPane();
            root.setTop(buildTopBar());
            BorderPane center = new BorderPane();
            center.setLeft(buildModePanel());
            center.setCenter(buildPreviewArea());
            center.setRight(buildFramePanel());
            root.setCenter(center);
            root.setBottom(statusLabel);

            Scene scene = new Scene(root, 1000, 620);
            stage.setTitle("王国保卫战 · 敌人动画编辑器");
            stage.setScene(scene);
            stage.show();

            enemyNameField.setOnAction(e -> loadByName());
            enemyChoice.setOnAction(e -> onEnemyChoice());
            populateEnemyChoices();
            modeList.getSelectionModel().selectedItemProperty()
                    .addListener((obs, o, n) -> onModeSelected());
            frameList.getSelectionModel().selectedItemProperty()
                    .addListener((obs, o, n) -> refreshPreview());

            refreshAll();
        }

        // ================= UI 构建 =================
        private HBox buildTopBar() {
            Button loadName = new Button("按名载入");
            loadName.setOnAction(e -> loadByName());
            Button openJson = new Button("打开 JSON…");
            openJson.setOnAction(e -> openJsonFile());
            Button save = new Button("保存");
            save.setOnAction(e -> saveToGame());
            Button saveAs = new Button("另存为…");
            saveAs.setOnAction(e -> saveAsJson());
            Button del = new Button("删除该敌人");
            del.setOnAction(e -> deleteEnemy());

            enemyNameField.setPrefWidth(150);
            enemyChoice.setPrefWidth(210);
            intervalField.setPrefWidth(50);

            HBox bar = new HBox(8, new Label("敌人(表/类):"), enemyChoice,
                    new Label("名字:"), enemyNameField,
                    new Label("interval:"), intervalField,
                    loadName, openJson, save, saveAs, del);
            bar.setPadding(new Insets(8));
            bar.setAlignment(Pos.CENTER_LEFT);
            bar.setStyle("-fx-background-color: #e0e0e0;");
            return bar;
        }

        private VBox buildModePanel() {
            modeList.setPrefWidth(140);
            modeList.setPrefHeight(400);
            Button add = new Button("+ 添加模式");
            add.setOnAction(e -> addMode());
            Button del = new Button("- 删除模式");
            del.setOnAction(e -> removeMode());
            VBox box = new VBox(8, new Label("行为模式"), modeList, add, del);
            box.setPadding(new Insets(10));
            return box;
        }

        private VBox buildPreviewArea() {
            previewView.setFitWidth(140);
            previewView.setFitHeight(140);
            previewView.setPreserveRatio(true);
            previewView.setStyle("-fx-background-color: #fafafa; -fx-border-color: #ccc;");

            Button play = new Button("轮播预览");
            play.setOnAction(e -> togglePlay());

            previewLabel.setWrapText(true);
            previewLabel.setFont(Font.font(12));

            VBox box = new VBox(10, previewLabel, previewView, play);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(12));
            VBox.setVgrow(box, Priority.ALWAYS);
            return box;
        }

        private VBox buildFramePanel() {
            frameList.setPrefWidth(260);
            frameList.setPrefHeight(360);

            Button add = new Button("+ 添加关键帧(选图)");
            add.setOnAction(e -> addFrame());
            Button up = new Button("上移");
            up.setOnAction(e -> moveFrame(-1));
            Button down = new Button("下移");
            down.setOnAction(e -> moveFrame(1));
            Button del = new Button("- 删除该帧");
            del.setOnAction(e -> removeFrame());

            VBox box = new VBox(8, new Label("当前模式的关键帧"), frameList,
                    new HBox(6, add), new HBox(6, up, down, del));
            box.setPadding(new Insets(10));
            return box;
        }

        // ================= 敌人下拉（已有表 / 已有敌人类名新建）=================
        /** 刷新下拉：已有动画表 + 已存在的敌人类名 + 手动输入项 */
        private void populateEnemyChoices() {
            choiceToName.clear();
            enemyChoice.getItems().clear();
            List<String> existing = AnimLibrary.listEnemies();

            for (String cls : KNOWN_ENEMY_CLASSES) {
                String key = camelToSnake(cls);
                boolean has = existing.contains(key);
                String disp = cls + (has ? "（已有）" : "（新建模板）");
                choiceToName.put(disp, key);
                enemyChoice.getItems().add(disp);
            }
            for (String n : existing) {
                boolean covered = false;
                for (String cls : KNOWN_ENEMY_CLASSES) {
                    if (camelToSnake(cls).equals(n)) { covered = true; break; }
                }
                if (!covered) {
                    String disp = n + "（已有·自定义）";
                    choiceToName.put(disp, n);
                    enemyChoice.getItems().add(disp);
                }
            }
            enemyChoice.getItems().add(CUSTOM_ITEM);
        }

        /** 下拉选择：已有表→载入继续修改；敌人类名无表→按类名新建模板 */
        private void onEnemyChoice() {
            String disp = enemyChoice.getValue();
            if (disp == null) return;
            String key = choiceToName.get(disp);
            if (key == null) {           // 手动输入项
                enemyNameField.requestFocus();
                return;
            }
            enemyNameField.setText(key);
            if (AnimLibrary.exists(key)) {
                loadByName();
                status("下拉载入已有敌人「" + key + "」（同名覆盖修改）");
            } else {
                adopt(AnimTable.template(key));
                status("已按敌人类名新建模板「" + key + "」");
            }
        }

        /** NormalEnemy → normal_enemy；FastEnemy → fast_enemy */
        private static String camelToSnake(String s) {
            if (s == null || s.isEmpty()) return s;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (Character.isUpperCase(c)) {
                    if (i > 0) sb.append('_');
                    sb.append(Character.toLowerCase(c));
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        // ================= 数据操作 =================
        private String currentName() {
            String n = enemyNameField.getText() == null ? "" : enemyNameField.getText().trim();
            return n.replaceAll("[^A-Za-z0-9_\\-]", "_");
        }

        private String currentMode() {
            return modeList.getSelectionModel().getSelectedItem();
        }

        private void loadByName() {
            String name = currentName();
            if (name.isEmpty()) {
                status("敌人名不能为空");
                return;
            }
            AnimTable t = AnimLibrary.read(name);
            if (t == null) {
                t = AnimTable.template(name);
                status("未找到「" + name + "」，已新建模板（idle/walk/attack）");
            } else {
                status("已载入「" + name + "」（同名覆盖修改）");
            }
            adopt(t);
        }

        private void adopt(AnimTable t) {
            table = t == null ? AnimTable.template("") : t;
            enemyNameField.setText(table.getName());
            intervalField.setText(String.valueOf(table.getInterval()));
            stopPlay();
            refreshAll();
        }

        private void openJsonFile() {
            FileChooser fc = jsonChooser("打开动画 JSON", false);
            File f = fc.showOpenDialog(null);
            if (f == null) return;
            try {
                AnimTable t = AnimTable.fromJson(Files.readString(f.toPath(), StandardCharsets.UTF_8));
                adopt(t);
                status("已从文件载入：" + f.getPath());
            } catch (IOException | RuntimeException ex) {
                error("载入失败", ex.getMessage());
            }
        }

        private void saveToGame() {
            if (!applyNameAndInterval()) return;
            File f = AnimLibrary.save(table);
            if (f == null) {
                status("未找到 src/main/resources（请以仓库根目录运行），已改为另存为");
                saveAsJson();
                return;
            }
            populateEnemyChoices();
            status("已保存到 " + f.getPath() + "（按名去重）");
        }

        private void saveAsJson() {
            applyNameAndInterval();
            FileChooser fc = jsonChooser("另存为动画 JSON", true);
            File f = fc.showSaveDialog(null);
            if (f == null) return;
            if (!f.getName().toLowerCase().endsWith(".json")) {
                f = new File(f.getParentFile(), f.getName() + ".json");
            }
            try {
                Files.writeString(f.toPath(), table.toJson(), StandardCharsets.UTF_8);
                status("已另存为 " + f.getPath());
            } catch (IOException ex) {
                error("保存失败", ex.getMessage());
            }
        }

        private boolean applyNameAndInterval() {
            String name = currentName();
            if (name.isEmpty()) {
                status("敌人名不能为空");
                return false;
            }
            int interval = 6;
            try {
                interval = Integer.parseInt(intervalField.getText().trim());
            } catch (NumberFormatException ignored) {
                status("interval 需为整数，已用默认 6");
            }
            table.setName(name);
            table.setInterval(interval);
            enemyNameField.setText(name);
            intervalField.setText(String.valueOf(interval));
            return true;
        }

        private void deleteEnemy() {
            String name = currentName();
            if (name.isEmpty()) return;
            boolean ok = AnimLibrary.delete(name);
            status(ok ? "已删除「" + name + "」的动画表" : "未找到可删除的动画表「" + name + "」");
            adopt(AnimTable.template(""));
            populateEnemyChoices();
        }

        // ================= 模式 =================
        private void addMode() {
            TextInputDialog dlg = new TextInputDialog("walk2");
            dlg.setTitle("添加行为模式");
            dlg.setHeaderText("新模式名");
            dlg.setContentText("模式名（字母数字下划线）：");
            dlg.showAndWait().ifPresent(m -> {
                String mode = m.trim().replaceAll("[^A-Za-z0-9_\\-]", "_");
                if (mode.isEmpty() || !table.addMode(mode)) {
                    status("添加失败：空名或已存在「" + mode + "」");
                } else {
                    status("已添加模式「" + mode + "」");
                }
                refreshModeList(mode);
            });
        }

        private void removeMode() {
            String mode = currentMode();
            if (mode == null) return;
            if (!table.getFrames(mode).isEmpty()) {
                status("模式「" + mode + "」还有帧，请先清空再删除");
                return;
            }
            table.removeMode(mode);
            status("已删除模式「" + mode + "」");
            refreshModeList(null);
        }

        // ================= 帧 =================
        private void addFrame() {
            String name = currentName();
            String mode = currentMode();
            if (name.isEmpty() || mode == null) {
                status("请先输入敌人名并选中一个行为模式");
                return;
            }
            File dir = AnimLibrary.enemyFramesDir(name);
            if (dir == null) {
                status("未找到 src/main/resources（请以仓库根目录运行）");
                return;
            }
            FileChooser fc = new FileChooser();
            fc.setTitle("选择关键帧图片（第 " + (table.getFrames(mode).size() + 1) + " 张）");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("图片", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
            File src = fc.showOpenDialog(null);
            if (src == null) return;

            if (!dir.exists() && !dir.mkdirs()) {
                error("拷贝失败", "无法创建目录 " + dir);
                return;
            }
            int idx = nextFrameIndex(dir, mode);
            String ext = extensionOf(src.getName());
            String fileName = mode + "_" + idx + ext;
            File dst = new File(dir, fileName);
            try {
                Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                error("拷贝失败", ex.getMessage());
                return;
            }
            table.addFrame(mode, "enemies/" + name + "/" + fileName);
            status("已添加帧：enemies/" + name + "/" + fileName);
            refreshFrameList();
        }

        private void removeFrame() {
            String mode = currentMode();
            int idx = frameList.getSelectionModel().getSelectedIndex();
            if (mode == null || !table.removeFrameAt(mode, idx)) return;
            status("已删除帧 #" + idx);
            refreshFrameList();
        }

        private void moveFrame(int delta) {
            String mode = currentMode();
            int from = frameList.getSelectionModel().getSelectedIndex();
            if (mode == null || from < 0) return;
            table.moveFrame(mode, from, from + delta);
            refreshFrameList();
            frameList.getSelectionModel().select(from + delta);
        }

        private int nextFrameIndex(File dir, String mode) {
            int max = -1;
            File[] files = dir.listFiles((d, n) -> n.startsWith(mode + "_"));
            if (files != null) {
                for (File f : files) {
                    String n = f.getName();
                    try {
                        int idx = Integer.parseInt(n.substring(mode.length() + 1,
                                n.lastIndexOf('.')));
                        max = Math.max(max, idx);
                    } catch (Exception ignored) {
                        // 非数字序号忽略
                    }
                }
            }
            return max + 1;
        }

        private String extensionOf(String fileName) {
            int dot = fileName.lastIndexOf('.');
            String ext = dot >= 0 ? fileName.substring(dot).toLowerCase() : ".png";
            return ext.equals(".jpg") ? ".jpg" : ext;
        }

        // ================= 预览 =================
        private void onModeSelected() {
            refreshFrameList();
        }

        private void refreshAll() {
            refreshModeList(null);
            refreshPreview();
        }

        private void refreshModeList(String select) {
            modeList.getItems().setAll(table.getModeNames());
            if (select != null && table.hasMode(select)) {
                modeList.getSelectionModel().select(select);
            } else if (!modeList.getItems().isEmpty()) {
                modeList.getSelectionModel().select(0);
            }
            refreshFrameList();
        }

        private void refreshFrameList() {
            String mode = currentMode();
            if (mode == null) {
                frameList.getItems().clear();
            } else {
                frameList.getItems().setAll(table.getFrames(mode));
            }
            refreshPreview();
        }

        private void refreshPreview() {
            stopPlay();
            String mode = currentMode();
            if (mode == null) {
                previewView.setImage(null);
                previewLabel.setText("（先选一个行为模式）");
                return;
            }
            List<String> rels = table.getFrames(mode);
            if (rels.isEmpty()) {
                previewView.setImage(null);
                previewLabel.setText("模式「" + mode + "」暂无帧");
                return;
            }
            Image img = loadImage(rels.get(0));
            previewView.setImage(img);
            previewLabel.setText(img == null ? "缺少素材：" + rels.get(0) : "模式「" + mode + "」共 " + rels.size() + " 帧");
        }

        private void togglePlay() {
            if (playing) {
                stopPlay();
                return;
            }
            String mode = currentMode();
            if (mode == null) return;
            playImages.clear();
            for (String rel : table.getFrames(mode)) {
                Image img = loadImage(rel);
                if (img != null) playImages.add(img);
            }
            if (playImages.isEmpty()) {
                status("当前模式没有可加载的素材，无法预览");
                return;
            }
            playing = true;
            playIndex = 0;
            playTick = 0;
            previewView.setImage(playImages.get(0));
            int interval = table.getInterval();
            playTimer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    playTick++;
                    if (playTick >= interval && playImages.size() > 1) {
                        playTick = 0;
                        playIndex = (playIndex + 1) % playImages.size();
                        previewView.setImage(playImages.get(playIndex));
                    }
                }
            };
            playTimer.start();
            status("轮播预览中（interval=" + interval + "）");
        }

        private void stopPlay() {
            playing = false;
            if (playTimer != null) {
                playTimer.stop();
                playTimer = null;
            }
        }

        private Image loadImage(String relative) {
            File f = AnimLibrary.resolveFrameFile(relative);
            return f == null ? null : new Image(f.toURI().toString());
        }

        // ================= 工具 =================
        private FileChooser jsonChooser(String title, boolean save) {
            FileChooser fc = new FileChooser();
            fc.setTitle(title);
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("动画 JSON", "*.json"));
            return fc;
        }

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
