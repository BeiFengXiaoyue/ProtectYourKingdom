package com.kingdom.game.view;

import com.kingdom.game.config.GameConfig;
import com.kingdom.game.controller.GameController;
import com.kingdom.game.controller.WaveManager;
import com.kingdom.game.model.GameState;
import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.TowerType;
import com.kingdom.game.model.tower.ArrowTower;
import com.kingdom.game.model.tower.Barrack;
import com.kingdom.game.model.tower.CannonTower;
import com.kingdom.game.model.tower.EliteArrowTower;
import com.kingdom.game.model.tower.EliteBarrack;
import com.kingdom.game.model.tower.EliteCannonTower;
import com.kingdom.game.model.tower.MasterArrowTower;
import com.kingdom.game.model.tower.MasterBarrack;
import com.kingdom.game.model.tower.MasterCannonTower;
import com.kingdom.game.util.asset.Assets;
import com.kingdom.game.util.asset.SizeTable;
import com.kingdom.game.util.audio.SoundManager;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
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
        // ===== 数值配置：改数值请编辑 src/main/resources/config/balance.json，重启生效 =====
        GameConfig config = new GameConfig();
        // 运行期瞬时覆盖（可选）：config.setInitialLives(30).setNormalStats(120, 80, 10);

        GameState state = new GameState(config);
        WaveManager waveManager = new WaveManager();
        GameController controller = new GameController(state, waveManager, config);

        // ===== UI 组装与依赖注入 =====
        HUD hud = new HUD(controller, controller);
        GameView view = new GameView(controller, controller, config, controller, controller, controller::resetGame);

        // 单位行为动画登记（外部叠加层）：单位类零改动；JSON 缺帧/缺文件时保持原渲染
        view.registerUnitAnimation("enemies", "NormalEnemy",
                "/assets/animations/enemies/normal_enemy.json");
        view.registerUnitAnimation("enemies", "FastEnemy",
                "/assets/animations/enemies/fast_enemy.json");
        view.registerUnitAnimation("enemies", "TankEnemy",
                "/assets/animations/enemies/tank_enemy.json");
        view.registerUnitAnimation("enemies", "BossEnemy",
                "/assets/animations/enemies/boss_enemy.json");
        view.registerUnitAnimation("allies", "Soldier",
                "/assets/animations/allies/soldier.json");
        // 三档士兵各有独立动画表（不共用 soldier.json，见《接口契约与抽象类说明》§4.7）：
        // 登记后 GameView 跳过静态渲染、改画本档动画帧，再叠 renderPostAnim 的等级标记
        view.registerUnitAnimation("allies", "EliteSoldier",
                "/assets/animations/allies/elite_soldier.json");
        view.registerUnitAnimation("allies", "RoyalSoldier",
                "/assets/animations/allies/royal_soldier.json");
        view.registerUnitAnimation("towers", "ArrowTower",
                "/assets/animations/towers/arrow_tower.json");

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
        // 塔登记（防御塔负责人交付 ArrowTower / CannonTower / Barrack）：
        controller.registerTower(TowerType.ARROW,
                new TowerSpec(TowerType.ARROW, "箭塔", ArrowTower.BUILD_COST),
                ArrowTower::new);
        controller.registerTower(TowerType.CANNON,
                new TowerSpec(TowerType.CANNON, "炮塔", CannonTower.BUILD_COST),
                CannonTower::new);
        controller.registerTower(TowerType.BARRACK,
                new TowerSpec(TowerType.BARRACK, "兵营", Barrack.BUILD_COST),
                Barrack::new);
        // 升级链工厂登记（《防御塔子系统说明》§10）：不进建塔目录，仅供 upgradeTower 原位替换取下一级工厂
        // 覆盖 TowerType 三族的 2 级精英与 3 级大师（缺任一登记，对应等级升级会提示"下一级尚未开放"）
        controller.registerUpgradeFactory(TowerType.ARROW_ELITE, EliteArrowTower::new);
        controller.registerUpgradeFactory(TowerType.CANNON_ELITE, EliteCannonTower::new);
        controller.registerUpgradeFactory(TowerType.BARRACK_ELITE, EliteBarrack::new);
        // L2→L3 工厂（2-1）：不登记则 upgradeTower 取不到下一级工厂，会提示"下一级尚未开放"。
        // L3 尺寸由 assets/sizes.json 的 3 条 Master 条目驱动（48×48）。
        controller.registerUpgradeFactory(TowerType.ARROW_MASTER, MasterArrowTower::new);
        controller.registerUpgradeFactory(TowerType.CANNON_MASTER, MasterCannonTower::new);
        controller.registerUpgradeFactory(TowerType.BARRACK_MASTER, MasterBarrack::new);
        controller.refreshUI();               // 推送初始生命/金币/波次到 HUD

        Assets.preload();                     // 预加载贴图（无图自动跳过）
        SizeTable.getInstance().preload();    // 预加载实体尺寸表（缺文件回退默认，不阻断）

        // ===== 三屏装配：开始界面 → 选关界面 → 战场（HUD + GameView），互斥显示 =====
        // 战场屏沿用原结构（HUD 在上、GameView 居中）；根改为 StackPane 以便整屏切换
        BorderPane gameScreen = new BorderPane();
        gameScreen.setTop(hud.getNode());
        gameScreen.setCenter(view.getNode());

        StackPane shell = new StackPane();
        final boolean[] loopStarted = {false};

        // 世界运行的唯一开关：循环 + 循环闸门 + HUD 计时器 + 后端时间基准四者必须同进同退。
        // （原先散在 6 处手工拼装，漏一处就失配——例如 HUD 计时器没停，暂停中还能点「立即开始」真的推进波次）
        // controller.pause()/resume() 不可省：停 AnimationTimer 只让 update 不再被调用，
        // 后端两个"绝对时间"基准（波间倒计时、出怪锚点）仍按真实时间流逝，必须在恢复时整体后移。
        Runnable resumeWorld = () -> {
            controller.resume();      // 先把两个时间基准后移，再起循环（幂等；未暂停时无操作）
            if (!loopStarted[0]) {
                view.startLoop();
                loopStarted[0] = true;
            }
            hud.setPaused(false);
        };
        Runnable pauseWorld = () -> {
            controller.pause();       // 先登记暂停时刻，再停循环（幂等）
            view.stopLoop();
            loopStarted[0] = false;   // 闸门与循环同进同退，避免"忘了复位闸门 → 下次进关永不起循环"
            hud.setPaused(true);
        };

        // 进入战场：清掉上一局残留 → 切屏 → 恢复世界（菜单期间世界不推进）
        Runnable enterLevel = () -> {
            view.prepareForLevelEntry();
            showScreen(shell, gameScreen);
            resumeWorld.run();
        };

        // 暂停：终态不允许暂停——结束遮罩盖不住 HUD（HUD 在 BorderPane.top，不在 GameView 里），
        // 只能在动作里挡，否则会留下"遮罩还在、循环却被停"的组合；已暂停则幂等返回
        Runnable pauseGame = () -> {
            if (controller.isGameOver() || controller.isVictory()) return;
            if (view.isPauseShown()) return;
            view.showPause();
            pauseWorld.run();
        };
        hud.setOnPause(pauseGame);

        // 选关界面：查询/动作全走接口（controller 同时实现 IGameStateReader 与 ILevelSwitcher）
        LevelSelectScreen levelSelectScreen = new LevelSelectScreen(controller, controller, enterLevel);
        // 开始界面：「开始游戏」先刷新关卡列表（打开时取一次，getLevels() 每次都会重解析 index.json）
        StartScreen startScreen = new StartScreen(() -> {
            levelSelectScreen.refresh();
            showScreen(shell, levelSelectScreen.getNode());
        });

        // 选关屏「← 返回」：二段接线。若把 startScreen 写进上面 LevelSelectScreen 的构造 lambda，
        // 会构成非法前向引用（两屏互相引用，任何一边写在前面都编译不过）。
        levelSelectScreen.setOnBack(() -> {
            showScreen(shell, startScreen.getNode());
            pauseWorld.run();   // 停世界：该路径现已可达（战场 → 暂停 → 返回选关 →「← 返回」）
        });

        // 战场暂停菜单的三个出口：继续 / 重新开始 → 恢复世界；返回选关 → 切屏 + 停世界
        view.setOnResume(resumeWorld);
        view.setOnExitToSelect(() -> {
            showScreen(shell, levelSelectScreen.getNode());
            levelSelectScreen.refresh();   // 按当前关卡列表重建磁贴（缩略图卡片；列表可能已变）
            pauseWorld.run();
        });

        shell.getChildren().addAll(startScreen.getNode(), levelSelectScreen.getNode(), gameScreen);
        pauseWorld.run();   // 起步停在菜单：世界先不跑（enterLevel 时恢复）
        showScreen(shell, startScreen.getNode());   // 启动先显示开始界面

        Scene scene = new Scene(shell);
        stage.setTitle("王国保卫战：前线哨站");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    /**
     * 三屏互斥显示：只切可见性，**不动 managed**——让 StackPane 的 pref 始终由战场屏（画布尺寸）
     * 决定，避免切到菜单时窗口跟着缩水；不可见节点不参与鼠标拾取，故隐藏的屏幕点不到。
     */
    private static void showScreen(StackPane shell, Node target) {
        for (Node n : shell.getChildren()) {
            n.setVisible(n == target);
        }
    }
}
