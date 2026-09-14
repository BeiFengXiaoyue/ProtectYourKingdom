package com.kingdom.game.util.balance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TowerBalance: 9 tower types in tower.json + fallback behavior.
 */
class TowerBalanceTest {

    // ================= Arrow Tower Series =================

    @Test
    void arrowL1_damage_is10() {
        assertEquals(10, TowerBalance.getInt("arrowL1", "damage", 0));
    }

    @Test
    void arrowL1_range_is120() {
        assertEquals(120.0, TowerBalance.getDouble("arrowL1", "range", 0));
    }

    @Test
    void arrowL1_cooldownMs_is1000() {
        assertEquals(1000, TowerBalance.getInt("arrowL1", "cooldownMs", 0));
    }

    @Test
    void arrowL2_damage_is15() {
        assertEquals(15, TowerBalance.getInt("arrowL2", "damage", 0));
    }

    @Test
    void arrowL2_range_is140() {
        assertEquals(140.0, TowerBalance.getDouble("arrowL2", "range", 0));
    }

    @Test
    void arrowL3_damage_is21() {
        assertEquals(21, TowerBalance.getInt("arrowL3", "damage", 0));
    }

    @Test
    void arrowL3_hasChainTargets() {
        assertTrue(TowerBalance.getInt("arrowL3", "chainTargets", 0) > 0);
    }

    @Test
    void arrowL3_chainDamageRatio_is50Percent() {
        assertEquals(0.5, TowerBalance.getDouble("arrowL3", "chainDamageRatio", 0), 0.001);
    }

    // ================= Cannon Tower Series =================

    @Test
    void cannonL1_damage_is30() {
        assertEquals(30, TowerBalance.getInt("cannonL1", "damage", 0));
    }

    @Test
    void cannonL1_range_is140() {
        assertEquals(140.0, TowerBalance.getDouble("cannonL1", "range", 0));
    }

    @Test
    void cannonL1_cooldownMs_is2400() {
        assertEquals(2400, TowerBalance.getInt("cannonL1", "cooldownMs", 0));
    }

    @Test
    void cannonL2_damage_is42() {
        assertEquals(42, TowerBalance.getInt("cannonL2", "damage", 0));
    }

    @Test
    void cannonL3_damage_is55() {
        assertEquals(55, TowerBalance.getInt("cannonL3", "damage", 0));
    }

    @Test
    void cannonL3_hasBurnDps() {
        assertTrue(TowerBalance.getInt("cannonL3", "burnDps", 0) > 0);
    }

    @Test
    void cannonL3_burnDurationMs_is3000() {
        assertEquals(3000, TowerBalance.getInt("cannonL3", "burnDurationMs", 0));
    }

    // ================= Barrack Series =================

    @Test
    void barrackL1_maxSoldiers_is2() {
        assertEquals(2, TowerBalance.getInt("barrackL1", "maxSoldiers", 0));
    }

    @Test
    void barrackL1_soldierHp_is50() {
        assertEquals(50, TowerBalance.getInt("barrackL1", "soldierHp", 0));
    }

    @Test
    void barrackL1_soldierAttack_is4() {
        assertEquals(4, TowerBalance.getInt("barrackL1", "soldierAttack", 0));
    }

    @Test
    void barrackL2_maxSoldiers_is3() {
        assertEquals(3, TowerBalance.getInt("barrackL2", "maxSoldiers", 0));
    }

    @Test
    void barrackL2_soldierHp_is80() {
        assertEquals(80, TowerBalance.getInt("barrackL2", "soldierHp", 0));
    }

    @Test
    void barrackL3_maxSoldiers_is4() {
        assertEquals(4, TowerBalance.getInt("barrackL3", "maxSoldiers", 0));
    }

    @Test
    void barrackL3_hasHealPerSecond() {
        assertTrue(TowerBalance.getInt("barrackL3", "healPerSecond", 0) > 0);
    }

    // ================= Progression Sanity =================

    @Test
    void arrowDamage_increasesWithLevel() {
        int l1 = TowerBalance.getInt("arrowL1", "damage", 0);
        int l2 = TowerBalance.getInt("arrowL2", "damage", 0);
        int l3 = TowerBalance.getInt("arrowL3", "damage", 0);
        assertTrue(l2 > l1);
        assertTrue(l3 > l2);
    }

    @Test
    void cannonDamage_increasesWithLevel() {
        int l1 = TowerBalance.getInt("cannonL1", "damage", 0);
        int l2 = TowerBalance.getInt("cannonL2", "damage", 0);
        int l3 = TowerBalance.getInt("cannonL3", "damage", 0);
        assertTrue(l2 > l1);
        assertTrue(l3 > l2);
    }

    @Test
    void barrackMaxSoldiers_increasesWithLevel() {
        int l1 = TowerBalance.getInt("barrackL1", "maxSoldiers", 0);
        int l2 = TowerBalance.getInt("barrackL2", "maxSoldiers", 0);
        int l3 = TowerBalance.getInt("barrackL3", "maxSoldiers", 0);
        assertTrue(l2 >= l1);
        assertTrue(l3 >= l2);
    }

    // ================= Fallback =================

    @Test
    void nonexistentTowerKey_returnsDefault() {
        assertEquals(999, TowerBalance.getInt("nonexistent", "damage", 999));
    }

    @Test
    void nonexistentField_returnsDefault() {
        assertEquals(999.0, TowerBalance.getDouble("arrowL1", "nonexistent", 999.0));
    }

    @Test
    void allTowerRanges_positive() {
        for (String key : new String[]{"arrowL1", "arrowL2", "arrowL3", "cannonL1", "cannonL2", "cannonL3"}) {
            assertTrue(TowerBalance.getDouble(key, "range", 0) > 0, key + " range should be positive");
        }
    }

    @Test
    void allTowerDamages_positive() {
        for (String key : new String[]{"arrowL1", "arrowL2", "arrowL3", "cannonL1", "cannonL2", "cannonL3"}) {
            assertTrue(TowerBalance.getInt(key, "damage", 0) > 0, key + " damage should be positive");
        }
    }

    @Test
    void allTowerCooldowns_positive() {
        for (String key : new String[]{"arrowL1", "arrowL2", "arrowL3", "cannonL1", "cannonL2", "cannonL3"}) {
            assertTrue(TowerBalance.getInt(key, "cooldownMs", 0) > 0, key + " cooldown should be positive");
        }
    }
}
