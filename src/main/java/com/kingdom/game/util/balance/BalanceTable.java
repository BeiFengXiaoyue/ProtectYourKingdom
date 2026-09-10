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
 * 当前纳入 JSON 化的字段（15 个，分 5 组）：
 * - ① 玩家开局：initialGold / initialLives / totalWaves
 * - ② 普通敌人：normalHp / normalSpeed / normalGoldReward
 * - ③ 快速敌人：fastHp / fastSpeed / fastGoldReward
 * - ④ 重甲敌人：tankHp / tankSpeed / tankGoldReward
 * - ⑤ Boss：bossHp / bossSpeed / bossGoldReward
 *
 * 波次节奏字段（waveEnemyCount / waveSpawnIntervalMs / waveIntermissionMs /
 * earlyStartRewardCap）不纳入 JSON 化，仍由 GameConfig 字面量默认值管理，
 * 避免与波次模块并行开发产生合并冲突。
 *
 * 职责：
 * - 承载 config/balance.json 的固定数值字段（强类型，有 getter/setter）；
 * - 额外支持动态扩展字段（extra Map），供编辑器添加未来新增的变量；
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

    /** 动态扩展字段：编辑器添加的未来变量，fromJson 未知键自动收入此 Map，toJson 一并写出 */
    private final Map<String, Object> extra = new LinkedHashMap<>();

    private BalanceTable() { }

    // ================= 内置默认（唯一来源）=================

    /** 内置默认 = 策划文档 v1.1 数值表（seed JSON 必须与此一致） */
    public static BalanceTable defaults() {
        BalanceTable b = new BalanceTable();
        // 玩家开局
        b.initialGold = 100;
        b.initialLives = 20;
        b.totalWaves = 10;
        // 普通敌人
        b.normalHp = 80;
        b.normalSpeed = 60.0;
        b.normalGoldReward = 10;
        // 快速敌人（速度约普通 2 倍，HP 低，赏金略高）
        b.fastHp = 50;
        b.fastSpeed = 120.0;
        b.fastGoldReward = 15;
        // 重甲敌人（HP 约普通 3 倍，速度慢，赏金高）
        b.tankHp = 240;
        b.tankSpeed = 35.0;
        b.tankGoldReward = 25;
        // Boss（高血量，最终波首领）
        b.bossHp = 1500;
        b.bossSpeed = 40.0;
        b.bossGoldReward = 200;
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

    // ================= 动态扩展字段 =================

    /** 已知的固定字段键集合（fromJson 时用于区分固定字段与扩展字段） */
    private static final Set<String> FIXED_KEYS = Set.of(
            "initialGold", "initialLives", "totalWaves",
            "normalHp", "normalSpeed", "normalGoldReward",
            "fastHp", "fastSpeed", "fastGoldReward",
            "tankHp", "tankSpeed", "tankGoldReward",
            "bossHp", "bossSpeed", "bossGoldReward");

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
        sb.append("  \"bossGoldReward\": ").append(num(bossGoldReward));
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

        // 未知键 → 收入动态扩展字段
        for (Map.Entry<String, Object> e : obj.entrySet()) {
            if (!FIXED_KEYS.contains(e.getKey())) {
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

    private static void warn(String key, Object actual, String reason) {
        System.err.println("[BalanceTable] 字段 \"" + key + "\" 非法（" + reason + "），实际: " + actual + " → 回退内置默认");
    }

    // ================= classpath 加载（运行期用）=================

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
