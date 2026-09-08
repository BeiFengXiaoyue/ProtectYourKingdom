package com.kingdom.game.config;

import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.TowerType;
import com.kingdom.game.util.MapRoute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GameConfig —— 开发期可修改的游戏配置对象（唯一数值来源）。
 *
 * 设计意图：
 * - 不是 static final 常量类（编译后改不动、调试麻烦），而是普通实例对象；
 * - 数值全部收敛在此，实体/控制器/界面通过 getter 现取；
 * - 提供 fluent setter，开发期在 Main 里改一行即可微调，改动对新建实体/新波次即时生效；
 * - 塔目录（TowerSpec）也登记在此，配合 registerTowerFactory 支持"逐步加塔"。
 *
 * 默认值取自《计划书 v1.0》§4.3 数值表。
 */
public class GameConfig {

    // ===== 画布 =====
    private double viewWidth = 900;
    private double viewHeight = 560;

    // ===== 游戏初值（Day1 验收：3 只漏掉后 20 → 17）=====
    private int initialGold = 100;
    private int initialLives = 20;
    private int totalWaves = 10;

    // ===== NormalEnemy 基础数值 =====
    private int normalHp = 80;
    private double normalSpeed = 60;   // px/s
    private int normalGoldReward = 10;

    // ===== 波次 =====
    private int waveEnemyCount = 3;        // 每波敌人数（Day1 固定 3 只验收）
    private long waveSpawnIntervalMs = 1000; // 出场间隔(ms)

    // ===== 敌人移动路径（预设拐点）=====
    private double[] pathX = {60, 300, 300, 620, 620, 860};
    private double[] pathY = {160, 160, 360, 360, 240, 240};

    // ===== 渲染配色 =====
    private String colorBackground = "#dcefc7"; // 草地
    private String colorPath = "#8a5a2b";       // 深色土路

    // ===== 塔目录（逐步开发：开始只有 ARROW，后续 addTowerSpec 追加）=====
    private final List<TowerSpec> towerSpecs = new ArrayList<>();

    /**
     * 无参构造：启动时自动探测 maps/default_path.json（由 util.MapRoute 工具类解析）。
     * - 存在：套用导出路线的画布尺寸与路径（敌人出生/渲染/移动/禁塔区随之生效）；
     * - 不存在/解析失败：保持下方默认值，行为与旧版一致。
     */
    public GameConfig() {
        MapRoute route = MapRoute.loadFromClasspath("/maps/default_path.json");
        if (route != null) {
            setViewSize(route.getWidth(), route.getHeight());
            setPath(route.getXs(), route.getYs());
        }
    }

    // ===== 画布 =====
    public double getViewWidth() { return viewWidth; }
    public double getViewHeight() { return viewHeight; }
    public GameConfig setViewSize(double width, double height) {
        this.viewWidth = width;
        this.viewHeight = height;
        return this;
    }

    // ===== 游戏初值 =====
    public int getInitialGold() { return initialGold; }
    public int getInitialLives() { return initialLives; }
    public int getTotalWaves() { return totalWaves; }
    public GameConfig setInitialGold(int initialGold) {
        this.initialGold = initialGold;
        return this;
    }
    public GameConfig setInitialLives(int initialLives) {
        this.initialLives = initialLives;
        return this;
    }
    public GameConfig setTotalWaves(int totalWaves) {
        this.totalWaves = totalWaves;
        return this;
    }

    // ===== NormalEnemy 数值 =====
    public int getNormalHp() { return normalHp; }
    public double getNormalSpeed() { return normalSpeed; }
    public int getNormalGoldReward() { return normalGoldReward; }
    public GameConfig setNormalStats(int hp, double speed, int goldReward) {
        this.normalHp = hp;
        this.normalSpeed = speed;
        this.normalGoldReward = goldReward;
        return this;
    }

    // ===== 波次 =====
    public int getWaveEnemyCount() { return waveEnemyCount; }
    public long getWaveSpawnIntervalMs() { return waveSpawnIntervalMs; }
    public GameConfig setWave(int enemyCount, long spawnIntervalMs) {
        this.waveEnemyCount = enemyCount;
        this.waveSpawnIntervalMs = spawnIntervalMs;
        return this;
    }

    // ===== 路径 =====
    public double[] getPathX() { return pathX; }
    public double[] getPathY() { return pathY; }
    public double getPathStartX() { return pathX[0]; }
    public double getPathStartY() { return pathY[0]; }
    public GameConfig setPath(double[] pathX, double[] pathY) {
        if (pathX == null || pathY == null || pathX.length == 0 || pathX.length != pathY.length) {
            throw new IllegalArgumentException("路径坐标数组不能为空且长度必须一致");
        }
        this.pathX = pathX;
        this.pathY = pathY;
        return this;
    }

    // ===== 配色 =====
    public String getColorBackground() { return colorBackground; }
    public String getColorPath() { return colorPath; }
    public GameConfig setColors(String background, String path) {
        this.colorBackground = background;
        this.colorPath = path;
        return this;
    }

    // ===== 塔目录 =====
    /** 登记一种可建造塔的目录条目（显示名/造价），由装配处或防御塔负责人调用 */
    public GameConfig addTowerSpec(TowerSpec spec) {
        if (spec != null) towerSpecs.add(spec);
        return this;
    }

    /** 当前可建造塔目录（只读） */
    public List<TowerSpec> getTowerSpecs() {
        return Collections.unmodifiableList(towerSpecs);
    }

    /** 按类型取塔目录条目；未登记返回 null */
    public TowerSpec getTowerSpec(TowerType type) {
        for (TowerSpec spec : towerSpecs) {
            if (spec.getType() == type) return spec;
        }
        return null;
    }
}
