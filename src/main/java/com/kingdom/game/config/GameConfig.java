package com.kingdom.game.config;

import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.TowerType;
import com.kingdom.game.util.balance.BalanceLibrary;
import com.kingdom.game.util.balance.BalanceTable;
import com.kingdom.game.util.map.LevelWaves;
import com.kingdom.game.util.map.MapLibrary;
import com.kingdom.game.util.map.MapRoute;
import com.kingdom.game.util.map.TowerSpots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GameConfig —— 运行期唯一数值入口（合并版：JSON 化 + LevelWaves 波次表）。
 *
 * 数值来源分三类：
 * 1. JSON 化字段（23 个）：玩家开局 + 四类敌人（normal/fast/tank/boss 的 HP/speed/goldReward）
 *    + 四类敌人近战（攻击力 / 攻击冷却），
 *    构造期从 classpath:/config/balance.json 读取（util.balance.BalanceLibrary），
 *    文件缺失/字段非法 → 回退 BalanceTable.defaults() 内置默认，不抛异常。
 * 2. 关卡波次表（LevelWaves）：从 maps/&lt;key&gt;/waves.json 读取，
 *    读取成功则总波数由波次表唯一决定（覆盖 JSON 中的 totalWaves）；
 *    缺失/为空/解析失败 → 回退全局波次配置并告警，运行期不崩。
 * 3. 字面量默认字段（波次节奏 4 个）：waveEnemyCount/waveSpawnIntervalMs/
 *    waveIntermissionMs/earlyStartRewardCap，仍写死在此类，不纳入 JSON 化。
 *
 * 改 JSON 化数值请编辑 src/main/resources/config/balance.json，重启游戏生效。
 */
public class GameConfig {

    // ===== 画布 =====
    private double viewWidth = 900;
    private double viewHeight = 560;

    // ===== ① 玩家开局（由 balance.json 读取，缺失回退 BalanceTable.defaults()）=====
    private int initialGold;
    private int initialLives;
    private int totalWaves;

    // ===== ② 普通敌人（由 balance.json 读取）=====
    private int normalHp;
    private double normalSpeed;   // px/s
    private int normalGoldReward;

    // ===== ③ 快速敌人（由 balance.json 读取）=====
    private int fastHp;
    private double fastSpeed;
    private int fastGoldReward;

    // ===== ④ 重甲敌人（由 balance.json 读取）=====
    private int tankHp;
    private double tankSpeed;
    private int tankGoldReward;

    // ===== ⑤ Boss（由 balance.json 读取）=====
    private int bossHp;
    private double bossSpeed;
    private int bossGoldReward;

    // ===== ⑥ 敌人近战：攻击力 / 攻击冷却（由 balance.json 读取）=====
    private int normalAttackDamage;
    private int normalAttackCooldownMs;
    private int fastAttackDamage;
    private int fastAttackCooldownMs;
    private int tankAttackDamage;
    private int tankAttackCooldownMs;
    private int bossAttackDamage;
    private int bossAttackCooldownMs;

    // 注：重甲物理减伤比例不经本类暴露——敌人侧直接读 BalanceTable.runtime()
    //     （见 TankEnemy 构造），此处不再保留平行副本，避免两套口径。

    // ===== 波次（不纳入 JSON 化，保持字面量默认，避免与波次模块合并冲突）=====
    private int waveEnemyCount = 3;        // 每波敌人数
    private long waveSpawnIntervalMs = 1000; // 出场间隔(ms)

    // ===== 波间倒计时（不纳入 JSON 化）=====
    private long waveIntermissionMs = 5000;   // 波间倒计时
    private int earlyStartRewardCap = 10;     // 提前开始奖励上限（金币）

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

    // ===== 关卡波次表（maps/<key>/waves.json；null=无波次表，运行期回退全局波次配置）=====
    private LevelWaves levelWaves;

    // ===== 塔目录（逐步开发：开始只有 ARROW，后续 addTowerSpec 追加）=====
    private final List<TowerSpec> towerSpecs = new ArrayList<>();

