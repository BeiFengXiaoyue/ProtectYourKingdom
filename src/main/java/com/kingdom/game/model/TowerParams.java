package com.kingdom.game.model;

/**
 * TowerParams —— 塔的**数值参数载体**（一个通用类，三族共用）。
 *
 * 设计意图（PM 要求）：塔的数值**不再以 {@code final} 常量写死在各个塔类里**，
 * 而是集中放进本类，由**构造期注入**给塔（`new ArrowTower(x, y, params)`）。
 * 因此本类**不是 final、字段也不是 final**，并提供链式 setter —— 接线人员（装配处）
 * 想改任何数值，只改一处（{@code TowerParamsDefaults}）即可，不必改塔类。
 *
 * 分组：
 * - 目录级：显示名 / 等级 / 造价 / 升级费 / 下一级 / 累计投入（累计投入决定出售返还 totalCost/2）
 * - 战斗级：伤害 / 射程 / 攻击间隔 / 溅射半径 / 投射物速度
 * - 兵营：士兵数上限 / 士兵 HP·速度·攻击·冷却 / 出场·补位间隔
 * - 3 级能力：连锁箭 / 燃烧 / 治疗光环 的参数（未用到的塔保持 0）
 *
 * 注：`totalCost` 与「造价 + 各级升级费」等价，属派生值，这里直接给出便于实现；
 * 数值来源见《建筑与怪物机制策划 v1.1》（战斗值）与 PRD（造价）。
 */
public class TowerParams {

    // ===== 目录级 =====
    private String displayName = "";
    private int level = 1;
    private int buildCost;
    /** 升到下一级的费用；满级为 0 */
    private int upgradeCost;
    /** 下一级的类型；null = 已满级 */
    private TowerType nextLevelType;
    /** 下一级的显示名（供 ITowerUpgrade.getNextLevelSpec 构造目录条目） */
    private String nextLevelName = "";
    /** 累计投入（= 造价 + 各级升级费），出售返还按其一半 */
    private int totalCost;

    // ===== 战斗级 =====
    private int damage;
    private double range;
    private int cooldownMs;
    private double splashRadius;
    private double projectileSpeed;

    // ===== 兵营 =====
    private int maxSoldiers;
    private int soldierHp;
    private double soldierSpeed;
    private int soldierAttack;
    private int soldierCooldownMs;
    private long spawnIntervalMs;

    // ===== 3 级能力 =====
    private int chainTargets;
    private double chainDamageRatio;
    private int burnDps;
    private int burnDurationMs;
    private int healPerSecond;
    private double auraRadius;

    // ===== 目录级 =====
    public String getDisplayName() { return displayName; }
    public TowerParams setDisplayName(String v) { this.displayName = v; return this; }

    public int getLevel() { return level; }
    public TowerParams setLevel(int v) { this.level = v; return this; }

    public int getBuildCost() { return buildCost; }
    public TowerParams setBuildCost(int v) { this.buildCost = v; return this; }

    public int getUpgradeCost() { return upgradeCost; }
    public TowerParams setUpgradeCost(int v) { this.upgradeCost = v; return this; }

    public TowerType getNextLevelType() { return nextLevelType; }
    public TowerParams setNextLevelType(TowerType v) { this.nextLevelType = v; return this; }

    public String getNextLevelName() { return nextLevelName; }
    public TowerParams setNextLevelName(String v) { this.nextLevelName = v; return this; }

    public int getTotalCost() { return totalCost; }
    public TowerParams setTotalCost(int v) { this.totalCost = v; return this; }

    // ===== 战斗级 =====
    public int getDamage() { return damage; }
    public TowerParams setDamage(int v) { this.damage = v; return this; }

    public double getRange() { return range; }
    public TowerParams setRange(double v) { this.range = v; return this; }

    public int getCooldownMs() { return cooldownMs; }
    public TowerParams setCooldownMs(int v) { this.cooldownMs = v; return this; }

    public double getSplashRadius() { return splashRadius; }
    public TowerParams setSplashRadius(double v) { this.splashRadius = v; return this; }

    public double getProjectileSpeed() { return projectileSpeed; }
    public TowerParams setProjectileSpeed(double v) { this.projectileSpeed = v; return this; }

    // ===== 兵营 =====
    public int getMaxSoldiers() { return maxSoldiers; }
    public TowerParams setMaxSoldiers(int v) { this.maxSoldiers = v; return this; }

    public int getSoldierHp() { return soldierHp; }
    public TowerParams setSoldierHp(int v) { this.soldierHp = v; return this; }

    public double getSoldierSpeed() { return soldierSpeed; }
    public TowerParams setSoldierSpeed(double v) { this.soldierSpeed = v; return this; }

    public int getSoldierAttack() { return soldierAttack; }
    public TowerParams setSoldierAttack(int v) { this.soldierAttack = v; return this; }

    public int getSoldierCooldownMs() { return soldierCooldownMs; }
    public TowerParams setSoldierCooldownMs(int v) { this.soldierCooldownMs = v; return this; }

    public long getSpawnIntervalMs() { return spawnIntervalMs; }
    public TowerParams setSpawnIntervalMs(long v) { this.spawnIntervalMs = v; return this; }

    // ===== 3 级能力 =====
    public int getChainTargets() { return chainTargets; }
    public TowerParams setChainTargets(int v) { this.chainTargets = v; return this; }

    public double getChainDamageRatio() { return chainDamageRatio; }
    public TowerParams setChainDamageRatio(double v) { this.chainDamageRatio = v; return this; }

    public int getBurnDps() { return burnDps; }
    public TowerParams setBurnDps(int v) { this.burnDps = v; return this; }

    public int getBurnDurationMs() { return burnDurationMs; }
    public TowerParams setBurnDurationMs(int v) { this.burnDurationMs = v; return this; }

    public int getHealPerSecond() { return healPerSecond; }
    public TowerParams setHealPerSecond(int v) { this.healPerSecond = v; return this; }

    public double getAuraRadius() { return auraRadius; }
    public TowerParams setAuraRadius(double v) { this.auraRadius = v; return this; }
}
