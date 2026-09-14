package com.kingdom.game.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TowerType enum: 9 tower types across 3 families x 3 levels.
 */
class TowerTypeTest {

    @Test
    void has9Values() {
        assertEquals(9, TowerType.values().length);
    }

    @Test
    void arrowFamily_has3Levels() {
        assertNotNull(TowerType.ARROW);
        assertNotNull(TowerType.ARROW_ELITE);
        assertNotNull(TowerType.ARROW_MASTER);
    }

    @Test
    void cannonFamily_has3Levels() {
        assertNotNull(TowerType.CANNON);
        assertNotNull(TowerType.CANNON_ELITE);
        assertNotNull(TowerType.CANNON_MASTER);
    }

    @Test
    void barrackFamily_has3Levels() {
        assertNotNull(TowerType.BARRACK);
        assertNotNull(TowerType.BARRACK_ELITE);
        assertNotNull(TowerType.BARRACK_MASTER);
    }

    @Test
    void valueOf_returnsCorrectEnum() {
        assertEquals(TowerType.ARROW, TowerType.valueOf("ARROW"));
        assertEquals(TowerType.CANNON, TowerType.valueOf("CANNON"));
        assertEquals(TowerType.BARRACK, TowerType.valueOf("BARRACK"));
    }

    @Test
    void valueOf_invalidName_throws() {
        assertThrows(IllegalArgumentException.class, () -> TowerType.valueOf("INVALID"));
    }

    @Test
    void ordinal_order_isArrowCannonBarrack() {
        assertEquals(0, TowerType.ARROW.ordinal());
        assertEquals(1, TowerType.ARROW_ELITE.ordinal());
        assertEquals(2, TowerType.ARROW_MASTER.ordinal());
        assertEquals(3, TowerType.CANNON.ordinal());
        assertEquals(6, TowerType.BARRACK.ordinal());
    }
}
