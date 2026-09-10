# 项目简介
**项目名称**：王国保卫战：前线哨站（Kingdom Defense: Frontline Outpost）
本项目为一款基于 **JavaFX** 开发的塔防策略游戏，
玩家在预设路径旁建造箭塔、炮塔和兵营，抵御多波敌人进攻（波数与组合由 `maps/<key>/waves.json` 唯一决定），
通过击杀敌人获取金币来升级或出售防御塔，最终击败Boss赢得胜利。

---

## 快速开始（运行游戏）
- 默认 `pom.xml` 的 javafx 插件 mainClass 为 `com.kingdom.game.view.Main`，直接执行 `mvn javafx:run` 运行游戏。
- IDE 直接运行不可用（JavaFX 模块检查），工具/游戏都请走 `mvn javafx:run`（工具见下节）。
- 点击「开始波次」出怪；生命值归零即失败，守住全部波次即胜利。
- 启动时会自动读取**默认地图**（`maps/index.json` 第一条，可用 `-Dmap.key=<key>` 指定）的
  `maps/<key>/path.json` 与 `spots.json`（若存在则按导出内容设置画布尺寸、敌人路径与可建塔位；不存在则回退内置默认）。
- ℹ️ 当前默认地图的波次（`maps/default/waves.json`）为**测试用例**数据（9 波、首波含 Boss），用于联调，**非最终关卡设计**；波数与组合由该文件决定，后续按游戏性需要调整。

## 运行工具（改 pom 后 mvn javafx:run）
所有工具/编辑器都通过**修改 `pom.xml` 的 javafx 插件 mainClass** 后用 `mvn javafx:run` 启动。
默认（游戏）为 `com.kingdom.game.view.Main`；运行工具时把它改成下表目标类，跑完改回游戏即可。

```xml
<!-- pom.xml javafx-maven-plugin 配置里 -->
<mainClass>com.kingdom.game.util.editor.PathEditorTool</mainClass>   <!-- 示例：运行路径标注工具 -->
```
命令：`mvn javafx:run`

| 工具 | pom 填的 mainClass |
|---|---|
| 游戏（默认） | `com.kingdom.game.view.Main` |
| 路径标注（路线编辑器） | `com.kingdom.game.util.editor.PathEditorTool` |
| 塔位标注 | `com.kingdom.game.util.editor.TowerSpotEditorTool` |
| 单位动画编辑器（敌人/友方/防御塔） | `com.kingdom.game.util.editor.AnimEditorTool` |
| 实体尺寸配置 | `com.kingdom.game.util.editor.SizeEditorTool` |
| 数值配置（balance.json） | `com.kingdom.game.util.editor.BalanceEditorTool` |
| 关卡波次配置 | `com.kingdom.game.util.editor.WaveEditorTool` |

> 注意：本环境**不支持在 IDE 直接运行工具类**（JavaFX 模块检查会报“缺少 JavaFX 运行时组件”）。
> 统一做法：把 pom 的 mainClass 改为目标工具类 → `mvn javafx:run` → 跑完改回 `com.kingdom.game.view.Main`。

## 路径标注工具（路线编辑器）
类：`com.kingdom.game.util.editor.PathEditorTool`（窗口本体为嵌套类 PathEditorToolApp）。
启动：**把 pom 的 `<mainClass>` 改成 `com.kingdom.game.util.editor.PathEditorTool` 后执行 `mvn javafx:run`**
（IDE 直接运行不可用，见上方“运行工具”说明）。

使用步骤：
1. 改 pom mainClass 后用 `mvn javafx:run` 打开工具窗口；
2. 顶栏选**地图**（maps/index.json 里已有地图）或点「新建地图…」（输入 key/名称并选底图）；
3. 选中地图会自动载入其底图与已有 `path.json`（无则空白）；在图上标路径点（左键加点、右键删最近、Backspace/Delete 撤销、Esc 清空、拖拽微调）；
4. 保存路线：
   - 「保存到该地图」→ 写入该地图的 `maps/<key>/path.json`（仓库根目录运行；否则自动改“另存为”）；
   - 「另存为 JSON」→ 自定义位置保存（草稿，可用「载入 JSON」回显继续编辑）。

坐标约定：保存的是**画布像素坐标**，路径点为“中心点”，与敌人移动逻辑一致。