    /**
     * 无参构造（合并版）：
     * 1. 从 classpath 读取 /config/balance.json（24 个 JSON 化字段），缺失/非法回退内置默认；
     * 2. 从 classpath 读 maps/index.json 选择"默认地图"（可用 -Dmap.key=&lt;key&gt; 指定），
     *    再从 maps/&lt;key&gt;/ 读取 path.json / spots.json / 底图文件名；
     * 3. 读取 maps/&lt;key&gt;/waves.json 波次表，成功则总波数由波次表决定（覆盖 JSON 的 totalWaves），
     *    缺失/为空/解析失败 → 回退全局波次配置并告警。
     * 任一文件缺失/解析失败 → 该项保持内置默认，运行期不崩。
     */
    public GameConfig() {
        // ===== 第 1 步：JSON 化数值从 balance.json 读取，缺失/非法回退内置默认 =====
        BalanceTable bt = BalanceLibrary.readFromClasspath();
        if (bt == null) bt = BalanceTable.defaults();
        // ① 玩家开局
        this.initialGold = bt.getInitialGold();
        this.initialLives = bt.getInitialLives();
        this.totalWaves = bt.getTotalWaves();
        // ② 普通敌人
        this.normalHp = bt.getNormalHp();
        this.normalSpeed = bt.getNormalSpeed();
        this.normalGoldReward = bt.getNormalGoldReward();
        // ③ 快速敌人
        this.fastHp = bt.getFastHp();
        this.fastSpeed = bt.getFastSpeed();
        this.fastGoldReward = bt.getFastGoldReward();
        // ④ 重甲敌人
        this.tankHp = bt.getTankHp();
        this.tankSpeed = bt.getTankSpeed();
        this.tankGoldReward = bt.getTankGoldReward();
        // ⑤ Boss
        this.bossHp = bt.getBossHp();
        this.bossSpeed = bt.getBossSpeed();
        this.bossGoldReward = bt.getBossGoldReward();
        // ⑥ 敌人近战：攻击力 / 冷却（数值由 F 定稿；BalanceTable 内置默认 = 原实体类写死值）
        this.normalAttackDamage = bt.getNormalAttackDamage();
        this.normalAttackCooldownMs = bt.getNormalAttackCooldownMs();
        this.fastAttackDamage = bt.getFastAttackDamage();
        this.fastAttackCooldownMs = bt.getFastAttackCooldownMs();
        this.tankAttackDamage = bt.getTankAttackDamage();
        this.tankAttackCooldownMs = bt.getTankAttackCooldownMs();
        this.bossAttackDamage = bt.getBossAttackDamage();
        this.bossAttackCooldownMs = bt.getBossAttackCooldownMs();
        // 波次节奏 4 个字段不读 JSON，保持上方字面量默认值

        // ===== 第 2 步：地图分包读取（选定默认地图后交给 loadMap，逻辑与原实现等价）=====
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
            loadMap(active.getKey());
        }
    }

    /**
     * 加载"地图域"数据到本实例（构造期与运行期切图共用；契约见《多关卡与运行期切图-接口规范》§3.2）。
     *
     * <p>只影响<b>地图域</b>字段：{@code mapKey} / {@code mapImageName} / 视图尺寸 / 路径 / 塔位 / 波次 / 总波数；
     * <b>不触碰</b> {@code balance.json} 全局数值（开局金币生命、敌人与塔属性）。
     *
     * <p><b>原子提交</b>：先校验并读取，任一步失败 → 返回 {@code false} 且<b>不修改任何字段</b>
     * （保持调用前的地图不变）。波次表缺失不视为失败：沿用既有口径回退全局波次配置并告警。
     *
     * @param key {@code maps/index.json} 中登记的关卡 key
     * @return 是否成功加载
     */
    public boolean loadMap(String key) {
        if (key == null || key.isBlank()) return false;
        MapLibrary.MapEntry entry = null;
        for (MapLibrary.MapEntry e : MapLibrary.listMapsFromClasspath()) {
            if (key.equals(e.getKey())) {
                entry = e;
                break;
            }
        }
        if (entry == null) {
            System.err.println("[GameConfig] loadMap：maps/index.json 中无此地图 \"" + key + "\"");
            return false;
        }
        MapRoute route = MapLibrary.readPathFromClasspath(key);
        TowerSpots spots = MapLibrary.readSpotsFromClasspath(key);
        if (route == null || spots == null) {
            System.err.println("[GameConfig] loadMap：maps/" + key
                    + " 的 path.json / spots.json 缺失或解析失败，保持原地图");
            return false;
        }
        // ===== 校验与读取全部通过 → 原子提交 =====
        this.mapKey = entry.getKey();
        this.mapImageName = entry.getImage();
        setViewSize(route.getWidth(), route.getHeight());
        setPath(route.getXs(), route.getYs());
        this.towerSpots = spots;

        // 波次表：读取成功则波数由文件唯一决定（覆盖 JSON 中的 totalWaves）；
        // 缺失/为空/解析失败 → 回退全局波次配置并告警，运行期不崩，且**不视为失败**
        LevelWaves waves = MapLibrary.readWavesFromClasspath(key);
        if (waves != null && !waves.getWaves().isEmpty()) {
            this.levelWaves = waves;
            this.totalWaves = waves.getWaves().size();
        } else {
            System.err.println("[GameConfig] maps/" + key
                    + "/waves.json 缺失/为空/解析失败，回退全局波次配置");
        }
        return true;
    }

    /** 当前地图显示名（读 maps/index.json 的 name；未登记回退 key，无地图回退空串） */
    public String getMapDisplayName() {
        String key = mapKey;
        if (key == null) return "";
        for (MapLibrary.MapEntry e : MapLibrary.listMapsFromClasspath()) {
            if (key.equals(e.getKey())) return e.getName();
        }
        return key;
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

    // ===== ① 玩家开局 =====
    public int getInitialGold() { return initialGold; }
    public int getInitialLives() { return initialLives; }
    public int getTotalWaves() { return totalWaves; }
    public GameConfig setInitialGold(int initialGold) { this.initialGold = initialGold; return this; }
    public GameConfig setInitialLives(int initialLives) { this.initialLives = initialLives; return this; }
    public GameConfig setTotalWaves(int totalWaves) { this.totalWaves = totalWaves; return this; }

    // ===== ② 普通敌人 =====
    public int getNormalHp() { return normalHp; }
    public double getNormalSpeed() { return normalSpeed; }
    public int getNormalGoldReward() { return normalGoldReward; }
    public GameConfig setNormalStats(int hp, double speed, int goldReward) {
        this.normalHp = hp;
        this.normalSpeed = speed;
        this.normalGoldReward = goldReward;
        return this;
    }

    // ===== ③ 快速敌人 =====
    public int getFastHp() { return fastHp; }
    public double getFastSpeed() { return fastSpeed; }
    public int getFastGoldReward() { return fastGoldReward; }
    public GameConfig setFastStats(int hp, double speed, int goldReward) {
        this.fastHp = hp;
        this.fastSpeed = speed;
        this.fastGoldReward = goldReward;
        return this;
    }

    // ===== ④ 重甲敌人 =====
    public int getTankHp() { return tankHp; }
    public double getTankSpeed() { return tankSpeed; }
    public int getTankGoldReward() { return tankGoldReward; }
    public GameConfig setTankStats(int hp, double speed, int goldReward) {
        this.tankHp = hp;
        this.tankSpeed = speed;
        this.tankGoldReward = goldReward;
        return this;
    }

    // ===== ⑤ Boss =====
    public int getBossHp() { return bossHp; }
    public double getBossSpeed() { return bossSpeed; }
    public int getBossGoldReward() { return bossGoldReward; }
    public GameConfig setBossStats(int hp, double speed, int goldReward) {
        this.bossHp = hp;
        this.bossSpeed = speed;
        this.bossGoldReward = goldReward;
        return this;
    }

    // ===== ⑥ 敌人近战：攻击力 / 攻击冷却 =====
    // 注：仅暴露 getter（消费方 spawnEnemy 只读）；写入通道仍是 balance.json / BalanceTable。
    public int getNormalAttackDamage() { return normalAttackDamage; }
    public int getNormalAttackCooldownMs() { return normalAttackCooldownMs; }
    public int getFastAttackDamage() { return fastAttackDamage; }
    public int getFastAttackCooldownMs() { return fastAttackCooldownMs; }
    public int getTankAttackDamage() { return tankAttackDamage; }
    public int getTankAttackCooldownMs() { return tankAttackCooldownMs; }
    public int getBossAttackDamage() { return bossAttackDamage; }
    public int getBossAttackCooldownMs() { return bossAttackCooldownMs; }

    // ===== 波次 =====
    /** 关卡波次表（null=无波次表，运行期回退全局波次配置）；敌人数值仍按 id 从本类现取 */
    public LevelWaves getLevelWaves() { return levelWaves; }
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

    // ===== 地图底图与塔位标点 =====
    public String getMapImageName() { return mapImageName; }
    public GameConfig setMapImageName(String mapImageName) {
        this.mapImageName = mapImageName;
        return this;
    }

    public TowerSpots getTowerSpots() { return towerSpots; }
    public GameConfig setTowerSpots(TowerSpots towerSpots) {
        if (towerSpots != null) this.towerSpots = towerSpots;
        return this;
    }

    // ===== 塔目录 =====
    public GameConfig addTowerSpec(TowerSpec spec) {
        if (spec != null) towerSpecs.add(spec);
        return this;
    }

    public List<TowerSpec> getTowerSpecs() {
        return Collections.unmodifiableList(towerSpecs);
    }

    public TowerSpec getTowerSpec(TowerType type) {
        for (TowerSpec spec : towerSpecs) {
            if (spec.getType() == type) return spec;
        }
        return null;
    }
}
