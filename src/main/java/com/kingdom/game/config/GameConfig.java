package com.kingdom.game.config;

import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.TowerType;
import com.kingdom.game.util.map.MapLibrary;
import com.kingdom.game.util.map.MapRoute;
import com.kingdom.game.util.map.TowerSpots;

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

    // ===== 波间倒计时（Kingdom Rush 式：第 2 波起每波前倒计时，可提前开始得金币）=====
    private long waveIntermissionMs = 5000;   // 临时数值：波间倒计时暂定 5s（待定稿调整）
    private int earlyStartRewardCap = 10;     // 临时数值：提前开始奖励上限（金币），可调

    // ===== 敌人移动路径（预设拐点）=====
    private double[] pathX = {60, 300, 300, 620, 620, 860};
    private double[] pathY = {160, 160, 360, 360, 240, 240};

    // ===== 渲染配色 =====
    private String colorBackground = "#dcefc7"; // 草地
    private String colorPath = "#8a5a2b";       // 深色土路

    // ===== 地图分包（maps/index.json 注册表 + 每图目录 maps/<key>/…）=====
    private String mapKey;                  // 活动地图 key（默认取 index 第一条）
    private String mapImageName;            // 底图文件名（位于 maps/<key>/ 下；null=无底图回退配色）
    private TowerSpots towerSpots = TowerSpots.of(900, 560, new double[0], new double[0]);

    // ===== 塔目录（逐步开发：开始只有 ARROW，后续 addTowerSpec 追加）=====
    private final List<TowerSpec> towerSpecs = new ArrayList<>();

    /**
     * 无参构造：启动时从 classpath 读 maps/index.json 选择“默认地图”（可用 -Dmap.key=&lt;key&gt; 指定），
     * 再从 maps/&lt;key&gt;/ 读取 path.json / spots.json / 底图文件名。
     * 任一文件缺失/解析失败 → 该项保持内置默认（画布/路径/塔位）。
     */
    public GameConfig() {
        java.util.List<MapLibrary.MapEntry> entries = MapLibrary.listMapsFromClasspath();
        String override = System.getProperty("map.key");
        MapLibrary.MapEntry active = null;
        for (MapLibrary.MapEntry e : entries) {
            if (override != null && e.getKey().equals(override)) {
                active = e;
                break;
            }
        }
        if (active == null && !entries.isEmpty()) active = entries.get(0);
        if (active != null) {
            this.mapKey = active.getKey();
            this.mapImageName = active.getImage();

            MapRoute route = MapLibrary.readPathFromClasspath(active.getKey());
            if (route != null) {
                setViewSize(route.getWidth(), route.getHeight());
                setPath(route.getXs(), route.getYs());
            }
            TowerSpots spots = MapLibrary.readSpotsFromClasspath(active.getKey());
            if (spots != null) {
                this.towerSpots = spots;
            }
        }
    }

    /** 活动地图 key（null=未找到 index，使用内置默认） */
    public String getMapKey() { return mapKey; }

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

    public long getWaveIntermissionMs() { return waveIntermissionMs; }
    public int getEarlyStartRewardCap() { return earlyStartRewardCap; }
    public GameConfig setWaveIntermissionMs(long intermissionMs) {
        this.waveIntermissionMs = Math.max(0, intermissionMs);
        return this;
    }
    public GameConfig setEarlyStartRewardCap(int cap) {
        this.earlyStartRewardCap = Math.max(0, cap);
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

    // ===== 地图底图与塔位标点（由标注工具生成，resources/maps/ 下）=====
    /** 地图底图文件名（位于 /maps/ 下）；null 表示无底图，渲染回退为配色画法 */
    public String getMapImageName() { return mapImageName; }
    public GameConfig setMapImageName(String mapImageName) {
        this.mapImageName = mapImageName;
        return this;
    }

    /** 手工标注的塔位（只读）；无标注文件时为空列表 */
    public TowerSpots getTowerSpots() { return towerSpots; }
    public GameConfig setTowerSpots(TowerSpots towerSpots) {
        if (towerSpots != null) this.towerSpots = towerSpots;
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
