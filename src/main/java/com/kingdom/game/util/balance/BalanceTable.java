package com.kingdom.game.util.balance;

import com.kingdom.game.util.json.MiniJson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * BalanceTable —— 玩法数值数据类（唯一内置默认持有者）。
 *
 * 当前纳入 JSON 化的字段（29 个，分 9 组）：
 * - ① 玩家开局：initialGold / initialLives / totalWaves
 * - ② 普通敌人：normalHp / normalSpeed / normalGoldReward
 * - ③ 快速敌人：fastHp / fastSpeed / fastGoldReward
 * - ④ 重甲敌人：tankHp / tankSpeed / tankGoldReward
 * - ⑤ Boss：bossHp / bossSpeed / bossGoldReward
 * - ⑥ 敌人近战：
 *      {normal|fast|tank|boss}AttackDamage / …AttackCooldownMs
 * - ⑦ 重甲减伤：tankPhysicalReduction，范围 [0,1)
 * - ⑧ 炮塔溅射半径：{cannon|eliteCannon|masterCannon}SplashRadius，
 *      必须 > 0；由各炮塔构造期经 {@link #runtime()} 读取并传给 Bomb
 * - ⑨ Boss 狂暴机制：bossStompIntervalMs（>0）、
 *      bossEnrageAttackCooldownCut（[0,1)）；由 {@code BossEnemy} 构造期经 {@link #runtime()} 读取
 * （字段按 id/语义的归属总表见 docs/数值配置JSON化规范.md）
 *
 * 波次节奏字段（waveEnemyCount / waveSpawnIntervalMs / waveIntermissionMs /
 * earlyStartRewardCap）不纳入 JSON 化，仍由 GameConfig 字面量默认值管理，
 * 避免与波次模块并行开发产生合并冲突。
 *
 * 职责：
 * - 承载 config/balance.json 的固定数值字段（强类型，有 getter/setter）；
 * - 额外支持动态扩展字段（extra Map），供编辑器添加未来新增的变量
 *   （⚠️ 这些字段**运行期无人读取**，fromJson 遇到未知键会打印 `[BalanceTable]` 告警）；
 * - toJson() / fromJson() 读写（复用 util.json.MiniJson，不引第三方库）；
 * - defaults() 提供内置默认，文件缺失/字段非法时回退；
 * - loadFromClasspath() 供运行期 GameConfig 读取，缺失返回 null（不抛异常）。
 */
public final class BalanceTable {

    // ===== ① 玩家开局 =====
    private int initialGold;
    private int initialLives;
    private int totalWaves;

    // ===== ② 普通敌人 =====
    private int normalHp;
    private double normalSpeed;
    private int normalGoldReward;

    // ===== ③ 快速敌人 =====
    private int fastHp;
    private double fastSpeed;
    private int fastGoldReward;

    // ===== ④ 重甲敌人 =====
    private int tankHp;
    private double tankSpeed;
    private int tankGoldReward;

    // ===== ⑤ Boss =====
    private int bossHp;
    private double bossSpeed;
    private int bossGoldReward;

    // ===== ⑥ 敌人近战：攻击力 / 攻击冷却（ms）=====
    private int normalAttackDamage;
    private int normalAttackCooldownMs;
    private int fastAttackDamage;
    private int fastAttackCooldownMs;
    private int tankAttackDamage;
    private int tankAttackCooldownMs;
    private int bossAttackDamage;
    private int bossAttackCooldownMs;

    // ===== ⑦ 重甲物理减伤（比例，0 ≤ v < 1；0 = 不减伤）=====
    private double tankPhysicalReduction;

    // ===== ⑧ 炮塔溅射半径（px，> 0）=====
    private double cannonSplashRadius;
    private double eliteCannonSplashRadius;
    private double masterCannonSplashRadius;

    // ===== ⑨ Boss 狂暴机制 =====
    /** 狂暴后震地间隔 ms（> 0）：每间隔触发一次全塔眩晕请求 */
    private int bossStompIntervalMs;
    /** 狂暴后攻击冷却削减比例（[0,1)）：0.3 = 攻击间隔砍 30%（冷却 ×0.7）；0 = 不改 */
    private double bossEnrageAttackCooldownCut;

    /** 动态扩展字段：编辑器添加的未来变量，fromJson 未知键自动收入此 Map，toJson 一并写出 */
    private final Map<String, Object> extra = new LinkedHashMap<>();

    private BalanceTable() { }

    // ================= 内置默认（唯一来源）=================

    /**
     * 内置默认 = 兜底值（仅当 config/balance.json 缺失/损坏时生效）。
     * 内容须与 config/balance.json 保持一致，改一处请同步另一处。
     */
    public static BalanceTable defaults() {
        BalanceTable b = new BalanceTable();
        // 玩家开局
        b.initialGold = 130;
        b.initialLives = 20;
        b.totalWaves = 10;
        // 普通敌人
        b.normalHp = 80;
        b.normalSpeed = 25.0;
        b.normalGoldReward = 15;
        // 快速敌人（速度约为普通 2 倍，HP 低，赏金略高）
        b.fastHp = 50;
        b.fastSpeed = 50.0;
        b.fastGoldReward = 22;
        // 重甲敌人（HP 约普通 3 倍，速度慢，赏金高）
        b.tankHp = 240;
        b.tankSpeed = 18.0;
        b.tankGoldReward = 35;
        // Boss（高血量，最终波首领）
        b.bossHp = 1500;
        b.bossSpeed = 22.0;
        b.bossGoldReward = 300;
        // 敌人近战：攻击力 / 攻击冷却（= 实体类原有写死值，改造后表现不变）
        b.normalAttackDamage = 5;
        b.normalAttackCooldownMs = 1000;
        b.fastAttackDamage = 4;
        b.fastAttackCooldownMs = 700;
        b.tankAttackDamage = 9;
        b.tankAttackCooldownMs = 1400;
        b.bossAttackDamage = 22;
        b.bossAttackCooldownMs = 1100;
        // 重甲物理减伤（30%，仅普通箭矢吃减伤；炮塔破甲无视）
        b.tankPhysicalReduction = 0.3;
        // 炮塔溅射半径（《建筑与怪物机制策划》L1/L2/L3 = 50/65/80）
        b.cannonSplashRadius = 50.0;
        b.eliteCannonSplashRadius = 65.0;
        b.masterCannonSplashRadius = 80.0;
        // Boss 狂暴机制（《策划》§3.2：狂暴后每 15s 震地一次；攻击间隔砍 30%）
        b.bossStompIntervalMs = 15000;
        b.bossEnrageAttackCooldownCut = 0.3;
        return b;
    }

    // ================= ① 玩家开局 Getter / Setter =================

    public int getInitialGold() { return initialGold; }
    public void setInitialGold(int v) { this.initialGold = v; }

    public int getInitialLives() { return initialLives; }
    public void setInitialLives(int v) { this.initialLives = v; }

    public int getTotalWaves() { return totalWaves; }
    public void setTotalWaves(int v) { this.totalWaves = v; }

    // ================= ② 普通敌人 Getter / Setter =================

    public int getNormalHp() { return normalHp; }
    public void setNormalHp(int v) { this.normalHp = v; }

    public double getNormalSpeed() { return normalSpeed; }
    public void setNormalSpeed(double v) { this.normalSpeed = v; }

    public int getNormalGoldReward() { return normalGoldReward; }
    public void setNormalGoldReward(int v) { this.normalGoldReward = v; }

    // ================= ③ 快速敌人 Getter / Setter =================

    public int getFastHp() { return fastHp; }
    public void setFastHp(int v) { this.fastHp = v; }

    public double getFastSpeed() { return fastSpeed; }
    public void setFastSpeed(double v) { this.fastSpeed = v; }

    public int getFastGoldReward() { return fastGoldReward; }
    public void setFastGoldReward(int v) { this.fastGoldReward = v; }

    // ================= ④ 重甲敌人 Getter / Setter =================

    public int getTankHp() { return tankHp; }
    public void setTankHp(int v) { this.tankHp = v; }

    public double getTankSpeed() { return tankSpeed; }
    public void setTankSpeed(double v) { this.tankSpeed = v; }

    public int getTankGoldReward() { return tankGoldReward; }
    public void setTankGoldReward(int v) { this.tankGoldReward = v; }

    // ================= ⑤ Boss Getter / Setter =================

    public int getBossHp() { return bossHp; }
    public void setBossHp(int v) { this.bossHp = v; }

    public double getBossSpeed() { return bossSpeed; }
    public void setBossSpeed(double v) { this.bossSpeed = v; }

    public int getBossGoldReward() { return bossGoldReward; }
    public void setBossGoldReward(int v) { this.bossGoldReward = v; }

    // ================= ⑥ 敌人近战 Getter / Setter =================

    public int getNormalAttackDamage() { return normalAttackDamage; }
    public void setNormalAttackDamage(int v) { this.normalAttackDamage = v; }

    public int getNormalAttackCooldownMs() { return normalAttackCooldownMs; }
    public void setNormalAttackCooldownMs(int v) { this.normalAttackCooldownMs = v; }

    public int getFastAttackDamage() { return fastAttackDamage; }
    public void setFastAttackDamage(int v) { this.fastAttackDamage = v; }

    public int getFastAttackCooldownMs() { return fastAttackCooldownMs; }
    public void setFastAttackCooldownMs(int v) { this.fastAttackCooldownMs = v; }

    public int getTankAttackDamage() { return tankAttackDamage; }
    public void setTankAttackDamage(int v) { this.tankAttackDamage = v; }

    public int getTankAttackCooldownMs() { return tankAttackCooldownMs; }
    public void setTankAttackCooldownMs(int v) { this.tankAttackCooldownMs = v; }

    public int getBossAttackDamage() { return bossAttackDamage; }
    public void setBossAttackDamage(int v) { this.bossAttackDamage = v; }

    public int getBossAttackCooldownMs() { return bossAttackCooldownMs; }
    public void setBossAttackCooldownMs(int v) { this.bossAttackCooldownMs = v; }

    // ================= ⑦ 重甲减伤 Getter / Setter =================

    public double getTankPhysicalReduction() { return tankPhysicalReduction; }
    public void setTankPhysicalReduction(double v) { this.tankPhysicalReduction = v; }

    // ================= ⑧ 炮塔溅射半径 Getter / Setter =================

    /** 1 级炮塔 Bomb 溅射半径 px（默认 50） */
    public double getCannonSplashRadius() { return cannonSplashRadius; }
    public void setCannonSplashRadius(double v) { this.cannonSplashRadius = v; }

    /** 2 级精英炮塔 Bomb 溅射半径 px（默认 65） */
    public double getEliteCannonSplashRadius() { return eliteCannonSplashRadius; }
    public void setEliteCannonSplashRadius(double v) { this.eliteCannonSplashRadius = v; }

    /** 3 级大师炮塔 Bomb 溅射半径 px（默认 80） */
    public double getMasterCannonSplashRadius() { return masterCannonSplashRadius; }
    public void setMasterCannonSplashRadius(double v) { this.masterCannonSplashRadius = v; }

    // ================= ⑨ Boss 狂暴机制 Getter / Setter =================

    /** 狂暴后震地间隔 ms（默认 15000 = 每 15 秒一次全塔眩晕） */
    public int getBossStompIntervalMs() { return bossStompIntervalMs; }
    public void setBossStompIntervalMs(int v) { this.bossStompIntervalMs = v; }

    /** 狂暴后攻击冷却削减比例，[0,1)（默认 0.3 = 攻击间隔砍 30%） */
    public double getBossEnrageAttackCooldownCut() { return bossEnrageAttackCooldownCut; }
    public void setBossEnrageAttackCooldownCut(double v) { this.bossEnrageAttackCooldownCut = v; }

    // ================= 动态扩展字段 =================

    /** 已知的固定字段键集合（fromJson 时用于区分固定字段与扩展字段） */
    private static final Set<String> FIXED_KEYS = Set.of(
            "initialGold", "initialLives", "totalWaves",
            "normalHp", "normalSpeed", "normalGoldReward",
            "fastHp", "fastSpeed", "fastGoldReward",
            "tankHp", "tankSpeed", "tankGoldReward",
            "bossHp", "bossSpeed", "bossGoldReward",
            "normalAttackDamage", "normalAttackCooldownMs",
            "fastAttackDamage", "fastAttackCooldownMs",
            "tankAttackDamage", "tankAttackCooldownMs",
            "bossAttackDamage", "bossAttackCooldownMs",
            "tankPhysicalReduction",
            "cannonSplashRadius", "eliteCannonSplashRadius", "masterCannonSplashRadius",
            "bossStompIntervalMs", "bossEnrageAttackCooldownCut");

    public Object getExtra(String key) { return extra.get(key); }

    public double getExtraDouble(String key, double defaultValue) {
        Object v = extra.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return defaultValue;
    }

    public void setExtra(String key, Object value) {
        if (key != null && !key.isBlank()) extra.put(key, value);
    }

    public void removeExtra(String key) { extra.remove(key); }

    public Set<String> getExtraKeys() { return extra.keySet(); }

    public boolean hasExtra(String key) { return extra.containsKey(key); }

    // ================= JSON 输出（顺序稳定，便于 diff）=================

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        // ① 玩家开局
        sb.append("  \"initialGold\": ").append(num(initialGold)).append(",\n");
        sb.append("  \"initialLives\": ").append(num(initialLives)).append(",\n");
        sb.append("  \"totalWaves\": ").append(num(totalWaves)).append(",\n");
        // ② 普通敌人
        sb.append("  \"normalHp\": ").append(num(normalHp)).append(",\n");
        sb.append("  \"normalSpeed\": ").append(num(normalSpeed)).append(",\n");
        sb.append("  \"normalGoldReward\": ").append(num(normalGoldReward)).append(",\n");
        // ③ 快速敌人
        sb.append("  \"fastHp\": ").append(num(fastHp)).append(",\n");
        sb.append("  \"fastSpeed\": ").append(num(fastSpeed)).append(",\n");
        sb.append("  \"fastGoldReward\": ").append(num(fastGoldReward)).append(",\n");
        // ④ 重甲敌人
        sb.append("  \"tankHp\": ").append(num(tankHp)).append(",\n");
        sb.append("  \"tankSpeed\": ").append(num(tankSpeed)).append(",\n");
        sb.append("  \"tankGoldReward\": ").append(num(tankGoldReward)).append(",\n");
        // ⑤ Boss
        sb.append("  \"bossHp\": ").append(num(bossHp)).append(",\n");
        sb.append("  \"bossSpeed\": ").append(num(bossSpeed)).append(",\n");
        sb.append("  \"bossGoldReward\": ").append(num(bossGoldReward)).append(",\n");
        // ⑥ 敌人近战：攻击力 / 攻击冷却
        sb.append("  \"normalAttackDamage\": ").append(num(normalAttackDamage)).append(",\n");
        sb.append("  \"normalAttackCooldownMs\": ").append(num(normalAttackCooldownMs)).append(",\n");
        sb.append("  \"fastAttackDamage\": ").append(num(fastAttackDamage)).append(",\n");
        sb.append("  \"fastAttackCooldownMs\": ").append(num(fastAttackCooldownMs)).append(",\n");
        sb.append("  \"tankAttackDamage\": ").append(num(tankAttackDamage)).append(",\n");
        sb.append("  \"tankAttackCooldownMs\": ").append(num(tankAttackCooldownMs)).append(",\n");
        sb.append("  \"bossAttackDamage\": ").append(num(bossAttackDamage)).append(",\n");
        sb.append("  \"bossAttackCooldownMs\": ").append(num(bossAttackCooldownMs)).append(",\n");
        // ⑦ 重甲物理减伤
        sb.append("  \"tankPhysicalReduction\": ").append(num(tankPhysicalReduction)).append(",\n");
        // ⑧ 炮塔溅射半径
        sb.append("  \"cannonSplashRadius\": ").append(num(cannonSplashRadius)).append(",\n");
        sb.append("  \"eliteCannonSplashRadius\": ").append(num(eliteCannonSplashRadius)).append(",\n");
        sb.append("  \"masterCannonSplashRadius\": ").append(num(masterCannonSplashRadius)).append(",\n");
        // ⑨ Boss 狂暴机制
        sb.append("  \"bossStompIntervalMs\": ").append(num(bossStompIntervalMs)).append(",\n");
        sb.append("  \"bossEnrageAttackCooldownCut\": ").append(num(bossEnrageAttackCooldownCut));
        // 动态扩展字段追加在后面
        if (!extra.isEmpty()) {
            sb.append(",\n");
            int i = 0;
            for (Map.Entry<String, Object> e : extra.entrySet()) {
                sb.append("  \"").append(escape(e.getKey())).append("\": ")
                        .append(valueToJson(e.getValue()));
                if (i < extra.size() - 1) sb.append(",");
                sb.append("\n");
                i++;
            }
        } else {
            sb.append("\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static String escape(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String num(long v) { return String.valueOf(v); }
    private static String num(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }

    private static String valueToJson(Object v) {
        if (v instanceof Number) {
            double d = ((Number) v).doubleValue();
            return num(d);
        }
        if (v == null) return "null";
        return "\"" + escape(String.valueOf(v)) + "\"";
    }

    // ================= JSON 输入（逐字段容错，不抛业务异常）=================

    public static BalanceTable fromJson(String json) {
        Object root = new MiniJson(json).parse();
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("JSON 顶层必须是对象");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = (Map<String, Object>) root;

        BalanceTable def = defaults();
        BalanceTable b = new BalanceTable();

        // ① 玩家开局
        b.initialGold = readInt(obj, "initialGold", def.initialGold, 0);
        b.initialLives = readInt(obj, "initialLives", def.initialLives, 1);
        b.totalWaves = readInt(obj, "totalWaves", def.totalWaves, 1);
        // ② 普通敌人
        b.normalHp = readInt(obj, "normalHp", def.normalHp, 1);
        b.normalSpeed = readDouble(obj, "normalSpeed", def.normalSpeed, 0.0, false);
        b.normalGoldReward = readInt(obj, "normalGoldReward", def.normalGoldReward, 0);
        // ③ 快速敌人
        b.fastHp = readInt(obj, "fastHp", def.fastHp, 1);
        b.fastSpeed = readDouble(obj, "fastSpeed", def.fastSpeed, 0.0, false);
        b.fastGoldReward = readInt(obj, "fastGoldReward", def.fastGoldReward, 0);
        // ④ 重甲敌人
        b.tankHp = readInt(obj, "tankHp", def.tankHp, 1);
        b.tankSpeed = readDouble(obj, "tankSpeed", def.tankSpeed, 0.0, false);
        b.tankGoldReward = readInt(obj, "tankGoldReward", def.tankGoldReward, 0);
        // ⑤ Boss
        b.bossHp = readInt(obj, "bossHp", def.bossHp, 1);
        b.bossSpeed = readDouble(obj, "bossSpeed", def.bossSpeed, 0.0, false);
        b.bossGoldReward = readInt(obj, "bossGoldReward", def.bossGoldReward, 0);
        // ⑥ 敌人近战：攻击力（≥1）/ 冷却（≥1；0 冷却 = 无限攻速，禁止）
        b.normalAttackDamage = readInt(obj, "normalAttackDamage", def.normalAttackDamage, 1);
        b.normalAttackCooldownMs = readInt(obj, "normalAttackCooldownMs", def.normalAttackCooldownMs, 1);
        b.fastAttackDamage = readInt(obj, "fastAttackDamage", def.fastAttackDamage, 1);
        b.fastAttackCooldownMs = readInt(obj, "fastAttackCooldownMs", def.fastAttackCooldownMs, 1);
        b.tankAttackDamage = readInt(obj, "tankAttackDamage", def.tankAttackDamage, 1);
        b.tankAttackCooldownMs = readInt(obj, "tankAttackCooldownMs", def.tankAttackCooldownMs, 1);
        b.bossAttackDamage = readInt(obj, "bossAttackDamage", def.bossAttackDamage, 1);
        b.bossAttackCooldownMs = readInt(obj, "bossAttackCooldownMs", def.bossAttackCooldownMs, 1);
        // ⑦ 重甲物理减伤（比例，必须 0 ≤ v < 1）
        b.tankPhysicalReduction = readRatio(obj, "tankPhysicalReduction", def.tankPhysicalReduction);
        // ⑧ 炮塔溅射半径（必须 > 0；0/负数 = 溅射永不命中，禁止）
        b.cannonSplashRadius = readDouble(obj, "cannonSplashRadius", def.cannonSplashRadius, 0.0, false);
        b.eliteCannonSplashRadius = readDouble(obj, "eliteCannonSplashRadius", def.eliteCannonSplashRadius, 0.0, false);
        b.masterCannonSplashRadius = readDouble(obj, "masterCannonSplashRadius", def.masterCannonSplashRadius, 0.0, false);
        // ⑨ Boss 狂暴机制：震地间隔（≥1ms）/ 冷却削减比例（[0,1)，上界开区间同 tankPhysicalReduction）
        b.bossStompIntervalMs = readInt(obj, "bossStompIntervalMs", def.bossStompIntervalMs, 1);
        b.bossEnrageAttackCooldownCut = readRatio(obj, "bossEnrageAttackCooldownCut", def.bossEnrageAttackCooldownCut);

        // 未知键 → 收入动态扩展字段（打印告警：这些字段运行期无人读取，避免"配了不生效却无提示"）
        for (Map.Entry<String, Object> e : obj.entrySet()) {
            if (!FIXED_KEYS.contains(e.getKey())) {
                System.err.println("[BalanceTable] 未知字段 \"" + e.getKey()
                        + "\" 已忽略（运行期不读取，请勿在此写无对应实现的数值）");
                b.extra.put(e.getKey(), e.getValue());
            }
        }

        return b;
    }

    private static int readInt(Map<String, Object> obj, String key, int def, int min) {
        Object v = obj.get(key);
        if (v instanceof Number) {
            int val = ((Number) v).intValue();
            if (val >= min) return val;
            warn(key, v, "小于最小值 " + min);
        } else if (v != null) {
            warn(key, v, "非数字");
        }
        return def;
    }

    private static double readDouble(Map<String, Object> obj, String key, double def, double min, boolean allowEq) {
        Object v = obj.get(key);
        if (v instanceof Number) {
            double val = ((Number) v).doubleValue();
            if (allowEq ? val >= min : val > min) return val;
            warn(key, v, allowEq ? "小于最小值 " + min : "必须大于 " + min);
        } else if (v != null) {
            warn(key, v, "非数字");
        }
        return def;
    }

    /** 比例字段：必须 0 ≤ v < 1（上界为开区间，readDouble 无法表达，故单列） */
    private static double readRatio(Map<String, Object> obj, String key, double def) {
        Object v = obj.get(key);
        if (v instanceof Number) {
            double val = ((Number) v).doubleValue();
            if (val >= 0.0 && val < 1.0) return val;
            warn(key, v, "必须位于 [0, 1)");
        } else if (v != null) {
            warn(key, v, "非数字");
        }
        return def;
    }

    private static void warn(String key, Object actual, String reason) {
        System.err.println("[BalanceTable] 字段 \"" + key + "\" 非法（" + reason + "），实际: " + actual + " → 回退内置默认");
    }

    // ================= classpath 加载（运行期用）=================

    /**
     * 运行期共享实例（只读）：首次访问时从 classpath 加载一次并缓存，
     * 缺失/解析失败 → 回退 {@link #defaults()}，永不返回 null、不抛异常。
     *
     * 供**实体构造期**取值使用（如炮塔读 ⑧ 溅射半径），对齐
     * {@code util.asset.SizeTable.getInstance()} 的既有范式：实体不持有 GameConfig 引用，
     * 又需要 classpath 数值时走这里。GameConfig 仍走 {@link #loadFromClasspath()} 各自加载。
     *
     * ⚠️ 只读，不提供热重载：改 balance.json 后需重启进程生效（同 GameConfig 契约）。
     */
    public static synchronized BalanceTable runtime() {
        if (runtimeInstance == null) {
            BalanceTable loaded = loadFromClasspath();
            runtimeInstance = (loaded != null) ? loaded : defaults();
        }
        return runtimeInstance;
    }

    /** 运行期共享实例（懒加载缓存，见 {@link #runtime()}） */
    private static BalanceTable runtimeInstance;

    public static BalanceTable loadFromClasspath() {
        try (InputStream in = BalanceTable.class.getResourceAsStream("/config/balance.json")) {
            if (in == null) {
                System.err.println("[BalanceTable] 未找到 /config/balance.json → 整表回退内置默认");
                return null;
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return fromJson(json);
        } catch (IOException | RuntimeException e) {
            System.err.println("[BalanceTable] 加载 /config/balance.json 失败: " + e.getMessage() + " → 整表回退内置默认");
            return null;
        }
    }
}
