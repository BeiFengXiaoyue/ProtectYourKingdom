package com.kingdom.game.controller;

import com.kingdom.game.config.GameConfig;
import com.kingdom.game.model.GameState;
import com.kingdom.game.model.TowerType;
import com.kingdom.game.model.enemy.Enemy;
import com.kingdom.game.model.enemy.NormalEnemy;
import com.kingdom.game.model.tower.Tower;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * GameController —— 后端核心控制器（具体类），实现 4 个 UI→后端接口；
 * 持有 3 个后端→UI 通知接口，由 Main/UI 层通过 setter 依赖注入。
 */
public class GameController implements ITowerBuilder, IWaveStarter, IGameLoop, IGameStateReader {

    private final GameState state;
    private final WaveManager waveManager;
    private final GameConfig config;

    private final List<Enemy> enemies = new ArrayList<>();

    // 后端 → UI 通知接口（setter 注入，可为空）
    private IStatusObserver statusObserver;
    private IRenderNotifier renderNotifier;
    private ITowerSelectionNotifier selectionNotifier;

    private boolean waveInProgress = false;

    public GameController(GameState state, WaveManager waveManager, GameConfig config) {
        this.state = state;
        this.waveManager = waveManager;
        this.config = config;
    }

    // ===== 通知接口注入（Main 组装阶段调用）=====
    public void setStatusObserver(IStatusObserver statusObserver) { this.statusObserver = statusObserver; }
    public void setRenderNotifier(IRenderNotifier renderNotifier) { this.renderNotifier = renderNotifier; }
    public void setSelectionNotifier(ITowerSelectionNotifier selectionNotifier) { this.selectionNotifier = selectionNotifier; }

    /** 组装完成后，把初始状态一次性推送给 HUD */
    public void refreshUI() {
        notifyGold(state.getGold());
        notifyLives(state.getLives());
        notifyMessage("点击「开始波次」开战！");
    }

    // ================= IGameLoop =================
    @Override
    public void update(long nanoTime) {
        if (state.isGameOver()) {          // 生命耗尽，冻结世界（仍允许重绘一次）
            if (renderNotifier != null) renderNotifier.requestRender();
            return;
        }

        waveManager.update(nanoTime, this::spawnEnemy);

        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            e.update();
            if (e.hasReachedEnd()) {            // 走到终点 → 扣生命
                it.remove();
                state.loseLives(1);
                notifyLives(state.getLives());
                if (state.isGameOver()) {
                    notifyMessage("游戏结束！所有生命值耗尽");
                    break;
                }
            } else if (!e.isAlive()) {          // 被击杀 → 赏金（Day2 起才有攻击方）
                it.remove();
                state.addGold(e.getGoldReward());
                notifyGold(state.getGold());
            }
        }

        // 一波结束：出怪完毕且场上清空 → 允许下一波
        if (!state.isGameOver() && waveInProgress && !waveManager.isSpawning() && enemies.isEmpty()) {
            waveInProgress = false;
            notifyMessage("第 " + state.getWave() + " 波结束，可开始下一波");
        }

        if (renderNotifier != null) renderNotifier.requestRender();
    }

    /** 生成一只敌人并加入战场（数值/路径取自 config） */
    private void spawnEnemy() {
        double[] px = config.getPathX();
        double[] py = config.getPathY();
        NormalEnemy enemy = new NormalEnemy(px[0], py[0],
                config.getNormalHp(), config.getNormalSpeed(), config.getNormalGoldReward());
        enemy.setPath(px.clone(), py.clone());
        enemies.add(enemy);
    }

    // ================= IWaveStarter =================
    @Override
    public void startNextWave() {
        if (!canStartNextWave()) return;
        state.advanceWave();
        waveManager.beginWave(config.getWaveEnemyCount(), config.getWaveSpawnIntervalMs());
        waveInProgress = true;
        notifyMessage("第 " + state.getWave() + " 波来袭！");
    }

    @Override
    public boolean canStartNextWave() {
        return !state.isGameOver() && !waveInProgress;
    }

    // ================= IGameStateReader =================
    @Override
    public int getCurrentGold() { return state.getGold(); }
    @Override
    public int getCurrentLives() { return state.getLives(); }
    @Override
    public int getCurrentWave() { return state.getWave(); }
    @Override
    public int getTotalWaves() { return state.getTotalWaves(); }
    @Override
    public List<Enemy> getEnemies() { return enemies; }

    // ================= ITowerBuilder（Day2 起实现）=================
    @Override
    public void placeTower(double x, double y, TowerType type) {
        // TODO Day2：校验金币并创建对应塔（本日竖切无建塔）
    }

    @Override
    public void upgradeTower(Tower tower) {
        // TODO Day6：升级塔
    }

    @Override
    public void sellTower(Tower tower) {
        // TODO Day6：出售返还 50% 投入
    }

    // ================= 内部通知小工具 =================
    private void notifyGold(int gold) {
        if (statusObserver != null) statusObserver.onGoldUpdated(gold);
    }

    private void notifyLives(int lives) {
        if (statusObserver != null) statusObserver.onLivesUpdated(lives);
    }

    private void notifyMessage(String message) {
        if (statusObserver != null) statusObserver.onWaveMessage(message);
    }
}
