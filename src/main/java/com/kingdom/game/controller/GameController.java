package com.kingdom.game.controller;

import com.kingdom.game.config.GameConfig;
import com.kingdom.game.model.GameObject;
import com.kingdom.game.model.GameState;
import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.TowerType;
import com.kingdom.game.model.ally.Ally;
import com.kingdom.game.model.ally.Soldier;
import com.kingdom.game.model.enemy.BossEnemy;
import com.kingdom.game.model.enemy.Enemy;
import com.kingdom.game.model.enemy.FastEnemy;
import com.kingdom.game.model.enemy.NormalEnemy;
import com.kingdom.game.model.enemy.TankEnemy;
import com.kingdom.game.model.projectile.Projectile;
import com.kingdom.game.model.tower.Barrack;
import com.kingdom.game.model.tower.ITowerUpgrade;
import com.kingdom.game.model.tower.Tower;
import com.kingdom.game.util.map.LevelWaves;
import com.kingdom.game.util.map.MapLibrary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * GameController —— 控制层核心（具体类），实现 4 个 UI→后端接口。
 *
 * 职责（唯一归属）：
 * - 持有敌/塔/投射物/友方注册表；
 * - 每帧统一驱动 update、碰撞检测、命中结算与列表移除；
 * - 金币/生命结算唯一在此发生；
 * - 向实体注入事件通道(attachEffects)，并持有各事件接口做控制层级通知。
 */
public class GameController implements ITowerBuilder, IWaveStarter, IGameLoop, IGameStateReader {

    private static final double PATH_CLEARANCE = 30;   // 塔中心离路径中线的最近距离下限
    private static final double TOWER_SPACING = 40;    // 塔之间最小间距
    private static final int BOSS_STOMP_STUN_MS = 3000; // Boss 震地 → 全塔眩晕时长（《整改方案》§7 P1-3）

    private final GameState state;
    private final WaveManager waveManager;
    private final GameConfig config;

    // ===== 实体注册表 =====
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Tower> towers = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Ally> allies = new ArrayList<>();

    // 塔工厂注册表：type -> (x,y) -> Tower（由装配处/防御塔负责人登记）
    private final Map<TowerType, BiFunction<Double, Double, Tower>> towerFactories = new HashMap<>();

    // ===== UI 通知接口（注入可为空）=====
    private IStatusObserver statusObserver;
    private IRenderNotifier renderNotifier;
    private ITowerSelectionNotifier selectionNotifier;

    // ===== 音效/视觉事件接口（注入可为空；实体侧缺省 FxNop）=====
    private IWaveSound waveSound;
    private ITowerSound towerSound;
    private IUnitSound unitSound;
    private ICombatSound combatSound;
    private IEndSound endSound;
    private IFloatingTextFx fxText;
    private IScreenFx fxScreen;
    private IParticleFx fxParticle;
    private ISelectionFx fxSelection;

    private boolean waveInProgress = false;

    /** 波间倒计时截止时刻（System.nanoTime）；<0 表示当前不在倒计时 */
    private long countdownDeadlineNanos = -1;

    /** 本次倒计时的总时长（ms）：下一波 maxPrepMs ≥0 用之，否则全局默认；提前奖励比例与其同源（§3.2/§6.2.4） */
    private long activeIntermissionMs = 0;

    public GameController(GameState state, WaveManager waveManager, GameConfig config) {
        this.state = state;
        this.waveManager = waveManager;
        this.config = config;
    }

    // ===== 事件接口注入（D/素材层接线）=====
    public void setStatusObserver(IStatusObserver statusObserver) { this.statusObserver = statusObserver; }
    public void setRenderNotifier(IRenderNotifier renderNotifier) { this.renderNotifier = renderNotifier; }
    public void setSelectionNotifier(ITowerSelectionNotifier selectionNotifier) { this.selectionNotifier = selectionNotifier; }
    public void setWaveSound(IWaveSound waveSound) { this.waveSound = waveSound; }
    public void setTowerSound(ITowerSound towerSound) { this.towerSound = towerSound; }
    public void setUnitSound(IUnitSound unitSound) { this.unitSound = unitSound; }
    public void setCombatSound(ICombatSound combatSound) { this.combatSound = combatSound; }
    public void setEndSound(IEndSound endSound) { this.endSound = endSound; }
    public void setFloatingTextFx(IFloatingTextFx fxText) { this.fxText = fxText; }
    public void setScreenFx(IScreenFx fxScreen) { this.fxScreen = fxScreen; }
    public void setParticleFx(IParticleFx fxParticle) { this.fxParticle = fxParticle; }
    public void setSelectionFx(ISelectionFx fxSelection) { this.fxSelection = fxSelection; }