## 地图分包与文件结构
```
src/main/resources/maps/
├── index.json                      注册表：{ maps:[ {key,name,image} ] }
└── <key>/                          一张地图一个目录
    ├── map.png                     底图（名称以 index 的 image 为准）
    ├── path.json                   敌人路径（MapRoute）
    ├── spots.json                  塔位（TowerSpots）
    └── waves.json                  关卡波次（LevelWaves，波数与出怪组合的唯一来源）
```
- 工具经 `maps/index.json` 列“已有地图”并可选择/新建；`PathEditorTool` 写 `path.json`，`TowerSpotEditorTool` 写 `spots.json`，`WaveEditorTool` 写 `waves.json`；
- 游戏启动时 `GameConfig` 读取默认地图（index 第一条，可用 `-Dmap.key=<key>` 指定）并加载 `path/spots/waves/底图`；
- 缺少某文件则该部分回退内置默认（缺 `waves.json` 时回退 `GameConfig` 的全局波次配置并告警）。

`path.json` 结构示例（沿用原 schema，仅“存放目录按地图分包”）：
```json
{
  "name": "default",
  "image": "map.png",
  "width": 700,
  "height": 600,
  "points": [ {"x":60,"y":160}, {"x":300,"y":160} ]
}
```
- `width / height`：画布尺寸（随地图）；`points`：路径拐点（≥2），首点为出生点、末点为终点。

## 单位动画编辑器（敌人 / 友方 / 防御塔）
类：`com.kingdom.game.util.editor.AnimEditorTool`（窗口本体为嵌套类 AnimEditorApp）。
启动：**把 pom 的 `<mainClass>` 改成 `com.kingdom.game.util.editor.AnimEditorTool` 后执行 `mvn javafx:run`**
（IDE 直接运行不可用，见“运行工具”说明）。

用途：给“敌人 / 友方 / 防御塔”任一单位的每种行为模式配一组关键帧并轮播；
**每个单位按类别存放一个 JSON**（同类别同名=去重覆盖），保存在
`src/main/resources/assets/animations/{enemies,allies,towers}/<单位名>.json`。

使用步骤：
1. 顶栏先选**类别**：敌人(enemies) / 友方(allies) / 防御塔(towers)；
2. **单位下拉**选单位：下拉含该类别已有表 + 已知单位类（敌人 Normal/Fast/Tank/Boss、友方 Soldier、塔 ArrowTower）；选择“新建模板”项或手动输入新单位名即可新建（也可「打开 JSON…」直接选文件）；
3. 左侧「行为模式」可**添加/删除模式**；选中一个模式；
4. 右侧「+ 添加关键帧(选图)」逐张选择图片 → 自动拷入
   `assets/<类别>/<单位名>/<模式>_<序号>.png` 并加入帧序列；可上移/下移/删除帧；
5. 设置 interval（每帧停留 tick，默认 6 ≈ 0.1s/帧）→ 点「轮播预览」检查；
6. 「保存」写回 `animations/<类别>/<单位名>.json`（同类别同名去重）；「删除该单位」删除对应 JSON。

默认模板模式：敌人/友方 = `idle/walk/attack`；防御塔 = `idle`（常时循环；暂不分“开火帧”，本轮不动塔基类）。

JSON 结构（帧路径相对 `assets/`，前缀为类别子目录）：
```json
{
  "kind": "enemies",
  "name": "normal_enemy",
  "interval": 6,
  "modes": {
    "idle": ["enemies/normal_enemy/idle_0.png"],
    "walk": ["enemies/normal_enemy/walk_0.png", "enemies/normal_enemy/walk_1.png"],
    "attack": []
  }
}
```

> 提示：当前提供“编辑器 + 动画表 JSON（三类通用）”，**游戏内渲染叠加已接入**（`util.anim.UnitAnimator` + `Main` 登记 6 类单位，帧图已入库）——配好的帧图会按 idle/walk/attack 轮播（详见 `docs/接口契约与抽象类说明.md` §4.6/§4.7）。

设计说明：`docs/单位行为动画-渲染层设计.md`（外部推导模式 + 叠加渲染方案，敌/友/塔通用，已实现）。
说明文档：`docs/接口契约与抽象类说明.md`（接口契约与抽象类说明）。
