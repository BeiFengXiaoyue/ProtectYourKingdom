package com.kingdom.game.model;

import com.kingdom.game.model.ally.EliteSoldier;
import com.kingdom.game.model.ally.Soldier;
import com.kingdom.game.model.enemy.NormalEnemy;
import com.kingdom.game.model.projectile.Arrow;
import com.kingdom.game.model.tower.ArrowTower;
import com.kingdom.game.model.tower.Barrack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the rendering decoupling (model 零 JavaFX).
 *
 * <p>Before the refactor, entity {@code render()} took a JavaFX {@code GraphicsContext} and could not
 * be exercised without a live JavaFX canvas. Now model draws through {@link IRenderTarget}, so the same
 * drawing logic is assertable head-lessly with a recording fake — this class is the payoff of that change.
 *
 * <p>Covers: HP bar geometry (half HP → half width, backdrop before fill), sprite-missing color-block
 * fallback, sprite hit path, sprite anchor (entity center → top-left), and Arrow's save/translate/rotate
 * coordinate-stack usage.
 */
class RenderTargetTest {

    /** 记录型 fake 绘制目标：把每次调用记进有序日志，贴图可用性可切换 */
    private static final class RecordingTarget implements IRenderTarget {

        final List<String> ops = new ArrayList<>();
        /** true 时 drawAsset 返回 true（模拟素材已提供），false 时返回 false（触发色块回退） */
        boolean assetAvailable = false;
        String fill = "#000000";
        String stroke = "#000000";
        double lineWidth = 1;

        boolean has(String op) { return ops.contains(op); }

        long count(String op) { return ops.stream().filter(op::equals).count(); }

        /** 最后一条以 prefix 开头的记录；无则 null */
        String last(String prefix) {
            for (int i = ops.size() - 1; i >= 0; i--) {
                if (ops.get(i).startsWith(prefix)) return ops.get(i);
            }
            return null;
        }

        /** 在指定填充色下发生的填充类调用（按绘制顺序） */
        List<String> fillsWith(String color) {
            List<String> out = new ArrayList<>();
            String cur = null;
            for (String op : ops) {
                if (op.startsWith("setFill:")) {
                    cur = op.substring("setFill:".length());
                } else if (color.equals(cur) && op.startsWith("fill")) {
                    out.add(op);
                }
            }
            return out;
        }

        @Override public void setFill(String color) { fill = color; ops.add("setFill:" + color); }

        @Override public void setStroke(String color) { stroke = color; ops.add("setStroke:" + color); }

        @Override public void setLineWidth(double width) { lineWidth = width; ops.add("setLineWidth:" + width); }

        @Override public void fillRect(double x, double y, double w, double h) {
            ops.add("fillRect:" + x + "," + y + "," + w + "," + h);
        }

        @Override public void fillOval(double x, double y, double w, double h) {
            ops.add("fillOval:" + x + "," + y + "," + w + "," + h);
        }

        @Override public void strokeRect(double x, double y, double w, double h) {
            ops.add("strokeRect:" + x + "," + y + "," + w + "," + h);
        }

        @Override public void strokeOval(double x, double y, double w, double h) {
            ops.add("strokeOval:" + x + "," + y + "," + w + "," + h);
        }

        @Override public void fillPolygon(double[] xs, double[] ys) {
            ops.add("fillPolygon:" + xs.length);
        }

        @Override public boolean drawAsset(AssetKey key, double x, double y, double w, double h) {
            ops.add("drawAsset:" + key + "@" + x + "," + y + "," + w + "," + h);
            return assetAvailable;
        }

        @Override public void save() { ops.add("save"); }

        @Override public void restore() { ops.add("restore"); }

        @Override public void translate(double dx, double dy) { ops.add("translate:" + dx + "," + dy); }

        @Override public void rotate(double degrees) { ops.add("rotate:" + degrees); }
    }

    private RecordingTarget rt;

    @BeforeEach
    void setUp() {
        rt = new RecordingTarget();
    }

    private static NormalEnemy enemyAt(double x, double y) {
        NormalEnemy e = new NormalEnemy(x, y, 100, 25, 15, 10, 1000);
        e.setPath(new double[]{x, x + 400}, new double[]{y, y});
        return e;
    }