    /** 组装完成后，把初始状态一次性推送给 HUD */
    public void refreshUI() {
        notifyGold(state.getGold());
        notifyLives(state.getLives());
        notifyMessage("点击「开始波次」开战！");
    }

    // ===== 塔工厂注册（A：防御塔负责人交付后在此登记）=====
    public void registerTower(TowerType type, TowerSpec spec, BiFunction<Double, Double, Tower> factory) {
        config.addTowerSpec(spec);
        towerFactories.put(type, factory);
    }

    /**
     * 登记升级链工厂（《防御塔子系统说明》§10）：只进工厂表、不进 config.towerSpecs，
     * 升级级塔不会出现在建塔目录；供 upgradeTower 原位替换取下一级工厂。
     */
    public void registerUpgradeFactory(TowerType type, BiFunction<Double, Double, Tower> factory) {
        if (type != null && factory != null) towerFactories.put(type, factory);
    }

    // ================= IGameLoop =================
    @Override
    public void update(long nanoTime) {
        if (state.isGameOver()) {          // 生命耗尽，冻结世界（仍允许重绘一次）
            if (renderNotifier != null) renderNotifier.requestRender();
            return;
        }

        // 波间倒计时到期 → 自动开下一波（剩余≤0，无提前奖励；倒计时统一用 System.nanoTime 时钟）
        if (countdownDeadlineNanos > 0 && System.nanoTime() >= countdownDeadlineNanos) {
            doStartWave(countdownDeadlineNanos);
        }

        waveManager.update(nanoTime, this::spawnEnemy);

        // Boss 震地结算（须在塔循环之前：本帧眩晕立即生效）
        settleBossStompRequests();

        // 塔：索敌开火（A 交付具体塔后生效）
        for (Tower t : new ArrayList<>(towers)) {
            t.update();
            t.tryAttack(enemies);
        }

        // 投射物：飞行 → 命中后调用 onHit 并移除（B 交付后生效）
        for (Projectile p : new ArrayList<>(projectiles)) {
            p.update();
            if (p.hasHit()) {
                p.onHit(enemies);
                projectiles.remove(p);
            }
        }

        // 友方：索敌 + 移动/AI（契约帧序的"友方 AI"位：视野内最近敌人喂给士兵，无目标时空转）
        for (Ally a : new ArrayList<>(allies)) {
            a.engage(a.findTarget(enemies));
            a.update();
        }

        // 敌我近战碰撞（拦截/反击）
        processEntityCollisions();

        // 敌人：更新并结算（到达终点扣命 / 死亡赏金）
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            e.update();
            if (e.hasReachedEnd()) {            // 走到终点 → 扣 1 生命
                it.remove();
                state.loseLives(1);
                notifyLives(state.getLives());
                if (state.isGameOver()) {
                    if (endSound != null) endSound.onGameOver();
                    notifyMessage("游戏结束！所有生命值耗尽");
                    break;
                }
            } else if (!e.isAlive()) {          // 被击杀 → 赏金
                it.remove();
                state.addGold(e.getGoldReward());
                notifyGold(state.getGold());
            }
        }

        // 友方死亡清理
        allies.removeIf(a -> !a.isAlive());

