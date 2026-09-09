package com.kingdom.game.view;

import com.kingdom.game.config.GameConfig;
import com.kingdom.game.controller.GameController;
import com.kingdom.game.controller.WaveManager;
import com.kingdom.game.model.GameState;
import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.TowerType;
import com.kingdom.game.model.tower.ArrowTower;
import com.kingdom.game.util.Assets;
import com.kingdom.game.util.SoundManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Main —— JavaFX 启动类，负责组装与依赖注入（唯一接线处）。
 *
 * 分工接线说明：
 * - GameView 实现 4 个视觉特效接口 → 经 controller.setXxxFx(view) 注入；
 * - 音效接口（IWaveSound 等）由素材/音效负责人实现后在此注入 controller；
 * - 塔由防御塔负责人实现具体类后，在此 registerTower(...) 登记即可上架。
 */
public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        // ===== 数值配置（开发期可在此微调）=====
        GameConfig config = new GameConfig();
        // 例：config.setInitialLives(30).setNormalStats(120, 80, 10);

        GameState state = new GameState(config);
        WaveManager waveManager = new WaveManager();
        GameController controller = new GameController(state, waveManager, config);

        // ===== UI 组装与依赖注入 =====
        HUD hud = new HUD(controller, controller);
        GameView view = new GameView(controller, controller, config, controller, controller::resetGame);

        controller.setStatusObserver(hud);
        controller.setRenderNotifier(view);
        controller.setFloatingTextFx(view);   // GameView 实现视觉特效
        controller.setScreenFx(view);
        controller.setParticleFx(view);
        controller.setSelectionFx(view);
        // 音效注入（素材负责人 E）：SoundManager 一次实现 5 个音效接口
        SoundManager sounds = SoundManager.getInstance();
        controller.setWaveSound(sounds);
        controller.setTowerSound(sounds);
        controller.setUnitSound(sounds);
        controller.setCombatSound(sounds);
        controller.setEndSound(sounds);
        // 塔登记（防御塔负责人交付 ArrowTower）：
        controller.registerTower(TowerType.ARROW,
                new TowerSpec(TowerType.ARROW, "箭塔", ArrowTower.BUILD_COST),
                ArrowTower::new);
        controller.refreshUI();               // 推送初始生命/金币/波次到 HUD

        Assets.preload();                     // 预加载贴图（无图自动跳过）

        BorderPane root = new BorderPane();
        root.setTop(hud.getNode());
        root.setCenter(view.getNode());

        Scene scene = new Scene(root);
        stage.setTitle("王国保卫战：前线哨站");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        view.startLoop();
    }
}
