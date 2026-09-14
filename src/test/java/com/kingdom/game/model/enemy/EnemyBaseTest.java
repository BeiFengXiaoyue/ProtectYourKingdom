package com.kingdom.game.model.enemy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Enemy base class: path movement, reaching end, taking damage, death.
 * Uses NormalEnemy as concrete implementation.
 */
class EnemyBaseTest {

    private NormalEnemy enemy;
    private static final double[] PATH_X = {100, 100, 200};
    private static final double[] PATH_Y = {0, 100, 100};

    @BeforeEach
    void setUp() {
        enemy = new NormalEnemy(0, 0, 100, 25, 15, 10, 1000);
        enemy.setPath(PATH_X.clone(), PATH_Y.clone());
    }

    @Test
    void initialHp_is100() {
        assertEquals(100, enemy.getCurrentHp());
        assertEquals(100, enemy.getMaxHp());
    }

    @Test
    void initialSpeed_is25() {
        assertEquals(25.0, enemy.getSpeed());
    }

    @Test
    void initiallyAlive() {
        assertTrue(enemy.isAlive());
    }

    @Test
    void initiallyNotReachedEnd() {
        assertFalse(enemy.hasReachedEnd());
    }

    @Test
    void initialPosition_isPathStart() {
        assertEquals(0.0, enemy.getX());
        assertEquals(0.0, enemy.getY());
    }

    @Test
    void move_changesPosition() {
        double oldX = enemy.getX();
        enemy.move();
        assertTrue(enemy.getX() > oldX);
    }

    @Test
    void move_movesTowardsFirstWaypoint() {
        enemy.move();
        assertTrue(enemy.getX() > 0);
        assertEquals(0.0, enemy.getY(), 0.001);
    }

    @Test
    void continuousMove_approachesWaypoint() {
        for (int i = 0; i < 200; i++) {
            enemy.move();
        }
        assertTrue(enemy.getX() > 50);
    }

    @Test
    void reachingEndOfPath_setsReachedEnd() {
        for (int i = 0; i < 1000; i++) {
            enemy.move();
            if (enemy.hasReachedEnd()) break;
        }
        assertTrue(enemy.hasReachedEnd());
    }

    @Test
    void takeDamage_reducesHp() {
        int actual = enemy.takeDamage(30);
        assertEquals(30, actual);
        assertEquals(70, enemy.getCurrentHp());
    }

    @Test
    void multipleTakeDamage_accumulates() {
        enemy.takeDamage(20);
        enemy.takeDamage(30);
        assertEquals(50, enemy.getCurrentHp());
    }

    @Test
    void hpDoesNotGoNegative() {
        enemy.takeDamage(999);
        assertEquals(0, enemy.getCurrentHp());
    }

    @Test
    void hpZero_dies() {
        enemy.takeDamage(100);
        assertEquals(0, enemy.getCurrentHp());
        assertFalse(enemy.isAlive());
    }

    @Test
    void deadEnemy_takingDamage_returnsZero() {
        enemy.takeDamage(100);
        int actual = enemy.takeDamage(50);
        assertEquals(0, actual);
        assertEquals(0, enemy.getCurrentHp());
    }

    @Test
    void moveDoesNotCheckAlive_byDesign() {
        enemy.takeDamage(100);
        assertFalse(enemy.isAlive());
        double x = enemy.getX();
        enemy.move();
        assertNotEquals(x, enemy.getX());
    }

    @Test
    void fastEnemy_fasterThanNormal() {
        FastEnemy fast = new FastEnemy(0, 0, 50, 50, 22, 8, 800);
        fast.setPath(PATH_X.clone(), PATH_Y.clone());
        enemy.move();
        fast.move();
        for (int i = 0; i < 10; i++) {
            enemy.move();
            fast.move();
        }
        assertTrue(fast.getX() > enemy.getX());
    }

    @Test
    void tankEnemy_higherHpThanNormal() {
        TankEnemy tank = new TankEnemy(0, 0, 240, 18, 35, 9, 1400);
        assertTrue(tank.getCurrentHp() > enemy.getCurrentHp());
    }

    @Test
    void bossEnemy_highestHp() {
        BossEnemy boss = new BossEnemy(0, 0, 1500, 22, 300, 30, 2000);
        assertTrue(boss.getCurrentHp() > 240);
        assertEquals(300, boss.getGoldReward());
    }

    @Test
    void emptyPath_reachesEndImmediately() {
        NormalEnemy e = new NormalEnemy(0, 0, 80, 25, 15, 5, 1000);
        e.setPath(new double[]{}, new double[]{});
        e.move();
        assertTrue(e.hasReachedEnd());
    }

    @Test
    void singleWaypoint_atStart_reachesEndAfterTwoMoves() {
        NormalEnemy e = new NormalEnemy(0, 0, 80, 25, 15, 5, 1000);
        e.setPath(new double[]{0}, new double[]{0});
        e.move();
        e.move();
        assertTrue(e.hasReachedEnd());
    }
}
