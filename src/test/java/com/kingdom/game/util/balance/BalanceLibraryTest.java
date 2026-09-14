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

    /**
     * 契约防回归：内置兜底 {@link BalanceTable#defaults()} 必须与随包发布的
     * {@code config/balance.json} 保持一致（见 {@code BalanceTable.defaults()} 的注释）。
     * 二者一旦只改一边，本用例立刻失败——历史事故即为 initialGold 100 / 130 各自为政。
     */
    @Test
    void defaults_matchesShippedBalanceFile() {
        BalanceTable shipped = BalanceLibrary.readFromClasspath();
        BalanceTable builtin = BalanceTable.defaults();
        assertNotNull(shipped);
        assertEquals(shipped.getInitialGold(), builtin.getInitialGold(),
                "initialGold：balance.json 与 BalanceTable.defaults() 不一致");
        assertEquals(shipped.getInitialLives(), builtin.getInitialLives(),
                "initialLives：balance.json 与 BalanceTable.defaults() 不一致");
        assertEquals(shipped.getTotalWaves(), builtin.getTotalWaves(),
                "totalWaves：balance.json 与 BalanceTable.defaults() 不一致");
    }

    @Test
    void readFromClasspath_hasExpectedValues() {
        BalanceTable fromClasspath = BalanceLibrary.readFromClasspath();
        assertNotNull(fromClasspath);
        assertEquals(100, fromClasspath.getInitialGold());
        assertEquals(20, fromClasspath.getInitialLives());
        assertEquals(80, fromClasspath.getNormalHp());
        assertEquals(1500, fromClasspath.getBossHp());
    }

    @Test
    void write_nullTable_returnsFalse() {
        assertFalse(BalanceLibrary.write(null));
    }

    // 注：本测试类**不得**调用 BalanceLibrary.write(...)。
    // write() 直接落盘仓库源文件 src/main/resources/config/balance.json，而非临时目录；
    // 原有两个用例（write_validTable_returnsTrue / writeThenRead_preservesValues）
    // 把它写成 BalanceTable.defaults() 的内容，导致每跑一次 mvn test
    // 就把 initialGold 从 100 覆盖成 130，并顺带改写整个文件的换行符（CRLF→LF）。
    // write() 属编辑器侧的文件 I/O，不在单元测试范围内，故移除；
    // defaults() 与 balance.json 的一致性改由 defaults_matchesShippedBalanceFile() 守护。

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
