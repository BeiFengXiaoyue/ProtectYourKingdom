package com.kingdom.game.model.tower;

import com.kingdom.game.model.TowerParams;
import com.kingdom.game.model.TowerType;

/**
 * TowerParamsDefaults —— 塔数值的**默认值集中处**（三族 × 三档，共 9 份）。
 *
 * 这里是"接线人员改数值"的**唯一入口**：塔类不再持有任何数值常量，
 * 装配处（`Main`）登记时从这里取一份 {@link TowerParams} 传给塔构造函数。
 * 想调某个塔的伤害/射程/冷却/造价/士兵参数/3 级能力参数，改本类对应方法即可，
 * 不必碰任何塔类，也不必碰控制器。
 *
 * 数值来源：战斗值 =《建筑与怪物机制策划 v1.1》；造价 = PRD（50/80/100）。
 * ⚠ 本轮迁移**不改任何数值**，以下数值与迁移前各塔类构造函数中的值逐一对应。
 */
public final class TowerParamsDefaults {

    private TowerParamsDefaults() { }   // 工具类，不实例化

    // ==================== 箭塔族 ====================

    /** 箭塔（1 级）：造价 50 / 升级费 75 → 精英箭塔 */
    public static TowerParams arrowL1() {
        return new TowerParams()
                .setDisplayName("箭塔").setLevel(1)
                .setBuildCost(50).setUpgradeCost(75).setTotalCost(50)
                .setNextLevelType(TowerType.ARROW_ELITE).setNextLevelName("精英箭塔")
                .setDamage(18).setRange(120).setCooldownMs(550).setProjectileSpeed(320);
    }

    /** 精英箭塔（2 级）：升级费 120 → 大师箭塔 */
    public static TowerParams arrowL2() {
        return new TowerParams()
                .setDisplayName("精英箭塔").setLevel(2)
                .setUpgradeCost(120).setTotalCost(125)
                .setNextLevelType(TowerType.ARROW_MASTER).setNextLevelName("大师箭塔")
                .setDamage(27).setRange(140).setCooldownMs(550).setProjectileSpeed(320);
    }

    /** 大师箭塔（3 级，满级）：+ 连锁箭（目标 1 / 伤害比 0.5） */
    public static TowerParams arrowL3() {
        return new TowerParams()
                .setDisplayName("大师箭塔").setLevel(3)
                .setTotalCost(245)
                .setNextLevelType(null).setNextLevelName("")
                .setDamage(41).setRange(140).setCooldownMs(440).setProjectileSpeed(320)
                .setChainTargets(1).setChainDamageRatio(0.5);
    }

    // ==================== 炮塔族 ====================

    /** 炮塔（1 级）：造价 80 / 升级费 150 → 精英炮塔 */
    public static TowerParams cannonL1() {
        return new TowerParams()
                .setDisplayName("炮塔").setLevel(1)
                .setBuildCost(80).setUpgradeCost(150).setTotalCost(80)
                .setNextLevelType(TowerType.CANNON_ELITE).setNextLevelName("精英炮塔")
                .setDamage(45).setRange(140).setCooldownMs(1500)
                .setSplashRadius(50).setProjectileSpeed(220);
    }

    /** 精英炮塔（2 级）：升级费 250 → 大师炮塔 */
    public static TowerParams cannonL2() {
        return new TowerParams()
                .setDisplayName("精英炮塔").setLevel(2)
                .setUpgradeCost(250).setTotalCost(230)
                .setNextLevelType(TowerType.CANNON_MASTER).setNextLevelName("大师炮塔")
                .setDamage(70).setRange(140).setCooldownMs(1500)
                .setSplashRadius(65).setProjectileSpeed(220);
    }

    /** 大师炮塔（3 级，满级）：+ 燃烧（每秒 5 / 持续 3000ms） */
    public static TowerParams cannonL3() {
        return new TowerParams()
                .setDisplayName("大师炮塔").setLevel(3)
                .setTotalCost(480)
                .setNextLevelType(null).setNextLevelName("")
                .setDamage(110).setRange(140).setCooldownMs(1500)
                .setSplashRadius(80).setProjectileSpeed(220)
                .setBurnDps(5).setBurnDurationMs(3000);
    }

    // ==================== 兵营族 ====================

    /** 兵营（1 级）：造价 100 / 升级费 100 → 精英兵营 */
    public static TowerParams barrackL1() {
        return new TowerParams()
                .setDisplayName("兵营").setLevel(1)
                .setBuildCost(100).setUpgradeCost(100).setTotalCost(100)
                .setNextLevelType(TowerType.BARRACK_ELITE).setNextLevelName("精英兵营")
                .setMaxSoldiers(2).setSoldierHp(50).setSoldierSpeed(40)
                .setSoldierAttack(8).setSoldierCooldownMs(800).setSpawnIntervalMs(5000);
    }

    /** 精英兵营（2 级）：升级费 180 → 大师兵营 */
    public static TowerParams barrackL2() {
        return new TowerParams()
                .setDisplayName("精英兵营").setLevel(2)
                .setUpgradeCost(180).setTotalCost(200)
                .setNextLevelType(TowerType.BARRACK_MASTER).setNextLevelName("大师兵营")
                .setMaxSoldiers(3).setSoldierHp(80).setSoldierSpeed(40)
                .setSoldierAttack(8).setSoldierCooldownMs(800).setSpawnIntervalMs(5000);
    }

    /** 大师兵营（3 级，满级）：+ 治疗光环（每秒 +2 / 半径 80） */
    public static TowerParams barrackL3() {
        return new TowerParams()
                .setDisplayName("大师兵营").setLevel(3)
                .setTotalCost(380)
                .setNextLevelType(null).setNextLevelName("")
                .setMaxSoldiers(4).setSoldierHp(80).setSoldierSpeed(40)
                .setSoldierAttack(8).setSoldierCooldownMs(800).setSpawnIntervalMs(5000)
                .setHealPerSecond(2).setAuraRadius(80);
    }
}
