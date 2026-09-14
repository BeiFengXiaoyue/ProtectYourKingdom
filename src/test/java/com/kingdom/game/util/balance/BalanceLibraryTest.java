package com.kingdom.game.util.balance;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BalanceLibrary: balance.json file location, read/write, classpath fallback.
 */
class BalanceLibraryTest {

    @Test
    void findRepoResourcesDir_returnsNonNull() {
        File dir = BalanceLibrary.findRepoResourcesDir();
        assertNotNull(dir);
        assertTrue(dir.isDirectory());
        assertTrue(dir.getPath().endsWith("resources"));
    }

    @Test
    void balanceFile_returnsNonNull() {
        File f = BalanceLibrary.balanceFile();
        assertNotNull(f);
        assertTrue(f.getPath().endsWith("balance.json"));
    }

    @Test
    void balanceFile_exists() {
        File f = BalanceLibrary.balanceFile();
        assertNotNull(f);
        assertTrue(f.isFile(), "balance.json should exist in src/main/resources/config/");
    }

    @Test
    void read_returnsNonNull() {
        BalanceTable table = BalanceLibrary.read();
        assertNotNull(table);
    }

    @Test
    void read_initialGold_isPositive() {
        BalanceTable table = BalanceLibrary.read();
        assertNotNull(table);
        assertTrue(table.getInitialGold() > 0);
    }

    @Test
    void read_hasCorrectInitialLives() {
        BalanceTable table = BalanceLibrary.read();
        assertNotNull(table);
        assertEquals(20, table.getInitialLives());
    }

    @Test
    void readFromClasspath_returnsNonNull() {
        BalanceTable table = BalanceLibrary.readFromClasspath();
        assertNotNull(table);
    }

    @Test
    void readFromClasspath_hasExpectedValues() {
        BalanceTable fromClasspath = BalanceLibrary.readFromClasspath();
        assertNotNull(fromClasspath);
        assertEquals(130, fromClasspath.getInitialGold());
        assertEquals(20, fromClasspath.getInitialLives());
        assertEquals(80, fromClasspath.getNormalHp());
        assertEquals(1500, fromClasspath.getBossHp());
    }

    @Test
    void write_nullTable_returnsFalse() {
        assertFalse(BalanceLibrary.write(null));
    }

    @Test
    void write_validTable_returnsTrue() {
        BalanceTable table = BalanceTable.defaults();
        assertTrue(BalanceLibrary.write(table));
    }

    @Test
    void writeThenRead_preservesValues() {
        BalanceTable original = BalanceTable.defaults();
        assertTrue(BalanceLibrary.write(original));
        BalanceTable readBack = BalanceLibrary.read();
        assertNotNull(readBack);
        assertEquals(original.getInitialGold(), readBack.getInitialGold());
        assertEquals(original.getNormalHp(), readBack.getNormalHp());
        assertEquals(original.getBossHp(), readBack.getBossHp());
    }

    @Test
    void balanceFile_inConfigSubdirectory() {
        File f = BalanceLibrary.balanceFile();
        assertNotNull(f);
        assertTrue(f.getParentFile().getName().equals("config"));
    }

    @Test
    void BALANCE_SUBDIR_isConfig() {
        assertEquals("config", BalanceLibrary.BALANCE_SUBDIR);
    }

    @Test
    void BALANCE_FILE_isBalanceJson() {
        assertEquals("balance.json", BalanceLibrary.BALANCE_FILE);
    }
}