    /** 从 "op:第一个参数" 形式的记录里取出逗号分隔的数字 */
    private static double[] numbersAfter(String op) {
        String tail = op.substring(op.indexOf(':') + 1);
        if (tail.contains("@")) tail = tail.substring(tail.indexOf('@') + 1);
        String[] parts = tail.split(",");
        double[] out = new double[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Double.parseDouble(parts[i]);
        return out;
    }

    // ===== 血条 =====

    @Test
    void hpBarWidth_halvesWhenHpHalved() {
        NormalEnemy enemy = enemyAt(0, 0);
        enemy.takeDamage(50);
        rt.assetAvailable = true;   // 有贴图，隔离出血条这一条绘制路径

        enemy.render(rt);

        List<String> bars = rt.fillsWith(GameColors.HP_BAR_FILL);
        assertEquals(1, bars.size(), "血量前景应恰好绘制一次");
        double[] p = numbersAfter(bars.get(0));
        assertEquals(enemy.getWidth() * 0.5, p[2], 1e-9, "半血时血条宽度应为一半");
    }

    @Test
    void hpBarBackdrop_isDrawnBeforeFill() {
        enemyAt(0, 0).render(rt);

        List<String> backdrop = rt.fillsWith(GameColors.HP_BAR_BG);
        List<String> fill = rt.fillsWith(GameColors.HP_BAR_FILL);
        assertEquals(1, backdrop.size(), "血条底槽应绘制一次");
        assertEquals(1, fill.size(), "血条前景应绘制一次");
        assertTrue(rt.ops.indexOf(backdrop.get(0)) < rt.ops.indexOf(fill.get(0)),
                "底槽必须先于前景绘制，否则前景会被底色盖住");
    }

    @Test
    void hpBar_usesBackdropAndFillColorsFromGameColors() {
        assertEquals("#000000", GameColors.HP_BAR_BG);
        assertEquals("#32cd32", GameColors.HP_BAR_FILL, "前景绿须等价改造前的 Color.LIMEGREEN");
    }

    // ===== 贴图缺失回退 =====

    @Test
    void withoutSprite_fallsBackToColorBlock() {
        NormalEnemy enemy = enemyAt(0, 0);
        rt.assetAvailable = false;

        enemy.render(rt);

        assertNotNull(rt.last("drawAsset:NORMAL_ENEMY"), "应尝试取一次贴图");
        assertFalse(rt.fillsWith("#ff0000").isEmpty(), "贴图缺失时应回退为红色圆身");
        assertTrue(rt.has("strokeOval"), "回退圆身应带描边");
    }

    @Test
    void withSprite_doesNotDrawColorBlockFallback() {
        NormalEnemy enemy = enemyAt(0, 0);
        rt.assetAvailable = true;

        enemy.render(rt);

        assertTrue(rt.fillsWith("#ff0000").isEmpty(), "有贴图时不应再画色块");
        assertFalse(rt.has("strokeOval"), "有贴图时不应画回退描边");
        assertEquals(1, rt.fillsWith(GameColors.HP_BAR_FILL).size(), "血条与贴图无关，仍应绘制");
    }

    @Test
    void spriteIsDrawnAtEntityTopLeftAnchor() {
        NormalEnemy enemy = enemyAt(100, 50);

        enemy.render(rt);

        String op = rt.last("drawAsset:");
        assertNotNull(op, "应请求绘制贴图");
        double[] p = numbersAfter(op);
        assertEquals(100 - enemy.getWidth() / 2, p[0], 1e-9, "贴图 x 应为实体中心 − 半宽");
        assertEquals(50 - enemy.getHeight() / 2, p[1], 1e-9, "贴图 y 应为实体中心 − 半高");
        assertEquals(enemy.getWidth(), p[2], 1e-9);
        assertEquals(enemy.getHeight(), p[3], 1e-9);
    }

    // ===== 投射物：画笔状态栈与旋转 =====

    @Test
    void arrowFallback_rotatesTowardTargetAndBalancesStateStack() {
        NormalEnemy target = new NormalEnemy(0, 60, 100, 0, 0, 1, 1000);   // 正下方 → 90°
        Arrow arrow = new Arrow(0, 0, target, 10, 300);
        rt.assetAvailable = false;

        arrow.render(rt);

        assertTrue(rt.has("save"), "应压入画笔状态");
        assertTrue(rt.has("restore"), "应弹出画笔状态");
        assertEquals(rt.count("save"), rt.count("restore"), "save/restore 必须成对");
        String rot = rt.last("rotate:");
        assertNotNull(rot);
        assertEquals(90.0, Double.parseDouble(rot.substring("rotate:".length())), 1e-6,
                "箭身应朝向目标方向");
        double[] t = numbersAfter(rt.last("translate:"));
        assertArrayEquals(new double[]{0, 0}, t, 1e-9, "应平移到实体中心后旋转");
    }

    // ===== 形状绘制覆盖面：多边形与描边 =====

    @Test
    void barrackFallback_drawsRoofPolygon() {
        Barrack barrack = new Barrack(0, 0);
        rt.assetAvailable = false;

        barrack.render(rt);

        assertEquals("fillPolygon:3", rt.last("fillPolygon:"), "屋顶应由 3 点多边形构成");
        assertFalse(rt.fillsWith(GameColors.TOWER_BASE_BARRACK).isEmpty(), "营房用兵营族底座色");
        assertTrue(rt.has("strokeRect"));
    }

    @Test
    void arrowTowerFallback_usesArrowBaseColorAndStroke() {
        ArrowTower tower = new ArrowTower(0, 0);
        rt.assetAvailable = false;

        tower.render(rt);

        assertFalse(rt.fillsWith(GameColors.TOWER_BASE_ARROW).isEmpty(), "箭塔用箭塔族底座色");
        assertTrue(rt.has("strokeRect"), "塔身应描边");
    }

    // ===== 友军识别环（renderPostAnim）=====

    @Test
    void allyRenderPostAnim_usesFriendRingColorAndWidth() {
        Soldier soldier = new Soldier(0, 0, 60, 30, 8, 1000);

        soldier.renderPostAnim(rt);

        assertEquals(GameColors.FRIEND_RING, rt.stroke);
        assertEquals(2.0, rt.lineWidth, 1e-9);
        assertEquals(1, rt.count("strokeOval"));
    }

    @Test
    void eliteSoldierRenderPostAnim_alsoDrawsFriendRing() {
        EliteSoldier elite = new EliteSoldier(0, 0, 80, 40, 11, 800);

        elite.renderPostAnim(rt);

        assertEquals(1, rt.fillsWith(GameColors.GOLD_MARK).size(), "2 级应补肩章金星");
        assertEquals(1, rt.count("strokeOval"), "识别环仍由基类画一次");
    }
}
