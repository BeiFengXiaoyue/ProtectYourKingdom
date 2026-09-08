package com.kingdom.game.model;

/**
 * Faction —— 阵营枚举。
 *
 * 设计为可增量扩展：
 * - 现有两类：ALLY（友方）/ ENEMY（敌方）；
 * - 新增阵营只需在此追加一个常量，所有使用方统一经 {@link LivingEntity#getFaction()} 读取，
 *   不散落 instanceof 判断；
 * - 阵营可在运行期通过 setFaction 修改（如魅惑/策反），由 LivingEntity 承载。
 */
public enum Faction {
    ALLY,
    ENEMY
}
