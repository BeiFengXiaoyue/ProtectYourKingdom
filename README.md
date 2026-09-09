# 项目简介
**项目名称**：王国保卫战：前线哨站（Kingdom Defense: Frontline Outpost）
本项目为一款基于 **JavaFX** 开发的塔防策略游戏，
玩家在预设路径旁建造箭塔、炮塔和兵营，抵御10波敌人进攻，
通过击杀敌人获取金币来升级或出售防御塔，最终击败Boss赢得胜利。

---

## 快速开始（运行游戏）
- 在 IDE 中运行 `com.kingdom.game.view.Main`（或执行 `mvn javafx:run`）。
- 点击「开始波次」出怪；生命值归零即失败，守住全部波次即胜利。
- 启动时会自动读取路线文件 `src/main/resources/maps/default_path.json`
  （若存在则按导出内容设置画布尺寸与敌人路径，不存在则使用内置默认路径）。

## 路径标注工具（路线编辑器）
类：`com.kingdom.game.util.PathEditorTool`（窗口本体为嵌套类 PathEditorToolApp）。
启动：直接运行 **`com.kingdom.game.util.PathEditorTool`** 或 **`PathEditorLauncher`** 均可
（入口类不继承 Application，绕开了“缺少 JavaFX 运行时组件”报错；不影响游戏入口）。

使用步骤：
1. 运行 `PathEditorLauncher` 打开工具窗口；
2. 「载入图片」选择地图底图；若无底图，可在右侧宽/高框填数后点「新建空白画布」；
3. 在图上标路径点（用鼠标操作）：
   - 左键点击：新增一个路径点（首点=敌人生成点，末点=漏怪终点）；
   - 右键点击：删除离光标最近的点；
   - `Backspace` / `Delete`：撤销最后一个点；`Esc`：清空全部；
   - 鼠标拖拽：微调已有的点；
4. 保存路线：
   - 「保存到游戏资源」→ 写入 `src/main/resources/maps/default_path.json`（要求以仓库根目录为工作目录运行；否则自动改为“另存为”）；
   - 「另存为 JSON」→ 自定义位置保存（可当草稿，之后用「载入 JSON」回显继续编辑）。

坐标约定：保存的是**画布像素坐标**，路径点为“中心点”，与敌人移动逻辑一致。

## 路线文件（maps/*.json）
由工具导出的 JSON 结构示例：
```json
{
  "name": "default_path",
  "image": "map.png",
  "width": 900,
  "height": 560,
  "points": [
    {"x": 60, "y": 160},
    {"x": 300, "y": 160},
    {"x": 300, "y": 360},
    {"x": 620, "y": 360},
    {"x": 620, "y": 240},
    {"x": 860, "y": 240}
  ]
}
```
- `width / height`：游戏画布尺寸（窗口随其变化）；
- `points`：路径拐点数组（≥2 个）；首点为出生点、末点为终点；
- `image` 仅供工具参考底图记录，游戏暂不加载。

## 敌人动画编辑器（行为帧序列）
类：`com.kingdom.game.util.AnimEditorTool`（窗口本体为嵌套类 AnimEditorApp）。
启动：直接运行 **`com.kingdom.game.util.AnimEditorTool`** 或 **`AnimEditorLauncher`**。

用途：把一个敌人的“每种行为模式”配一组关键帧图片并轮播；**每个敌人一个 JSON**（同名=去重覆盖），
保存到 `src/main/resources/assets/animations/<敌人名>.json`。

使用步骤：
1. 顶部输入**敌人名** → 点「按名载入」（已有=继续修改；没有=新建 idle/walk/attack 模板）；也可「打开 JSON…」直接选文件；
2. 左侧「行为模式」可**添加/删除模式**；选中一个模式；
3. 右侧「+ 添加关键帧(选图)」逐张选择图片 → 自动拷入
   `assets/enemies/<敌人名>/<模式>_<序号>.png` 并加入帧序列；可上移/下移/删除帧；
4. 设置 interval（每帧停留 tick，默认 6 ≈ 0.1s/帧）→ 点「轮播预览」检查；
5. 「保存」写回 `animations/<敌人名>.json`（同名覆盖）；「删除该敌人」删除对应 JSON。

JSON 结构（帧路径相对 `assets/`）：
```json
{
  "name": "normal_enemy",
  "interval": 6,
  "modes": {
    "idle": ["enemies/normal_enemy/idle_0.png"],
    "walk": ["enemies/normal_enemy/walk_0.png", "enemies/normal_enemy/walk_1.png"],
    "attack": []
  }
}
```

> 提示：当前仅提供“编辑器 + 动画表 JSON”，**游戏内敌人渲染叠加尚未接入**——配好的帧图暂不影响运行表现（详见 `docs/接口契约与抽象类说明.md` §4.6）。

设计说明：`docs/单位行为动画-渲染层设计.md`（外部推导模式 + 叠加渲染方案，敌/友通用，未实现）。
说明文档：`docs/接口契约与抽象类说明.md`（接口契约与抽象类说明）。
