package com.kingdom.game.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TowerSpec: tower catalog entry (type, display name, cost).
 */
class TowerSpecTest {

    @Test
    void constructor_setsAllFields() {
        TowerSpec spec = new TowerSpec(TowerType.ARROW, "箭塔", 50);
        assertEquals(TowerType.ARROW, spec.getType());
        assertEquals("箭塔", spec.getDisplayName());
        assertEquals(50, spec.getCost());
    }

    @Test
    void cannonSpec_costIs100() {
        TowerSpec spec = new TowerSpec(TowerType.CANNON, "炮塔", 100);
        assertEquals(100, spec.getCost());
    }

    @Test
    void barrackSpec_costIs75() {
        TowerSpec spec = new TowerSpec(TowerType.BARRACK, "兵营", 75);
        assertEquals(75, spec.getCost());
    }

    @Test
    void displayName_canBeAnyString() {
        TowerSpec spec = new TowerSpec(TowerType.ARROW_MASTER, "大师箭塔", 200);
        assertEquals("大师箭塔", spec.getDisplayName());
    }

    @Test
    void zeroCost_allowed() {
        TowerSpec spec = new TowerSpec(TowerType.ARROW, "免费塔", 0);
        assertEquals(0, spec.getCost());
    }

    @Test
    void negativeCost_allowedByDataClass() {
        TowerSpec spec = new TowerSpec(TowerType.ARROW, "负价塔", -10);
        assertEquals(-10, spec.getCost());
    }

    @Test
    void allTowerTypes_canCreateSpec() {
        for (TowerType type : TowerType.values()) {
            TowerSpec spec = new TowerSpec(type, type.name(), 100);
            assertEquals(type, spec.getType());
        }
    }
}
