package com.kingdom.game.model.enemy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for 4 enemy classes: HP, speed, gold reward, damage reduction, faction.
 * Values from config/balance.json (BalanceTable).
 */
class EnemyClassesTest {

    // ================= Normal Enemy =================

    @Test
    void normalEnemy_hp80_speed25_gold15() {
        NormalEnemy e = new NormalEnemy(0, 0, 80, 25, 15);
        assertEquals(80, e.getCurrentHp());
        assertEquals(25.0, e.getSpeed());
        assertEquals(15, e.getGoldReward());
    }

    @Test
    void normalEnemy_initiallyAlive_notReachedEnd() {
        NormalEnemy e = new NormalEnemy(0, 0, 80, 25, 15);
        assertTrue(e.isAlive());
        assertFalse(e.hasReachedEnd());
    }

    // ================= Fast Enemy =================

    @Test
    void fastEnemy_hp50_speed50_gold22() {
        FastEnemy e = new FastEnemy(0, 0, 50, 50, 22);
        assertEquals(50, e.getCurrentHp());
        assertEquals(50.0, e.getSpeed());
        assertEquals(22, e.getGoldReward());
    }

    @Test
    void fastEnemy_fasterThanNormal() {
        FastEnemy fast = new FastEnemy(0, 0, 50, 50, 22);
        NormalEnemy normal = new NormalEnemy(0, 0, 80, 25, 15);
        assertTrue(fast.getSpeed() > normal.getSpeed());
    }

    @Test
    void fastEnemy_lowerHpThanNormal() {
        FastEnemy fast = new FastEnemy(0, 0, 50, 50, 22);
        NormalEnemy normal = new NormalEnemy(0, 0, 80, 25, 15);
        assertTrue(fast.getCurrentHp() < normal.getCurrentHp());
    }

    // ================= Tank Enemy =================

    @Test
    void tankEnemy_hp240_speed18_gold35() {
        TankEnemy e = new TankEnemy(0, 0, 240, 18, 35);
        assertEquals(240, e.getCurrentHp());
        assertEquals(18.0, e.getSpeed());
        assertEquals(35, e.getGoldReward());
    }

    @Test
    void tankEnemy_hasPhysicalReduction() {
        TankEnemy tank = new TankEnemy(0, 0, 240, 18, 35);
        int actual = tank.takeDamage(10);
        assertEquals(7, actual);
        assertEquals(233, tank.getCurrentHp());
    }

    @Test
    void tankEnemy_armorPiercing_ignoresReduction() {
        TankEnemy tank = new TankEnemy(0, 0, 240, 18, 35);
        int actual = tank.takeDamage(10, true);
        assertEquals(10, actual);
        assertEquals(230, tank.getCurrentHp());
    }

    @Test
    void tankEnemy_highestHp_amongRegularEnemies() {
        TankEnemy tank = new TankEnemy(0, 0, 240, 18, 35);
        NormalEnemy normal = new NormalEnemy(0, 0, 80, 25, 15);
        FastEnemy fast = new FastEnemy(0, 0, 50, 50, 22);
        assertTrue(tank.getCurrentHp() > normal.getCurrentHp());
        assertTrue(tank.getCurrentHp() > fast.getCurrentHp());
    }

    @Test
    void tankEnemy_slowest_amongRegularEnemies() {
        TankEnemy tank = new TankEnemy(0, 0, 240, 18, 35);
        NormalEnemy normal = new NormalEnemy(0, 0, 80, 25, 15);
        FastEnemy fast = new FastEnemy(0, 0, 50, 50, 22);
        assertTrue(tank.getSpeed() < normal.getSpeed());
        assertTrue(tank.getSpeed() < fast.getSpeed());
    }

    // ================= Boss Enemy =================

    @Test
    void bossEnemy_hp1500_speed22_gold300() {
        BossEnemy e = new BossEnemy(0, 0, 1500, 22, 300);
        assertEquals(1500, e.getCurrentHp());
        assertEquals(22.0, e.getSpeed());
        assertEquals(300, e.getGoldReward());
    }

    @Test
    void bossHp_muchHigherThanOtherEnemies() {
        BossEnemy boss = new BossEnemy(0, 0, 1500, 22, 300);
        TankEnemy tank = new TankEnemy(0, 0, 240, 18, 35);
        assertTrue(boss.getCurrentHp() > tank.getCurrentHp() * 5);
    }

    @Test
    void bossGold_highestReward() {
        BossEnemy boss = new BossEnemy(0, 0, 1500, 22, 300);
        TankEnemy tank = new TankEnemy(0, 0, 240, 18, 35);
        FastEnemy fast = new FastEnemy(0, 0, 50, 50, 22);
        NormalEnemy normal = new NormalEnemy(0, 0, 80, 25, 15);
        assertTrue(boss.getGoldReward() > tank.getGoldReward());
        assertTrue(boss.getGoldReward() > fast.getGoldReward());
        assertTrue(boss.getGoldReward() > normal.getGoldReward());
    }

    @Test
    void bossStillAlive_afterTakingDamage() {
        BossEnemy boss = new BossEnemy(0, 0, 1500, 22, 300);
        boss.takeDamage(500);
        assertTrue(boss.isAlive());
        assertEquals(1000, boss.getCurrentHp());
    }

    @Test
    void bossRequiresLargeDamage_toKill() {
        BossEnemy boss = new BossEnemy(0, 0, 1500, 22, 300);
        for (int i = 0; i < 150; i++) {
            boss.takeDamage(10);
        }
        assertFalse(boss.isAlive());
        assertEquals(0, boss.getCurrentHp());
    }

    // ================= Common Properties =================

    @Test
    void allEnemies_factionIsEnemy() {
        assertEquals(com.kingdom.game.model.Faction.ENEMY, new NormalEnemy(0, 0, 80, 25, 15).getFaction());
        assertEquals(com.kingdom.game.model.Faction.ENEMY, new FastEnemy(0, 0, 50, 50, 22).getFaction());
        assertEquals(com.kingdom.game.model.Faction.ENEMY, new TankEnemy(0, 0, 240, 18, 35).getFaction());
        assertEquals(com.kingdom.game.model.Faction.ENEMY, new BossEnemy(0, 0, 1500, 22, 300).getFaction());
    }

    @Test
    void allEnemies_initiallyAlive() {
        assertTrue(new NormalEnemy(0, 0, 80, 25, 15).isAlive());
        assertTrue(new FastEnemy(0, 0, 50, 50, 22).isAlive());
        assertTrue(new TankEnemy(0, 0, 240, 18, 35).isAlive());
        assertTrue(new BossEnemy(0, 0, 1500, 22, 300).isAlive());
    }

    @Test
    void allEnemies_initiallyNotReachedEnd() {
        assertFalse(new NormalEnemy(0, 0, 80, 25, 15).hasReachedEnd());
        assertFalse(new FastEnemy(0, 0, 50, 50, 22).hasReachedEnd());
        assertFalse(new TankEnemy(0, 0, 240, 18, 35).hasReachedEnd());
        assertFalse(new BossEnemy(0, 0, 1500, 22, 300).hasReachedEnd());
    }
}