        // 一波结束：出怪完毕且场上清空 → 允许下一波 / 判定胜利
        // 生命耗尽是终局，优先于"本波结束"：最后一个敌人进家扣光生命时，本帧只出失败信号
        if (!state.isGameOver() && waveInProgress && !waveManager.isSpawning() && enemies.isEmpty()) {
            waveInProgress = false;
            if (state.getWave() >= state.getTotalWaves()) {
                if (endSound != null) endSound.onVictory();
                notifyMessage("胜利！所有波次已击退！");
            } else {
                // 下一波准备时长：波次表该波 maxPrepMs ≥0 用之，否则全局默认（§3.2/§6.2.4）
                activeIntermissionMs = nextIntermissionMs();
                countdownDeadlineNanos = System.nanoTime() + activeIntermissionMs * 1_000_000L;
                notifyMessage("第 " + state.getWave() + " 波结束，可开始下一波");
            }
        }

        if (renderNotifier != null) renderNotifier.requestRender();
    }

    /**
     * Boss 震地结算：实体只把请求记在 {@link BossEnemy#consumeStompRequest()} 标记里，
     * 控制层每帧轮询一次，命中则全塔眩晕 3 秒（契约：实体不改全局列表，结算权归 GameController）。
     * 触发节奏：Boss 半血狂暴后，每 {@code bossStompIntervalMs}（默认 15000ms）置位一次请求。
     */
    private void settleBossStompRequests() {
        for (Enemy e : enemies) {
            if (e instanceof BossEnemy && ((BossEnemy) e).consumeStompRequest()) {
                for (Tower t : towers) t.stun(BOSS_STOMP_STUN_MS);
            }
        }
    }

    /** 敌我近战碰撞检测（O(n*m)，规模小可接受；后续可换网格优化） */
    private void processEntityCollisions() {
        for (Enemy e : enemies) {
            if (!e.isCollisionEnabled()) continue;
            for (Ally a : allies) {
                if (!a.isCollisionEnabled()) continue;
                double dist = Math.hypot(e.getX() - a.getX(), e.getY() - a.getY());
                if (dist < e.getCollisionRadius() + a.getCollisionRadius()) {
                    e.onCollision(a);
                    a.onCollision(e);
                }
            }
        }
    }

    /**
     * 生成一只敌人并加入战场（规范 §6.2.2/§4）：按词汇表 id 建对应类型，
     * 数值从 GameConfig 按 id 槽位现取；出生点=路径起点。
     */
    private void spawnEnemy(String typeId) {
        double[] px = config.getPathX();
        double[] py = config.getPathY();
        Enemy enemy;
        switch (typeId) {
            case "fast_enemy":
                enemy = new FastEnemy(px[0], py[0],
                        config.getFastHp(), config.getFastSpeed(), config.getFastGoldReward(),
                        config.getFastAttackDamage(), config.getFastAttackCooldownMs());
                break;
            case "tank_enemy":
                enemy = new TankEnemy(px[0], py[0],
                        config.getTankHp(), config.getTankSpeed(), config.getTankGoldReward(),
                        config.getTankAttackDamage(), config.getTankAttackCooldownMs());
                break;
            case "boss_enemy":
                enemy = new BossEnemy(px[0], py[0],
                        config.getBossHp(), config.getBossSpeed(), config.getBossGoldReward(),
                        config.getBossAttackDamage(), config.getBossAttackCooldownMs());
                break;
            case "normal_enemy":
                enemy = new NormalEnemy(px[0], py[0],
                        config.getNormalHp(), config.getNormalSpeed(), config.getNormalGoldReward(),
                        config.getNormalAttackDamage(), config.getNormalAttackCooldownMs());
                break;
            default:   // 编辑器已拦截未知 id（§3.4），此为防御性兜底：按普通敌人处理
                System.err.println("[GameController] 未知敌人 id：" + typeId + "，按普通敌人兜底");
                enemy = new NormalEnemy(px[0], py[0],
                        config.getNormalHp(), config.getNormalSpeed(), config.getNormalGoldReward(),
                        config.getNormalAttackDamage(), config.getNormalAttackCooldownMs());
        }
        enemy.setPath(px.clone(), py.clone());
        attachFx(enemy);
        enemies.add(enemy);
    }

    /**
     * 把一波的出怪组按 §3.3 时间轴展开为逐只出怪队列：
     * 第 1 只于本波开始（T+0）出生，此后每一只 = 上一只出生时刻 +「它所在组的 intervalMs」
     * （跨组空隙 = 后一组自身的间隔，无缝接力；与规范示例表/工具时间轴预览同一条公式）。
     */
    private List<WaveManager.SpawnOrder> expandOrders(LevelWaves.Wave wave) {
        List<WaveManager.SpawnOrder> orders = new ArrayList<>();
        long lastNanos = -1;
        for (LevelWaves.SpawnGroup group : wave.getGroups()) {
            long stepNanos = group.getIntervalMs() * 1_000_000L;
            for (int i = 0; i < group.getCount(); i++) {
                long at = lastNanos < 0 ? 0 : lastNanos + stepNanos;
                orders.add(new WaveManager.SpawnOrder(group.getType(), at));
                lastNanos = at;
            }
        }
        return orders;
    }

    /** 下一波的准备时长（ms，§3.2/§6.2.4）：波次表下一波 maxPrepMs ≥ 0 用之，否则全局默认 */
    private long nextIntermissionMs() {
        LevelWaves waves = config.getLevelWaves();
        if (waves != null && state.getWave() < waves.getWaves().size()) {
            long prepMs = waves.getWaves().get(state.getWave()).getMaxPrepMs();
            if (prepMs >= 0) return prepMs;
        }
        return config.getWaveIntermissionMs();
    }

    /** 向实体注入事件通道（注册/放置时调用） */
    private void attachFx(GameObject obj) {
        obj.attachEffects(combatSound, fxText, fxScreen, fxParticle, fxSelection);
    }

    /** 建塔/升级共用接线（§11 一致性要求）：投射物出口（含投射物事件通道）+ 塔事件槽 + 兵营友方出口 */
    private void injectTowerChannels(Tower tower) {
        // 投射物入注册表时注入事件通道：命中音效/飘字经事件槽发出（缺注入则静默落到 FxNop）
        tower.setProjectileSink(p -> {
            attachFx(p);
            projectiles.add(p);
        });
        attachFx(tower);
        // 兵营：注入“友方出口”→ 产出的士兵加入 allies 注册表
        if (tower instanceof Barrack) {
            ((Barrack) tower).setAllySink(this::addAlly);
        }
    }

    /** 友方（士兵等）入战场：注入事件通道 + 驻守点 + 加入注册表 + 出兵音效 */
    public void addAlly(Ally ally) {
        if (ally == null) return;
        attachFx(ally);
        if (ally instanceof Soldier) {   // 士兵：驻守点=离兵营最近的路径点（上路拦截；仿 placeTower 的 Barrack 装配先例）
            double[] g = nearestPathPoint(ally.getX(), ally.getY());
            if (g != null) ((Soldier) ally).setGuardPoint(g[0], g[1]);
        }
        allies.add(ally);
        if (unitSound != null) unitSound.onUnitSpawned(ally);
    }

    // ================= IWaveStarter =================
    @Override
    public void startNextWave() {
        if (!canStartNextWave()) return;
        doStartWave(System.nanoTime());
    }

    /** 开一波：先按剩余时间结算提前奖励（自动开波时剩余≤0 无奖励），再登记出怪节奏 */
    private void doStartWave(long nowNanos) {
        int bonus = earlyStartBonusAt(nowNanos);
        countdownDeadlineNanos = -1;                  // 开波即清倒计时（防重复触发）
        state.advanceWave();
        // 出怪队列（§6.2.3）：波次表已载入 → 按当前波 groups 展开的 §3.3 时间轴；
        // 否则回退全局"数量+统一间隔"（普通敌人）
        LevelWaves waves = config.getLevelWaves();
        if (waves != null && !waves.getWaves().isEmpty()) {
            int waveIndex = Math.min(state.getWave() - 1, waves.getWaves().size() - 1);
            waveManager.beginWave(expandOrders(waves.getWaves().get(waveIndex)));
        } else {
            waveManager.beginWave(config.getWaveEnemyCount(), config.getWaveSpawnIntervalMs());
        }
        waveInProgress = true;
        if (bonus > 0) {
            state.addGold(bonus);
            notifyGold(state.getGold());
        }
        if (waveSound != null) waveSound.onWaveStarted(state.getWave());
        String msg = "第 " + state.getWave() + " 波来袭！";
        if (bonus > 0) msg += " +" + bonus + " 金币";
        notifyMessage(msg);
        if (state.getWave() >= state.getTotalWaves() && fxScreen != null) {
            fxScreen.showBossWarning();   // 最终波 Boss 预警
        }
    }

    @Override
    public boolean canStartNextWave() {
        return !state.isGameOver() && !waveInProgress && state.getWave() < state.getTotalWaves();
    }

    /** 提前奖励 = 剩余比例 × 上限（round 到整金币）；非倒计时 / 已到期返回 0 */
    private int earlyStartBonusAt(long nowNanos) {
        if (countdownDeadlineNanos <= 0 || nowNanos >= countdownDeadlineNanos) return 0;
        long totalMs = activeIntermissionMs;   // 与实际倒计时总长同源（下一波 maxPrepMs 或全局默认）
        if (totalMs <= 0) return 0;
        double remainMs = (countdownDeadlineNanos - nowNanos) / 1_000_000.0;
        double ratio = Math.min(1.0, Math.max(0.0, remainMs / totalMs));
        return (int) Math.round(config.getEarlyStartRewardCap() * ratio);
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
    public long getNextWaveCountdownMs() {
        if (countdownDeadlineNanos < 0) return 0;
        long remainNanos = countdownDeadlineNanos - System.nanoTime();
        return Math.max(0, remainNanos / 1_000_000L);
    }
    @Override
    public int getEarlyStartBonus() {
        return earlyStartBonusAt(System.nanoTime());
    }
    @Override
    public List<Enemy> getEnemies() { return enemies; }
    @Override
    public List<Tower> getTowers() { return towers; }
    @Override
    public List<Projectile> getProjectiles() { return projectiles; }
    @Override
    public List<Ally> getAllies() { return allies; }
    @Override
    public List<TowerSpec> getTowerSpecs() { return config.getTowerSpecs(); }
    @Override
    public boolean isGameOver() { return state.isGameOver(); }
    @Override
    public boolean isVictory() {
        return !state.isGameOver() && state.getWave() >= state.getTotalWaves()
                && !waveInProgress && enemies.isEmpty();
    }

    // ================= ITowerBuilder =================
    @Override
    public boolean placeTower(double x, double y, TowerType type) {
        BiFunction<Double, Double, Tower> factory = towerFactories.get(type);
        TowerSpec spec = config.getTowerSpec(type);
        if (factory == null || spec == null) {
            notifyMessage("该塔尚未开放建造");
            return false;
        }
        // 越界 / 路径 / 重叠 校验
        if (x < 0 || y < 0 || x > config.getViewWidth() || y > config.getViewHeight()) {
            notifyMessage("建造位置超出地图范围");
            return false;
        }
        if (distToPath(x, y) < PATH_CLEARANCE) {
            notifyMessage("不能在道路上建造");
            return false;
        }
        for (Tower t : towers) {
            if (Math.hypot(t.getX() - x, t.getY() - y) < TOWER_SPACING) {
                notifyMessage("该位置已被占据");
                return false;
            }
        }
        if (!state.spendGold(spec.getCost())) {
            notifyMessage("金币不足！需要 " + spec.getCost());
            return false;
        }

        Tower tower = factory.apply(x, y);
        tower.setX(x);
        tower.setY(y);
        injectTowerChannels(tower);
        towers.add(tower);

        if (towerSound != null) towerSound.onTowerPlaced(type);
        notifyGold(state.getGold());
        if (renderNotifier != null) renderNotifier.requestRender();
        return true;
    }

    /**
     * 升级 = 原位替换（《防御塔子系统说明》§11）：校验满级/工厂/金币 → 扣下一级造价 →
     * 同坐标经工厂建下一级塔 → 注入通道 → 注册表原位替换 → 旧塔 destroy（不走 sell() 防误退款）。
     * 费用语义（§12）：升级价 = nextSpec.getCost()；替换后 totalCost 由下一级塔构造函数
     * 按"累计投入"设定（精英塔类已内置，与默认升级链一致）。
     */
    @Override
    public void upgradeTower(Tower tower) {
        int idx = tower == null ? -1 : towers.indexOf(tower);
        if (idx < 0) return;                          // stale 引用（已替换/已出售）防重复扣费
        if (!(tower instanceof ITowerUpgrade)) {
            notifyMessage("该塔不支持升级");
            return;
        }
        TowerSpec next = ((ITowerUpgrade) tower).getNextLevelSpec();
        if (next == null) {
            notifyMessage("该塔已满级");
            return;
        }
        BiFunction<Double, Double, Tower> factory = towerFactories.get(next.getType());
        if (factory == null) {
            notifyMessage("下一级尚未开放");
            return;
        }
        if (!state.spendGold(next.getCost())) {
            notifyMessage("金币不足！升级需要 " + next.getCost());
            return;
        }
        Tower upgraded = factory.apply(tower.getX(), tower.getY());
        retireBarrackSoldiers(tower);          // 旧兵营产出随替换退役（P1-9，避免新旧两批并存）
        injectTowerChannels(upgraded);
        towers.set(idx, upgraded);
        tower.destroy();
        if (towerSound != null) towerSound.onTowerUpgraded(upgraded);
        notifyGold(state.getGold());
        if (renderNotifier != null) renderNotifier.requestRender();
    }

    @Override
    public void sellTower(Tower tower) {
        if (tower == null) return;
        int refund = tower.sell();
        retireBarrackSoldiers(tower);          // 兵营出售后旧兵随之退场，避免无兵营的孤儿兵（P1-9）
        towers.remove(tower);
        state.addGold(refund);
        if (towerSound != null) towerSound.onTowerSold(tower);
        notifyGold(state.getGold());
        if (renderNotifier != null) renderNotifier.requestRender();
    }

    /**
     * 兵营被替换/出售前，把其产出的在役士兵从 {@code allies} 注册表移除（P1-9）。
     *
     * 契约「实体不碰全局列表」：兵营只交出所产士兵（{@link Barrack#releaseSoldiers()}），
     * 由本控制层负责删除。非兵营塔为空操作。旧兵属"退役"而非阵亡，不触发死亡音效。
     */
    private void retireBarrackSoldiers(Tower tower) {
        if (tower instanceof Barrack) {
            allies.removeAll(((Barrack) tower).releaseSoldiers());
        }
    }

    /** 点到路径折线的最短距离（用于禁止在道路上建塔） */
    private double distToPath(double x, double y) {
        double[] px = config.getPathX();
        double[] py = config.getPathY();
        double min = Double.MAX_VALUE;
        for (int i = 0; i < px.length - 1; i++) {
            min = Math.min(min, distToSegment(x, y, px[i], py[i], px[i + 1], py[i + 1]));
        }
        return min;
    }

    private double distToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1, dy = y2 - y1;
        double lenSq = dx * dx + dy * dy;
        if (lenSq == 0) return Math.hypot(px - x1, py - y1);
        double t = ((px - x1) * dx + (py - y1) * dy) / lenSq;
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
    }

    /** 路径折线上离 (x,y) 最近的点（无路径/路径点不足返回 null）；供士兵驻守点注入 */
    private double[] nearestPathPoint(double x, double y) {
        double[] px = config.getPathX();
        double[] py = config.getPathY();
        if (px == null || py == null || px.length < 2 || py.length < 2) return null;
        double[] best = null;
        double min = Double.MAX_VALUE;
        for (int i = 0; i < px.length - 1 && i < py.length - 1; i++) {
            double dx = px[i + 1] - px[i], dy = py[i + 1] - py[i];
            double lenSq = dx * dx + dy * dy;
            double t = lenSq == 0 ? 0 : ((x - px[i]) * dx + (y - py[i]) * dy) / lenSq;
            t = Math.max(0, Math.min(1, t));
            double gx = px[i] + t * dx, gy = py[i] + t * dy;
            double d = Math.hypot(x - gx, y - gy);
            if (d < min) {
                min = d;
                best = new double[]{gx, gy};
            }
        }
        return best;
    }

    // ================= 重开一局 =================
    public void resetGame() {
        clearBattlefield();
        state.reset();
        notifyGold(state.getGold());
        notifyLives(state.getLives());
        notifyMessage("游戏已重置，点击「开始波次」开战！");
        if (renderNotifier != null) renderNotifier.requestRender();
    }

    /** 清空战场：敌/塔/投射物/友方 + 波次进度与节奏标志（重开与切关共用）。 */
    private void clearBattlefield() {
        enemies.clear();
        towers.clear();
        projectiles.clear();
        allies.clear();
        waveManager.reset();
        waveInProgress = false;
        countdownDeadlineNanos = -1;
        activeIntermissionMs = 0;
    }

    // ================= 多关卡（运行期切图；契约见《多关卡与运行期切图-接口规范》§3.3）=================
    // 说明：本批仅"铺能力"，以下方法暂无调用方（UI 入口留待阶段二），故不影响当前运行行为。

    /** 全部关卡，顺序即关卡顺序（maps/index.json 数组顺序）。 */
    public List<LevelInfo> getLevels() {
        List<MapLibrary.MapEntry> entries = MapLibrary.listMapsFromClasspath();
        List<LevelInfo> levels = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            MapLibrary.MapEntry e = entries.get(i);
            levels.add(new LevelInfo(i, e.getKey(), e.getName()));
        }
        return levels;
    }

    /** 关卡总数。 */
    public int getLevelCount() { return getLevels().size(); }

    /** 当前关卡信息；当前 key 未登记时返回 null。 */
    public LevelInfo getCurrentLevel() {
        String cur = config.getMapKey();
        if (cur == null) return null;
        for (LevelInfo li : getLevels()) {
            if (li.getKey().equals(cur)) return li;
        }
        return null;
    }

    /** 当前关卡 key（= 当前地图 key）。 */
    public String getCurrentLevelKey() { return config.getMapKey(); }

    /** 当前关卡序号；未匹配到返回 -1。 */
    public int getCurrentLevelIndex() {
        LevelInfo cur = getCurrentLevel();
        return cur == null ? -1 : cur.getIndex();
    }

    /** 是否存在下一关。 */
    public boolean hasNextLevel() {
        int i = getCurrentLevelIndex();
        return i >= 0 && i + 1 < getLevelCount();
    }

    /** 是否存在上一关。 */
    public boolean hasPrevLevel() { return getCurrentLevelIndex() > 0; }

    /** 跳转到下一关；无下一关返回 false（状态零变化）。 */
    public boolean goNextLevel() {
        int i = getCurrentLevelIndex();
        return i >= 0 && switchLevelAt(i + 1);
    }

    /** 跳转到上一关；无上一关返回 false（状态零变化）。 */
    public boolean goPrevLevel() {
        int i = getCurrentLevelIndex();
        return i > 0 && switchLevelAt(i - 1);
    }

    /**
     * 跳转到指定关卡（按 key）：成功则清空战场、按新关重置金币/生命/波次并通知 UI；
     * 失败（key 不存在或地图数据缺失）返回 false 且**状态零变化**。
     */
    public boolean switchLevel(String key) {
        if (!config.loadMap(key)) {
            notifyMessage("关卡不存在或地图数据缺失：" + key);
            return false;
        }
        clearBattlefield();
        state.applyConfig(config);
        notifyGold(state.getGold());
        notifyLives(state.getLives());
        notifyMessage("已切换关卡：" + config.getMapDisplayName());
        if (renderNotifier != null) renderNotifier.requestRender();
        return true;
    }

    /** 按序号跳转（越界返回 false，状态零变化）。 */
    public boolean switchLevelAt(int index) {
        List<LevelInfo> levels = getLevels();
        if (index < 0 || index >= levels.size()) return false;
        return switchLevel(levels.get(index).getKey());
    }

    /** 重开本关（不切图）：战场与状态重置，地图不变。 */
    public boolean restartLevel() {
        resetGame();
        return true;
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
