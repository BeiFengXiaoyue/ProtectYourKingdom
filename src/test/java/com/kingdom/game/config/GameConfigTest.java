package com.kingdom.game.config;

import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.TowerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameConfig: initial values from balance.json, enemy stats, tower specs, setters.
 */
class GameConfigTest {

    private GameConfig config;

    @BeforeEach
    void setUp() {
        config = new GameConfig();
    }

    // ================= Initial Resources =================

    @Test
    void initialGold_is130() {
        assertEquals(130, config.getInitialGold());
    }

    @Test
    void initialLives_is20() {
        assertEquals(20, config.getInitialLives());
    }

    @Test
    void totalWaves_isPositive() {
        assertTrue(config.getTotalWaves() > 0);
    }

    @Test
    void viewSize_defaultIsPositive() {
        assertTrue(config.getViewWidth() > 0);
        assertTrue(config.getViewHeight() > 0);
    }

    // ================= Normal Enemy =================

    @Test
    void normalEnemyHp_is80() {
        assertEquals(80, config.getNormalHp());
    }

    @Test
    void normalEnemySpeed_is25() {
        assertEquals(25.0, config.getNormalSpeed());
    }

    @Test
    void normalEnemyGoldReward_is15() {
        assertEquals(15, config.getNormalGoldReward());
    }

    @Test
    void normalEnemyAttackDamage_isPositive() {
        assertTrue(config.getNormalAttackDamage() > 0);
    }

    @Test
    void normalEnemyAttackCooldown_isPositive() {
        assertTrue(config.getNormalAttackCooldownMs() > 0);
    }

    // ================= Fast Enemy =================

    @Test
    void fastEnemyHp_is50() {
        assertEquals(50, config.getFastHp());
    }

    @Test
    void fastEnemySpeed_is50() {
        assertEquals(50.0, config.getFastSpeed());
    }

    @Test
    void fastEnemyGoldReward_is22() {
        assertEquals(22, config.getFastGoldReward());
    }

    // ================= Tank Enemy =================

    @Test
    void tankEnemyHp_is240() {
        assertEquals(240, config.getTankHp());
    }

    @Test
    void tankEnemySpeed_is18() {
        assertEquals(18.0, config.getTankSpeed());
    }

    @Test
    void tankEnemyGoldReward_is35() {
        assertEquals(35, config.getTankGoldReward());
    }

    // ================= Boss Enemy =================

    @Test
    void bossEnemyHp_is1500() {
        assertEquals(1500, config.getBossHp());
    }

    @Test
    void bossEnemySpeed_is22() {
        assertEquals(22.0, config.getBossSpeed());
    }

    @Test
    void bossEnemyGoldReward_is300() {
        assertEquals(300, config.getBossGoldReward());
    }

    // ================= Setters (fluent API) =================

    @Test
    void setInitialGold_updatesValue() {
        config.setInitialGold(200);
        assertEquals(200, config.getInitialGold());
    }

    @Test
    void setInitialLives_updatesValue() {
        config.setInitialLives(10);
        assertEquals(10, config.getInitialLives());
    }

    @Test
    void setTotalWaves_updatesValue() {
        config.setTotalWaves(15);
        assertEquals(15, config.getTotalWaves());
    }

    @Test
    void setViewSize_updatesDimensions() {
        config.setViewSize(1200, 800);
        assertEquals(1200.0, config.getViewWidth());
        assertEquals(800.0, config.getViewHeight());
    }

    @Test
    void setNormalStats_updatesAllThree() {
        config.setNormalStats(100, 30, 20);
        assertEquals(100, config.getNormalHp());
        assertEquals(30.0, config.getNormalSpeed());
        assertEquals(20, config.getNormalGoldReward());
    }

    @Test
    void setFastStats_updatesAllThree() {
        config.setFastStats(60, 55, 25);
        assertEquals(60, config.getFastHp());
        assertEquals(55.0, config.getFastSpeed());
        assertEquals(25, config.getFastGoldReward());
    }

    @Test
    void setTankStats_updatesAllThree() {
        config.setTankStats(300, 15, 40);
        assertEquals(300, config.getTankHp());
        assertEquals(15.0, config.getTankSpeed());
        assertEquals(40, config.getTankGoldReward());
    }

    @Test
    void setBossStats_updatesAllThree() {
        config.setBossStats(2000, 20, 400);
        assertEquals(2000, config.getBossHp());
        assertEquals(20.0, config.getBossSpeed());
        assertEquals(400, config.getBossGoldReward());
    }

    // ================= Tower Specs =================

    @Test
    void addTowerSpec_addsToCatalog() {
        TowerSpec spec = new TowerSpec(TowerType.ARROW, "箭塔", 50);
        config.addTowerSpec(spec);
        assertTrue(config.getTowerSpecs().contains(spec));
    }

    @Test
    void addTowerSpec_null_doesNotThrow() {
        assertDoesNotThrow(() -> config.addTowerSpec(null));
    }

    @Test
    void getTowerSpec_existingType_returnsSpec() {
        TowerSpec spec = new TowerSpec(TowerType.CANNON, "炮塔", 100);
        config.addTowerSpec(spec);
        TowerSpec found = config.getTowerSpec(TowerType.CANNON);
        assertNotNull(found);
        assertEquals("炮塔", found.getDisplayName());
    }

    @Test
    void getTowerSpec_unknownType_returnsNull() {
        assertNull(config.getTowerSpec(TowerType.ARROW_MASTER));
    }

    @Test
    void getTowerSpecs_returnsUnmodifiableList() {
        TowerSpec spec = new TowerSpec(TowerType.ARROW, "箭塔", 50);
        config.addTowerSpec(spec);
        assertThrows(UnsupportedOperationException.class,
                () -> config.getTowerSpecs().add(new TowerSpec(TowerType.BARRACK, "兵营", 75)));
    }

    // ================= Wave Config =================

    @Test
    void waveIntermissionMs_isNonNegative() {
        assertTrue(config.getWaveIntermissionMs() >= 0);
    }

    @Test
    void earlyStartRewardCap_isNonNegative() {
        assertTrue(config.getEarlyStartRewardCap() >= 0);
    }

    @Test
    void setWaveIntermissionMs_updatesValue() {
        config.setWaveIntermissionMs(5000);
        assertEquals(5000, config.getWaveIntermissionMs());
    }

    @Test
    void setEarlyStartRewardCap_updatesValue() {
        config.setEarlyStartRewardCap(100);
        assertEquals(100, config.getEarlyStartRewardCap());
    }

    // ================= Path =================

    @Test
    void getPathX_notNull() {
        assertNotNull(config.getPathX());
    }

    @Test
    void getPathY_notNull() {
        assertNotNull(config.getPathY());
    }

    @Test
    void getPathStartX_returnsFirstPoint() {
        assertEquals(config.getPathX()[0], config.getPathStartX());
    }

    @Test
    void getPathStartY_returnsFirstPoint() {
        assertEquals(config.getPathY()[0], config.getPathStartY());
    }

    // ================= Colors =================

    @Test
    void colorBackground_notNull() {
        assertNotNull(config.getColorBackground());
    }

    @Test
    void colorPath_notNull() {
        assertNotNull(config.getColorPath());
    }

    @Test
    void setColors_updatesBoth() {
        config.setColors("#000000", "#ffffff");
        assertEquals("#000000", config.getColorBackground());
        assertEquals("#ffffff", config.getColorPath());
    }
}
