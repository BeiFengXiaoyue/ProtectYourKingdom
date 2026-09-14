package com.kingdom.game.util.balance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BalanceTable: 29 balance.json fields + value sanity checks.
 */
class BalanceTableTest {

    private BalanceTable table() {
        return BalanceTable.runtime();
    }

    // ================= Initial Resources =================

    @Test
    void initialGold_is100() {
        assertEquals(100, table().getInitialGold());
    }

    @Test
    void initialLives_is20() {
        assertEquals(20, table().getInitialLives());
    }

    @Test
    void totalWaves_isPositive() {
        assertTrue(table().getTotalWaves() > 0);
    }

    // ================= Normal Enemy =================

    @Test
    void normalEnemyHp_is80() {
        assertEquals(80, table().getNormalHp());
    }

    @Test
    void normalEnemySpeed_is25() {
        assertEquals(25.0, table().getNormalSpeed());
    }

    @Test
    void normalEnemyGoldReward_is15() {
        assertEquals(15, table().getNormalGoldReward());
    }

    @Test
    void normalEnemyAttackDamage_isPositive() {
        assertTrue(table().getNormalAttackDamage() > 0);
    }

    @Test
    void normalEnemyAttackCooldown_isPositive() {
        assertTrue(table().getNormalAttackCooldownMs() > 0);
    }

    // ================= Fast Enemy =================

    @Test
    void fastEnemyHp_is50() {
        assertEquals(50, table().getFastHp());
    }

    @Test
    void fastEnemySpeed_is50() {
        assertEquals(50.0, table().getFastSpeed());
    }

    @Test
    void fastEnemyGoldReward_is22() {
        assertEquals(22, table().getFastGoldReward());
    }

    @Test
    void fastEnemyAttackDamage_isPositive() {
        assertTrue(table().getFastAttackDamage() > 0);
    }

    @Test
    void fastEnemyAttackCooldown_isPositive() {
        assertTrue(table().getFastAttackCooldownMs() > 0);
    }

    // ================= Tank Enemy =================

    @Test
    void tankEnemyHp_is240() {
        assertEquals(240, table().getTankHp());
    }

    @Test
    void tankEnemySpeed_is18() {
        assertEquals(18.0, table().getTankSpeed());
    }

    @Test
    void tankEnemyGoldReward_is35() {
        assertEquals(35, table().getTankGoldReward());
    }

    @Test
    void tankPhysicalReduction_is30Percent() {
        assertEquals(0.3, table().getTankPhysicalReduction(), 0.001);
    }

    @Test
    void tankEnemyAttackDamage_isPositive() {
        assertTrue(table().getTankAttackDamage() > 0);
    }

    @Test
    void tankEnemyAttackCooldown_isPositive() {
        assertTrue(table().getTankAttackCooldownMs() > 0);
    }

    // ================= Boss Enemy =================

    @Test
    void bossEnemyHp_is1500() {
        assertEquals(1500, table().getBossHp());
    }

    @Test
    void bossEnemySpeed_is22() {
        assertEquals(22.0, table().getBossSpeed());
    }

    @Test
    void bossEnemyGoldReward_is300() {
        assertEquals(300, table().getBossGoldReward());
    }

    @Test
    void bossEnemyAttackDamage_isPositive() {
        assertTrue(table().getBossAttackDamage() > 0);
    }

    @Test
    void bossEnemyAttackCooldown_isPositive() {
        assertTrue(table().getBossAttackCooldownMs() > 0);
    }

    // ================= Cannon Splash =================

    @Test
    void cannonSplashRadius_isPositive() {
        assertTrue(table().getCannonSplashRadius() > 0);
    }

    @Test
    void eliteCannonSplashRadius_isPositive() {
        assertTrue(table().getEliteCannonSplashRadius() > 0);
    }

    @Test
    void masterCannonSplashRadius_isPositive() {
        assertTrue(table().getMasterCannonSplashRadius() > 0);
    }

    @Test
    void cannonSplashRadius_increasesWithLevel() {
        assertTrue(table().getEliteCannonSplashRadius() >= table().getCannonSplashRadius());
        assertTrue(table().getMasterCannonSplashRadius() >= table().getEliteCannonSplashRadius());
    }

    // ================= Boss Enrage =================

    @Test
    void bossStompIntervalMs_isPositive() {
        assertTrue(table().getBossStompIntervalMs() > 0);
    }

    @Test
    void bossEnrageAttackCooldownCut_between0And1() {
        double cut = table().getBossEnrageAttackCooldownCut();
        assertTrue(cut >= 0 && cut < 1);
    }

    // ================= Value Sanity =================

    @Test
    void fastEnemy_fasterThanNormal() {
        assertTrue(table().getFastSpeed() > table().getNormalSpeed());
    }

    @Test
    void tankEnemy_slowerThanNormal() {
        assertTrue(table().getTankSpeed() < table().getNormalSpeed());
    }

    @Test
    void tankEnemy_higherHpThanNormal() {
        assertTrue(table().getTankHp() > table().getNormalHp());
    }

    @Test
    void bossEnemy_highestHp() {
        assertTrue(table().getBossHp() > table().getTankHp());
        assertTrue(table().getBossHp() > table().getNormalHp());
        assertTrue(table().getBossHp() > table().getFastHp());
    }

    @Test
    void bossEnemy_highestGoldReward() {
        assertTrue(table().getBossGoldReward() > table().getTankGoldReward());
        assertTrue(table().getBossGoldReward() > table().getNormalGoldReward());
        assertTrue(table().getBossGoldReward() > table().getFastGoldReward());
    }

    @Test
    void tankPhysicalReduction_between0And1() {
        double r = table().getTankPhysicalReduction();
        assertTrue(r >= 0 && r < 1);
    }

    @Test
    void allEnemySpeeds_positive() {
        assertTrue(table().getNormalSpeed() > 0);
        assertTrue(table().getFastSpeed() > 0);
        assertTrue(table().getTankSpeed() > 0);
        assertTrue(table().getBossSpeed() > 0);
    }

    @Test
    void allEnemyHps_positive() {
        assertTrue(table().getNormalHp() > 0);
        assertTrue(table().getFastHp() > 0);
        assertTrue(table().getTankHp() > 0);
        assertTrue(table().getBossHp() > 0);
    }

    @Test
    void allGoldRewards_positive() {
        assertTrue(table().getNormalGoldReward() > 0);
        assertTrue(table().getFastGoldReward() > 0);
        assertTrue(table().getTankGoldReward() > 0);
        assertTrue(table().getBossGoldReward() > 0);
    }

    @Test
    void initialGold_enoughForOneArrowTower() {
        assertTrue(table().getInitialGold() >= 50);
    }

    @Test
    void initialLives_reasonable() {
        assertTrue(table().getInitialLives() >= 1 && table().getInitialLives() <= 100);
    }
}
