package com.kingdom.game.model.tower;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for 9 tower classes: level, damage, range, upgrade chain, cost.
 * Values from config/tower.json (TowerBalance).
 */
class TowerClassesTest {

    // ================= Arrow Tower Series =================

    @Test
    void arrowL1_level1_damage10_range120() {
        ArrowTower t = new ArrowTower(0, 0);
        assertEquals(1, t.getLevel());
        assertEquals(10, t.getBaseAttackDamage());
        assertEquals(120.0, t.getAttackRange());
        assertEquals(50, ArrowTower.BUILD_COST);
    }

    @Test
    void arrowL1_upgradable_notMaxLevel() {
        ArrowTower t = new ArrowTower(0, 0);
        assertFalse(t.isMaxLevel());
        assertNotNull(t.getNextLevelSpec());
    }

    @Test
    void arrowL2_level2_damage15_range140() {
        EliteArrowTower t = new EliteArrowTower(0, 0);
        assertEquals(2, t.getLevel());
        assertEquals(15, t.getBaseAttackDamage());
        assertEquals(140.0, t.getAttackRange());
    }

    @Test
    void arrowL2_upgradable_notMaxLevel() {
        EliteArrowTower t = new EliteArrowTower(0, 0);
        assertFalse(t.isMaxLevel());
        assertNotNull(t.getNextLevelSpec());
    }

    @Test
    void arrowL3_level3_damage21_range140() {
        MasterArrowTower t = new MasterArrowTower(0, 0);
        assertEquals(3, t.getLevel());
        assertEquals(21, t.getBaseAttackDamage());
        assertEquals(140.0, t.getAttackRange());
    }

    @Test
    void arrowL3_maxLevel_noUpgrade() {
        MasterArrowTower t = new MasterArrowTower(0, 0);
        assertTrue(t.isMaxLevel());
        assertNull(t.getNextLevelSpec());
    }

    @Test
    void arrowDamage_increasesWithLevel() {
        ArrowTower l1 = new ArrowTower(0, 0);
        EliteArrowTower l2 = new EliteArrowTower(0, 0);
        MasterArrowTower l3 = new MasterArrowTower(0, 0);
        assertTrue(l2.getBaseAttackDamage() > l1.getBaseAttackDamage());
        assertTrue(l3.getBaseAttackDamage() > l2.getBaseAttackDamage());
    }

    // ================= Cannon Tower Series =================

    @Test
    void cannonL1_level1_damage30_range140() {
        CannonTower t = new CannonTower(0, 0);
        assertEquals(1, t.getLevel());
        assertEquals(30, t.getBaseAttackDamage());
        assertEquals(140.0, t.getAttackRange());
        assertEquals(100, CannonTower.BUILD_COST);
    }

    @Test
    void cannonL1_upgradable_notMaxLevel() {
        CannonTower t = new CannonTower(0, 0);
        assertFalse(t.isMaxLevel());
        assertNotNull(t.getNextLevelSpec());
    }

    @Test
    void cannonL2_level2_damage42_range140() {
        EliteCannonTower t = new EliteCannonTower(0, 0);
        assertEquals(2, t.getLevel());
        assertEquals(42, t.getBaseAttackDamage());
        assertEquals(140.0, t.getAttackRange());
    }

    @Test
    void cannonL3_level3_damage55_range140() {
        MasterCannonTower t = new MasterCannonTower(0, 0);
        assertEquals(3, t.getLevel());
        assertEquals(55, t.getBaseAttackDamage());
        assertEquals(140.0, t.getAttackRange());
    }

    @Test
    void cannonL3_maxLevel_noUpgrade() {
        MasterCannonTower t = new MasterCannonTower(0, 0);
        assertTrue(t.isMaxLevel());
        assertNull(t.getNextLevelSpec());
    }

    @Test
    void cannonDamage_higherThanArrow_sameLevel() {
        CannonTower cannon = new CannonTower(0, 0);
        ArrowTower arrow = new ArrowTower(0, 0);
        assertTrue(cannon.getBaseAttackDamage() > arrow.getBaseAttackDamage());
    }

    // ================= Barrack Series =================

    @Test
    void barrackL1_level1_noDirectAttack() {
        Barrack t = new Barrack(0, 0);
        assertEquals(1, t.getLevel());
        assertEquals(0, t.getBaseAttackDamage());
        assertEquals(0.0, t.getAttackRange());
        assertEquals(100, Barrack.BUILD_COST);
    }

    @Test
    void barrackL1_upgradable_notMaxLevel() {
        Barrack t = new Barrack(0, 0);
        assertFalse(t.isMaxLevel());
        assertNotNull(t.getNextLevelSpec());
    }

    @Test
    void barrackL2_level2_noDirectAttack() {
        EliteBarrack t = new EliteBarrack(0, 0);
        assertEquals(2, t.getLevel());
        assertEquals(0, t.getBaseAttackDamage());
        assertEquals(0.0, t.getAttackRange());
    }

    @Test
    void barrackL3_level3_noDirectAttack() {
        MasterBarrack t = new MasterBarrack(0, 0);
        assertEquals(3, t.getLevel());
        assertEquals(0, t.getBaseAttackDamage());
        assertEquals(0.0, t.getAttackRange());
    }

    @Test
    void barrackL3_maxLevel_noUpgrade() {
        MasterBarrack t = new MasterBarrack(0, 0);
        assertTrue(t.isMaxLevel());
        assertNull(t.getNextLevelSpec());
    }

    // ================= Common Properties =================

    @Test
    void allTowers_initiallyNotStunned() {
        assertFalse(new ArrowTower(0, 0).isStunned());
        assertFalse(new CannonTower(0, 0).isStunned());
        assertFalse(new Barrack(0, 0).isStunned());
        assertFalse(new EliteArrowTower(0, 0).isStunned());
        assertFalse(new MasterArrowTower(0, 0).isStunned());
    }

    @Test
    void allTowers_totalCost_equalsBuildCost() {
        assertEquals(50, new ArrowTower(0, 0).getTotalCost());
        assertEquals(100, new CannonTower(0, 0).getTotalCost());
        assertEquals(100, new Barrack(0, 0).getTotalCost());
    }

    @Test
    void allLevel2Towers_haveNextLevel() {
        assertFalse(new EliteArrowTower(0, 0).isMaxLevel());
        assertFalse(new EliteCannonTower(0, 0).isMaxLevel());
        assertFalse(new EliteBarrack(0, 0).isMaxLevel());
    }

    @Test
    void allLevel3Towers_areMaxLevel() {
        assertTrue(new MasterArrowTower(0, 0).isMaxLevel());
        assertTrue(new MasterCannonTower(0, 0).isMaxLevel());
        assertTrue(new MasterBarrack(0, 0).isMaxLevel());
    }
}
