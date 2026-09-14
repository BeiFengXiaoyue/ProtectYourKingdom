package com.kingdom.game.model.ally;

import com.kingdom.game.model.Faction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for 3 soldier classes: HP, speed, attack, faction, level progression.
 */
class SoldierClassesTest {

    @Test
    void level1Soldier_hp70_speed60_attack10() {
        Soldier s = new Soldier(0, 0, 70, 60, 10, 800);
        assertEquals(70, s.getCurrentHp());
        assertEquals(60.0, s.getSpeed());
        assertEquals(10, s.getAttackDamage());
    }

    @Test
    void level1Soldier_factionAlly_initiallyAlive() {
        Soldier s = new Soldier(0, 0, 70, 60, 10, 800);
        assertEquals(Faction.ALLY, s.getFaction());
        assertTrue(s.isAlive());
    }

    @Test
    void level2EliteSoldier_hp100_speed65_attack14() {
        EliteSoldier s = new EliteSoldier(0, 0, 100, 65, 14, 700);
        assertEquals(100, s.getCurrentHp());
        assertEquals(65.0, s.getSpeed());
        assertEquals(14, s.getAttackDamage());
    }

    @Test
    void level2EliteSoldier_factionAlly() {
        EliteSoldier s = new EliteSoldier(0, 0, 100, 65, 14, 700);
        assertEquals(Faction.ALLY, s.getFaction());
    }

    @Test
    void level3RoyalSoldier_hp150_speed72_attack20() {
        RoyalSoldier s = new RoyalSoldier(0, 0, 150, 72, 20, 620);
        assertEquals(150, s.getCurrentHp());
        assertEquals(72.0, s.getSpeed());
        assertEquals(20, s.getAttackDamage());
    }

    @Test
    void level3RoyalSoldier_factionAlly() {
        RoyalSoldier s = new RoyalSoldier(0, 0, 150, 72, 20, 620);
        assertEquals(Faction.ALLY, s.getFaction());
    }

    @Test
    void soldierHp_increasesWithLevel() {
        Soldier l1 = new Soldier(0, 0, 70, 60, 10, 800);
        EliteSoldier l2 = new EliteSoldier(0, 0, 100, 65, 14, 700);
        RoyalSoldier l3 = new RoyalSoldier(0, 0, 150, 72, 20, 620);
        assertTrue(l2.getCurrentHp() > l1.getCurrentHp());
        assertTrue(l3.getCurrentHp() > l2.getCurrentHp());
    }

    @Test
    void soldierAttack_increasesWithLevel() {
        Soldier l1 = new Soldier(0, 0, 70, 60, 10, 800);
        EliteSoldier l2 = new EliteSoldier(0, 0, 100, 65, 14, 700);
        RoyalSoldier l3 = new RoyalSoldier(0, 0, 150, 72, 20, 620);
        assertTrue(l2.getAttackDamage() > l1.getAttackDamage());
        assertTrue(l3.getAttackDamage() > l2.getAttackDamage());
    }

    @Test
    void soldierSpeed_increasesWithLevel() {
        Soldier l1 = new Soldier(0, 0, 70, 60, 10, 800);
        EliteSoldier l2 = new EliteSoldier(0, 0, 100, 65, 14, 700);
        RoyalSoldier l3 = new RoyalSoldier(0, 0, 150, 72, 20, 620);
        assertTrue(l2.getSpeed() > l1.getSpeed());
        assertTrue(l3.getSpeed() > l2.getSpeed());
    }

    @Test
    void allSoldiers_initiallyAlive() {
        assertTrue(new Soldier(0, 0, 70, 60, 10, 800).isAlive());
        assertTrue(new EliteSoldier(0, 0, 100, 65, 14, 700).isAlive());
        assertTrue(new RoyalSoldier(0, 0, 150, 72, 20, 620).isAlive());
    }

    @Test
    void soldierTakesDamage_hpDecreases() {
        Soldier s = new Soldier(0, 0, 70, 60, 10, 800);
        s.takeDamage(20);
        assertEquals(50, s.getCurrentHp());
    }

    @Test
    void soldierAttacksEnemy_enemyTakesDamage() {
        Soldier s = new Soldier(0, 0, 70, 60, 10, 800);
        com.kingdom.game.model.enemy.NormalEnemy enemy =
                new com.kingdom.game.model.enemy.NormalEnemy(50, 0, 80, 25, 15, 5, 1000);
        s.tryAttack(enemy);
        assertEquals(70, enemy.getCurrentHp());
    }
}
