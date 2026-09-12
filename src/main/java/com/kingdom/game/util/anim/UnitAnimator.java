package com.kingdom.game.util.anim;

import com.kingdom.game.model.GameObject;
import com.kingdom.game.model.LivingEntity;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * UnitAnimator —— 单位动画叠加层（外部观察 + 叠加绘制，按实例有状态）。
 *
 * 设计意图（见 docs/单位行为动画-渲染层设计.md §3.2）：
 * - 单位类零改动：动画层只读公开状态，在本单位 render() 之后“叠加”当前帧贴图；
 * - 模式判定交给 {@link UnitPoseResolver}，本类只负责“记上一帧坐标 → 推 tick → 取帧 → 画”；
 * - 回退：未注册 / 缺 JSON / 模式缺键 / 帧为空 / 缺帧图 → **一律无操作，单位保持原渲染**（回归无损）。
 *
 * 接线：Main 调 {@code register(kind, 单位类名, JSON资源地址)}；GameView 在敌/友/塔绘制循环里
 * 于各自 {@code render(gc)} 之后调用 {@code overlay(gc, unit)}。
 */
public final class UnitAnimator {

    /** 判定“本帧有位移”的坐标阈值（px） */
    private static final double MOVE_EPSILON = 0.01;

    /** 每实例动画状态（上帧坐标 / 当前模式 / tick） */
    private static final class UnitState {
        double lastX, lastY;
        String mode = UnitPoseResolver.MODE_IDLE;
        int tick = 0;
        boolean hasLast = false;
    }

    /** 单位类名（getSimpleName）→ 动画表 */
    private final Map<String, AnimTable> tablesByClass = new LinkedHashMap<>();

    /** 单位实例 → 动画状态（弱引用键，实体出战场后自动释放） */
    private final Map<GameObject, UnitState> states = new WeakHashMap<>();

    /** 帧图缓存：相对路径 → Image；加载失败记入 missing 做负缓存，避免每帧重试 IO */
    private final Map<String, Image> imageCache = new HashMap<>();
    private final Set<String> missingImages = new HashSet<>();

    /**
     * 登记“单位类名 → 动画表 JSON”。
     *
     * @param kind               类别（enemies/allies/towers），仅标识用
     * @param unitClassName      单位具体类名（{@code getSimpleName()}，如 NormalEnemy）
     * @param descriptorResource classpath 资源地址（如 /assets/animations/enemies/normal_enemy.json）
     */
    public void register(String kind, String unitClassName, String descriptorResource) {
        if (unitClassName == null || unitClassName.isBlank() || descriptorResource == null) return;
        AnimTable table = AnimTable.loadFromClasspath(descriptorResource);
        if (table == null) {
            System.err.println("[UnitAnimator] 未找到动画表，该单位保持原渲染: "
                    + kind + "/" + unitClassName + " -> " + descriptorResource);
            return;
        }
        tablesByClass.put(unitClassName, table);
    }

    /** 是否已为某单位类登记动画表 */
    public boolean isRegistered(String unitClassName) {
        return tablesByClass.containsKey(unitClassName);
    }

    /**
     * 本帧是否会为该单位叠加动画帧（已登记且当前模式有帧）。
     * GameView 据此跳过单位静态渲染——静态底图与动画帧透明叠加会产生残影。
     *
     * ⚠️ 必须与 {@link #overlay} 用**同一次位移判定**得出同一模式，否则会出现
     * 「判有却不画（该帧单位不可见）」或「判无却画了（静态+动画双画残影）」。
     * 本方法只读实例状态：不推进 tick、不更新上帧坐标、也不创建状态。
     */
    public boolean hasOverlay(GameObject unit) {
        if (unit == null) return false;
        // 与 overlay 的死亡分支保持一致：死亡单位不会叠加，须交回静态渲染
        if (unit instanceof LivingEntity && !((LivingEntity) unit).isAlive()) return false;
        AnimTable table = tablesByClass.get(unit.getClass().getSimpleName());
        if (table == null) return false;
        String mode = UnitPoseResolver.resolve(unit, movedThisFrame(unit, states.get(unit)));
        return table.hasMode(mode) && !table.getFrames(mode).isEmpty();
    }

    /**
     * 本帧相对上一帧是否有位移（**只读**：不更新 lastX/lastY、不推进 tick）。
     * 供 {@link #hasOverlay} 与 {@link #overlay} 共用，保证两者判定一致；状态未建立时按“未移动”处理。
     */
    private boolean movedThisFrame(GameObject unit, UnitState st) {
        return st != null && st.hasLast
                && (Math.abs(unit.getX() - st.lastX) > MOVE_EPSILON
                || Math.abs(unit.getY() - st.lastY) > MOVE_EPSILON);
    }

    /**
     * 在单位自身 render() 之后叠加当前动画帧；任何缺料情况都静默跳过。
     *
     * @param gc   画布上下文
     * @param unit 单位（GameObject：敌/友/塔）
     */
    public void overlay(GraphicsContext gc, GameObject unit) {
        if (gc == null || unit == null) return;

        // 死亡单位：清状态，不再叠加（移除由 GameController 负责）
        if (unit instanceof LivingEntity && !((LivingEntity) unit).isAlive()) {
            states.remove(unit);
            return;
        }

        AnimTable table = tablesByClass.get(unit.getClass().getSimpleName());
        if (table == null) return;

        UnitState st = states.get(unit);
        if (st == null) {
            st = new UnitState();
            states.put(unit, st);
        }

        // 本帧位移（跨帧由本层按实例记录；判定与 hasOverlay 共用同一私有方法）
        boolean moved = movedThisFrame(unit, st);
        st.lastX = unit.getX();
        st.lastY = unit.getY();
        st.hasLast = true;

        // 模式 → 帧序列；缺键/空帧 → 不叠加（保持原渲染）
        String mode = UnitPoseResolver.resolve(unit, moved);
        if (!table.hasMode(mode)) return;
        List<String> frames = table.getFrames(mode);
        if (frames == null || frames.isEmpty()) return;

        // 帧推进：模式切换重置 tick
        if (!mode.equals(st.mode)) {
            st.mode = mode;
            st.tick = 0;
        } else {
            st.tick++;
        }
        int interval = Math.max(1, table.getInterval());
        String framePath = frames.get((st.tick / interval) % frames.size());

        Image img = image(framePath);
        if (img == null) return;

        double w = unit.getWidth();
        double h = unit.getHeight();
        gc.drawImage(img, unit.getX() - w / 2.0, unit.getY() - h / 2.0, w, h);
    }

    /** 按相对 assets/ 的路径加载帧图（带缓存 + 缺失负缓存）；失败返回 null */
    private Image image(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return null;
        if (missingImages.contains(relativePath)) return null;
        Image cached = imageCache.get(relativePath);
        if (cached != null) return cached;

        String resource = relativePath.startsWith("/") ? relativePath : "/assets/" + relativePath;
        try (InputStream in = UnitAnimator.class.getResourceAsStream(resource)) {
            if (in == null) {
                missingImages.add(relativePath);
                return null;
            }
            Image img = new Image(in);
            imageCache.put(relativePath, img);
            return img;
        } catch (Exception e) {
            System.err.println("[UnitAnimator] 帧图加载失败: " + resource + " -> " + e.getMessage());
            missingImages.add(relativePath);
            return null;
        }
    }
}
