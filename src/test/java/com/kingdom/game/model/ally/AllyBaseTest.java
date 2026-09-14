package com.kingdom.game.model.ally;

import com.kingdom.game.model.Faction;
import com.kingdom.game.model.enemy.NormalEnemy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Ally base class: faction, target locking, collision, attack.
 * Uses Soldier as concrete implementation.
 */
class AllyBaseTest {

    private Soldier soldier;
    private NormalEnemy enemy;

    @BeforeEach
    void setUp() {
        soldier = new Soldier(100, 100, 50, 40, 4, 1100);
        enemy = new NormalEnemy(150, 100, 80, 25, 15, 5, 1000);
        enemy.setPath(new double[]{200, 300}, new double[]{100, 100});
    }

    @Test
    void initialFaction_isAlly() {
        assertEquals(Faction.ALLY, soldier.getFaction());
    }

    @Test
    void engage_noTarget_acquiresTarget() {
        assertNull(soldier.getTarget());
        soldier.engage(enemy);
        assertSame(enemy, soldier.getTarget());
    }

    @Test
    void engage_hasAliveTarget_doesNotSwitch() {
        NormalEnemy enemy2 = new NormalEnemy(200, 100, 80, 25, 15, 5, 1000);
        enemy2.setPath(new double[]{250, 300}, new double[]{100, 100});
        soldier.engage(enemy);
        soldier.engage(enemy2);
        assertSame(enemy, soldier.getTarget());
    }

    @Test
    void engage_targetDead_acquiresNewTarget() {
        NormalEnemy enemy2 = new NormalEnemy(200, 100, 80, 25, 15, 5, 1000);
        enemy2.setPath(new double[]{250, 300}, new double[]{100, 100});
        soldier.engage(enemy);
        enemy.takeDamage(100);
        soldier.engage(enemy2);
        assertSame(enemy2, soldier.getTarget());
    }

    @Test
    void engage_null_doesNothing() {
        soldier.engage(enemy);
        soldier.engage(null);
        assertSame(enemy, soldier.getTarget());
    }

    @Test
    void pollTarget_findsEnemyInVision() {
        soldier.pollTarget(List.of(enemy));
        assertSame(enemy, soldier.getTarget());
    }

    @Test
    void pollTarget_enemyFar_returnsNull() {
        NormalEnemy farEnemy = new NormalEnemy(500, 500, 80, 25, 15, 5, 1000);
        farEnemy.setPath(new double[]{550, 600}, new double[]{500, 500});
        soldier.pollTarget(List.of(farEnemy));
        assertNull(soldier.getTarget());
    }

    @Test
    void collision_withEnemy_returnsTrue() {
        boolean result = soldier.handleEntityCollision(enemy);
        assertTrue(result);
        assertSame(enemy, soldier.getTarget());
    }

    @Test
    void collision_withAlly_returnsFalse() {
        Soldier other = new Soldier(120, 100, 50, 40, 4, 1100);
        boolean result = soldier.handleEntityCollision(other);
        assertFalse(result);
    }

    @Test
    void tryAttack_doesNotThrow() {
        assertDoesNotThrow(() -> soldier.tryAttack(enemy));
    }

    @Test
    void tryAttack_dealsDamageToEnemy() {
        int hpBefore = enemy.getCurrentHp();
        soldier.tryAttack(enemy);
        assertEquals(hpBefore - 4, enemy.getCurrentHp());
    }

    @Test
    void tryAttack_setsCooldown() {
        soldier.tryAttack(enemy);
        int hpBefore = enemy.getCurrentHp();
        soldier.tryAttack(enemy);
        assertEquals(hpBefore, enemy.getCurrentHp());
    }

    @Test
    void initiallyAlive() {
        assertTrue(soldier.isAlive());
    }

    @Test
    void takeDamage_reducesHp() {
        soldier.takeDamage(20);
        assertEquals(30, soldier.getCurrentHp());
    }
}
