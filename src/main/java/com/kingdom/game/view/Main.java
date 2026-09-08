package com.kingdom.game.view;

import com.kingdom.game.config.GameConfig;
import com.kingdom.game.controller.GameController;
import com.kingdom.game.controller.WaveManager;
import com.kingdom.game.model.GameState;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Main —— JavaFX 启动类，负责组装与依赖注入（唯一接线处）。
 * 开发期微调数值：改下面 config 的 setter 即可，无需动其他类。
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
        GameView view = new GameView(controller, controller, config);

        controller.setStatusObserver(hud);
        controller.setRenderNotifier(view);
        controller.refreshUI();               // 推送初始生命/金币/波次到 HUD

        BorderPane root = new BorderPane();
        root.setTop(hud.getNode());
        root.setCenter(view.getNode());

        Scene scene = new Scene(root);
        stage.setTitle("王国保卫战：前线哨站（Day 0/1 竖切）");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        view.startLoop();
    }
}
