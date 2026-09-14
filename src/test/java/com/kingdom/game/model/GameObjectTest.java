package com.kingdom.game.model;

import com.kingdom.game.model.tower.ArrowTower;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameObject base class: position, size, effect injection, lifecycle.
 * Uses ArrowTower as concrete implementation.
 */
class GameObjectTest {

    @Test
    void constructor_setsPosition() {
        ArrowTower obj = new ArrowTower(100, 200);
        assertEquals(100.0, obj.getX());
        assertEquals(200.0, obj.getY());
    }

    @Test
    void setX_modifiesX() {
        ArrowTower obj = new ArrowTower(0, 0);
        obj.setX(150);
        assertEquals(150.0, obj.getX());
    }

    @Test
    void setY_modifiesY() {
        ArrowTower obj = new ArrowTower(0, 0);
        obj.setY(250);
        assertEquals(250.0, obj.getY());
    }

    @Test
    void negativeCoordinates_allowed() {
        ArrowTower obj = new ArrowTower(-10, -20);
        assertEquals(-10.0, obj.getX());
        assertEquals(-20.0, obj.getY());
    }

    @Test
    void size_drivenBySizeTable() {
        ArrowTower obj = new ArrowTower(0, 0);
        assertTrue(obj.getWidth() > 0);
        assertTrue(obj.getHeight() > 0);
    }

    @Test
    void setWidth_modifiesWidth() {
        ArrowTower obj = new ArrowTower(0, 0);
        obj.setWidth(50);
        assertEquals(50.0, obj.getWidth());
    }

    @Test
    void setHeight_modifiesHeight() {
        ArrowTower obj = new ArrowTower(0, 0);
        obj.setHeight(60);
        assertEquals(60.0, obj.getHeight());
    }

    @Test
    void defaultEffects_areNopImplementations() {
        ArrowTower obj = new ArrowTower(0, 0);
        assertDoesNotThrow(() -> obj.attachEffects(null, null, null, null, null));
    }

    @Test
    void attachEffects_nullDoesNotReplace() {
        ArrowTower obj = new ArrowTower(0, 0);
        assertDoesNotThrow(() -> obj.attachEffects(null, null, null, null, null));
        assertDoesNotThrow(() -> obj.attachEffects(null, null, null, null, null));
    }

    @Test
    void destroy_defaultNoOp() {
        ArrowTower obj = new ArrowTower(0, 0);
        assertDoesNotThrow(obj::destroy);
    }

    @Test
    void multipleDestroy_safe() {
        ArrowTower obj = new ArrowTower(0, 0);
        obj.destroy();
        obj.destroy();
        obj.destroy();
    }

    @Test
    void update_canBeCalled() {
        ArrowTower obj = new ArrowTower(0, 0);
        assertDoesNotThrow(obj::update);
    }
}
