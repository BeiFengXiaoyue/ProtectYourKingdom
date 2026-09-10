# 数值配置 JSON 化规范（GameConfig 外置 · 工具类可读写）

> 提出：集成/架构（A）｜执行：（待定，数值数据类由 A／可视化工具由 D）｜日期：2026-09-10
> 适用版本：dev（feature 分支）
> 任务性质：把**当前写在 Java 里、只能改代码重编译的玩法数值**改为**一份可编辑的 JSON 配置文件**；
> 新增 `util.balance` 工具类负责“改文件”，`GameConfig` 负责“读文件”。**本文档即派工单**，按 §五 实施、§八 验收。
> **状态：已实现**（2026-09-10 校准）——`util/balance/BalanceTable`、`util/balance/BalanceLibrary`、
> `src/main/resources/config/balance.json`、`GameConfig` 构造期接入、`util/editor/BalanceEditorTool` **均已落地**。
> ⚠️ 与初稿的差异：实际纳入 JSON 化的字段为 **24 个**（`initialGold/Lives/TotalWaves` + 四类敌人各 `Hp/Speed/GoldReward`
> + 四类敌人各 `AttackDamage/AttackCooldownMs`（8 个）+ `tankPhysicalReduction`），
> **不含**波次节奏 4 键（`waveEnemyCount`/`waveSpawnIntervalMs`/`waveIntermissionMs`/`earlyStartRewardCap`）——
> 以 §3.2 现行表与 §3.4 说明为准。
>
> 🆕 **2026-09-10 第二批**：§3.2 新增 9 键（8 个近战攻击力/冷却 + 1 个重甲减伤）。**A 侧读通道已落地**
> （`BalanceTable` 字段/`FIXED_KEYS`/`defaults`/`toJson`/`fromJson` + `GameConfig` 9 个 getter）；
> ⏳ **仍待**：`balance.json` 取值与 `BalanceEditorTool` 输入框（F-1）、`spawnEnemy` 传参（待 B-4 扩构造）、
> 减伤与破甲的消费实现（B-5 / C）。在 F-1 之前，这 9 项一律走 `BalanceTable.defaults()` 内置默认。

---

## 一、背景与问题

**现状**：玩法数值全部以 Java 字段默认值写在 `config/GameConfig.java`：

| 位置 | 数值 | 后果 |
|---|---|---|
| `GameConfig.java:31-33` | 初始金币 100 / 生命 20 / 波数 10 | 改数值＝改 Java、重新编译 |
| `GameConfig.java:36-38` | 普通敌人 HP80 / 速度60 / 赏金10 | 同上 |
| `GameConfig.java:41-42` | 每波 3 只 / 出生间隔 1000ms | 同上 |
| `GameConfig.java:45-46` | 波间倒计时 5000ms / 提前奖励上限 10 | 注释自称“临时数值，待定稿调整”，却仍需改代码 |

`view/Main.java:35-37` 只能靠“改一行 Java 再编译”来微调：

```java
// ===== 数值配置（开发期可在此微调）=====
GameConfig config = new GameConfig();
// 例：config.setInitialLives(30).setNormalStats(120, 80, 10);
```

**问题**：
1. 数值调优（平衡性微调）是高频、非程序性的工作，却要求动 Java、重编译，玩法/数值方无法自助；
2. 与项目其余数据体系不对称——地图（`maps/<key>/*.json`）与动画（`assets/animations/*.json`）都已是“**JSON 源文件 + 工具类读写 + 运行期读取 + 缺失回退**”，唯独数值还是代码；
3. 已有的 `GameConfig` 构造器（`GameConfig.java:69-94`）本来就演示了这套“读 classpath JSON，缺了就用内置默认”的写法，只是没被用到数值上。

**根因**：数值缺少一个“**可被工具读写的数据文件**”作为来源，而 `GameConfig` 缺少对应的读取入口。

---

## 二、目标与总体方案

**一句话目标**：玩法数值由一份**可编辑的配置文件**唯一决定；工具类负责改它，`GameConfig` 负责读它，Java 默认值只作“文件缺失时的回退”，改数值不再动代码。

**数据流（双轨）**：

```
src/main/resources/config/balance.json   ← 数值的唯一源文件
        │                                          
        │  ①开发期：工具类/编辑器「写」
        ▼                                          
util.balance.BalanceLibrary.read()/write()  （repo 文件优先）
        │                                          
        │  ②运行期：GameConfig 启动时「读」（classpath 只读）
        ▼                                          classpath:/config/balance.json
config.GameConfig  ← 构造期读取并合并到自身字段（缺文件/缺字段 → 回退内置默认）
        │
        ▼
GameController / GameState / HUD ……（消费方一行不改，仍只用 GameConfig getter）
```

**两条轨的职责边界（务必理解）**：

| 角色 | 面向 | 行为 |
|---|---|---|
| `util.balance.BalanceLibrary`（工具类） | 开发期 | **读写仓库文件** `src/main/resources/config/balance.json`（编辑器保存、工具调用） |
| `config.GameConfig`（运行期） | 游戏启动 | 从 **classpath** 只读同一份文件，读到即用、缺失回退，**不写文件** |

> 两条轨指向**同一个文件**，不是两份配置——这是“工具改文件、GameConfig 读文件”的闭环。

**改造范围原则**：
- 只搬“**数值**”，不搬结构数据：画布尺寸/路径/塔位仍归地图 JSON，塔目录（`TowerSpec` 列表）仍由 `Main` 登记；
- 首版 seed JSON 的每个值 = 现在 `GameConfig` 的 Java 默认值，改造后**表现完全不变**；
- 数值此后调整 = 改 `config/balance.json`（可选 §五·6 可视化工具），**不碰 Java**；
- 缺文件、缺字段、字段非法 → **一律回退内置默认、不抛异常、不阻断启动**（对齐 `Assets` / `SizeTable` 的容错风格）；
- **生效时机**：`GameConfig` 构造期读取一次并固化；改 JSON 后**重启游戏生效**，运行中不重读、不逐帧查表。

---

## 三、JSON 契约

### 3.1 文件与位置

- 唯一文件：`src/main/resources/config/balance.json`。
- 开发期读写**仓库文件**；运行期从 **classpath**（`/config/balance.json`）读同一份（Maven 自动把 `src/main/resources` 打进 classpath）。
- 顶层为**扁平对象**（键即 `GameConfig` 字段名，便于逐字段回退与人工对照），不支持嵌套分组。

### 3.2 字段表

| 键 | 类型 | 必填 | 对应 `GameConfig` | 合法范围 | 说明 |
|---|---|---|---|---|---|
| `initialGold` | number | 否 | `initialGold` | ≥0 整数 | 开局金币 |
| `initialLives` | number | 否 | `initialLives` | ≥1 整数 | 初始生命 |
| `totalWaves` | number | 否 | `totalWaves` | ≥1 整数 | 总波数（⚠️ **通常被关卡波次表覆盖，仅作兜底**，见 §3.4） |
| `normalHp` | number | 否 | `normalHp` | ≥1 整数 | 普通敌人 HP |
| `normalSpeed` | number | 否 | `normalSpeed` | >0 | 普通敌人速度（px/s，允许小数） |
| `normalGoldReward` | number | 否 | `normalGoldReward` | ≥0 整数 | 普通敌人击杀赏金 |
| `fastHp` | number | 否 | `fastHp` | ≥1 整数 | 快速敌人 HP |
| `fastSpeed` | number | 否 | `fastSpeed` | >0 | 快速敌人速度 |
| `fastGoldReward` | number | 否 | `fastGoldReward` | ≥0 整数 | 快速敌人赏金 |
| `tankHp` | number | 否 | `tankHp` | ≥1 整数 | 重甲敌人 HP |
| `tankSpeed` | number | 否 | `tankSpeed` | >0 | 重甲敌人速度 |
| `tankGoldReward` | number | 否 | `tankGoldReward` | ≥0 整数 | 重甲敌人赏金 |
| `bossHp` | number | 否 | `bossHp` | ≥1 整数 | Boss HP |
| `bossSpeed` | number | 否 | `bossSpeed` | >0 | Boss 速度 |
| `bossGoldReward` | number | 否 | `bossGoldReward` | ≥0 整数 | Boss 赏金 |
| `normalAttackDamage` | number | 否 | `normalAttackDamage` | ≥1 整数 | 普通敌人近战攻击力（默认 5） |
| `normalAttackCooldownMs` | number | 否 | `normalAttackCooldownMs` | ≥1 整数 | 普通敌人近战冷却 ms（默认 1000） |
| `fastAttackDamage` | number | 否 | `fastAttackDamage` | ≥1 整数 | 快速敌人攻击力（默认 4） |
| `fastAttackCooldownMs` | number | 否 | `fastAttackCooldownMs` | ≥1 整数 | 快速敌人冷却 ms（默认 700） |
| `tankAttackDamage` | number | 否 | `tankAttackDamage` | ≥1 整数 | 重甲敌人攻击力（默认 9） |
| `tankAttackCooldownMs` | number | 否 | `tankAttackCooldownMs` | ≥1 整数 | 重甲敌人冷却 ms（默认 1400） |
| `bossAttackDamage` | number | 否 | `bossAttackDamage` | ≥1 整数 | Boss 攻击力（默认 22） |
| `bossAttackCooldownMs` | number | 否 | `bossAttackCooldownMs` | ≥1 整数 | Boss 冷却 ms（默认 1100） |
| `tankPhysicalReduction` | number | 否 | `tankPhysicalReduction` | **[0, 1)** | 重甲物理减伤比例（默认 0.3 = 减伤 30%；0 = 不减伤） |

> 🆕 末 9 行 = 2026-09-10 新增（《整改方案》§7 P0-1 / P0-3）。冷却下限为 **1**（0 冷却 = 无限攻速，禁止）；
> `tankPhysicalReduction` 上界为**开区间**（≥1 会把伤害变成治疗），故 `fromJson` 单列 `readRatio` 校验，越界告警回退默认。

> ⚠️ **原「已知缺口」已关闭**：四类敌人的攻击力 / 冷却**已有对应键**（上表末 9 行），不再写死在实体类。
> 剩余待办仅为**落地**：`balance.json` 取值 + `BalanceEditorTool` 输入框（F-1）、`spawnEnemy` 传参（待 B-4）、
> 减伤 / 破甲的消费实现（B-5 / C）——在此之前的运行期表现与改造前完全一致（走内置默认）。
> 另：`BalanceTable.fromJson` 会把**未知键**收进内部 `extra`，**运行期无人读取**；现已改为**启动时打印
> `[BalanceTable] 未知字段 "…" 已忽略（运行期不读取…）` 告警**，编辑器的「自定义变量」区也加了醒目提示——
> 仍请勿在 `balance.json` 写无对应实现的字段（见《整改方案》§7 P0-2）。

> 全部字段“否（选填）”：任一字段缺失/非法 → 该项回退内置默认，不影响其他字段。

### 3.3 首版 seed 内容（= 现行 `config/balance.json` 实际内容）

```json
{
  "initialGold": 100,
  "initialLives": 20,
  "totalWaves": 10,
  "normalHp": 80,
  "normalSpeed": 60,
  "normalGoldReward": 10,
  "fastHp": 50,
  "fastSpeed": 120,
  "fastGoldReward": 15,
  "tankHp": 240,
  "tankSpeed": 35,
  "tankGoldReward": 25,
  "bossHp": 1500,
  "bossSpeed": 40,
  "bossGoldReward": 200
}
```

> 与 `BalanceTable.defaults()`（内置默认）完全一致。**注意 `totalWaves` 会被 `maps/<key>/waves.json` 覆盖**（§3.4）。
>
> 🆕 上表**尚未**包含 §3.2 新增的 9 键（8 个近战 + `tankPhysicalReduction`）：它们是**选填项**，缺失即回退内置默认
> （5/1000、4/700、9/1400、22/1100、0.3），因此不补也不影响启动；补写由 **F-1** 在定稿数值时一并完成。

### 3.4 明确**不纳入**本文件的数值（避免第二套通道）

| 不纳入项 | 归口 | 原因 |
|---|---|---|
| 画布 `viewWidth/viewHeight` | 地图 `path.json` 的 `width/height`（`GameConfig.java:86-87` 覆盖） | 会与地图数据形成双源 |
| 敌人路径 `pathX/pathY` | 地图 `maps/<key>/path.json` | 已由 `MapRoute` 统一管理 |
| 塔位 `towerSpots` | 地图 `maps/<key>/spots.json` | 已由 `TowerSpots` 统一管理 |
| 配色 `colorBackground/colorPath` | 渲染常量（留在 `GameConfig` Java 字段） | 属表现，非“数值调控” |
| 塔目录 `towerSpecs`（显示名/造价） | `Main` 的 `registerTower(...)` 登记 | 属装配，非本任务 |
| 单位宽高 | `assets/sizes.json`（见《视觉尺寸配置规范》） | 已有专项规范 |
| 波次节奏 `waveEnemyCount` / `waveSpawnIntervalMs` / `waveIntermissionMs` / `earlyStartRewardCap` | `GameConfig` 的 Java 字面量（波次模块） | 初稿 §3.2 曾把这 4 键列入，实施时为避免与波次模块并行开发冲突而**明确不纳入**；`waveIntermissionMs` 可被波次表 `waves[].maxPrepMs` 覆盖 |
| 敌人 **攻击力 / 攻击冷却** | ✅ **已纳入本文件**（§3.2 末 9 行） | 2026-09-10 关闭原"待纳入"项：A 的读通道已落（`BalanceTable`/`GameConfig`）；**取值与编辑器待 F-1**、`spawnEnemy` 传参待 B-4 |
| 塔 / 士兵数值（造价/伤害/射程/冷却/溅射/士兵属性） | 各塔类与 `Barrack` 实例字段（当前**不可配**） | ⏳ **待纳入**（二期，见《整改方案》§7 P0-4） |

---

## 四、回退规则（严格顺序，全程不抛异常）

1. 文件存在且解析成功、顶层为对象 → 进入逐字段判定；
2. 逐字段：键存在且为**数字**且在合法范围内 → 采用该值；
3. 否则（键缺失 / 非数字 / 越界 / 解析失败）→ 该字段取**内置默认**，并打印一条 `[BalanceTable]` 前缀的 `System.err` 警告；
4. 文件整体缺失 / 读取失败 / JSON 非法 / 顶层不是对象 → **整表**回退内置默认，打印警告，游戏正常启动；
5. 内置默认 = §3.3 的 seed 值（即改造前 Java 字段值），且**只允许存在于 `BalanceTable` 一处**。

---

## 五、运行期行为契约（代码改造清单）

| # | 文件 | 改动要求 |
|---|---|---|
| 1 | `util/balance/BalanceTable.java` **（新增）** | 数值数据类：§三 全部字段 + getter/setter；`static BalanceTable defaults()`（内置默认）；`String toJson()`（顺序稳定、便于 diff）；`static BalanceTable fromJson(String)`（复用 `util.json.MiniJson`，单字段容错）；`static BalanceTable loadFromClasspath()`（读 `/config/balance.json`，缺失返回 `null` 并告警）。**无 JavaFX 依赖** |
| 2 | `util/balance/BalanceLibrary.java` **（新增）** | 仓库/工具类（仿 `util/map/MapLibrary`）：常量 `BALANCE_SUBDIR = "config"`、`BALANCE_FILE = "balance.json"`；`findRepoResourcesDir()`（同 `AnimLibrary.findRepoResourcesDir()` 规则，从 `user.dir` 向上找 `src/main/resources`）；`balanceFile()`；`BalanceTable read()`（**repo 优先**、退回 classpath，供工具/编辑器）；`BalanceTable readFromClasspath()`（供运行期 `GameConfig`）；`boolean write(BalanceTable)`（`mkdirs` + `Files.writeString`，失败返回 false 并告警） |
| 3 | `config/GameConfig.java` | 删除 **24 个 JSON 化字段**的字面量默认值（波次节奏 4 键保留字面量）；构造器开头 `BalanceTable bt = BalanceLibrary.readFromClasspath(); if (bt == null) bt = BalanceTable.defaults();` 再把 24 个字段赋为 `bt` 对应值（读地图逻辑不动）；**所有 getter / fluent setter 签名与语义保持不变**（新增 9 键**只加 getter**，写入通道仍是 `balance.json`） |
| 4 | `view/Main.java` | `:35-37` 的“数值配置（开发期可在此微调）”注释改为指引：改数值请编辑 `src/main/resources/config/balance.json`（示例保留 `config.setXxx(...)` 作为运行期瞬时覆盖手段的说明） |
| 5 | `util/editor/BalanceEditorTool.java` **（可选·二期）** | 可视化数值编辑工具，工程结构仿 `util.editor.AnimEditorTool`（`main()` 转发嵌套 `BalanceEditorApp extends Application`，规避 JavaFX 模块检查）：表单展示全部字段 → 校验范围 → 经 `BalanceLibrary.write()` 保存并提示“重启游戏生效”。**已交付 15 键；§3.2 新增 9 键的输入框与本地 `FIXED_KEYS` 由 F-1 补齐**（⚠️ 未补之前，工具保存会把 9 键以**内置默认值**写进 `balance.json`） |
| 6 | `src/main/resources/config/balance.json` **（新增）** | §3.3 的 seed 内容 |

**实现要求**：
- JSON 解析**复用 `util/json/MiniJson`**（与 `MapRoute`/`AnimTable`/`MapLibrary` 一致），不引入第三方 JSON 库；
- `MiniJson` 数字统一解析为 `Double`（`MiniJson.java:126-137`），整数/长整型字段需 `((Number) v).intValue()/longValue()` 转换；写出时整数不带 `.0`（同 `MapRoute.num()` 的写法）；
- `GameConfig` 仍是**运行期唯一数值入口**：实体/控制器/HUD 依旧只调 `config.getXxx()`，**零改动**（如 `GameState.java:19-21`、`GameController.spawnEnemy` 用 `config.getNormalHp()...`）；
- 删除 `GameConfig` 字段默认值后，若 JSON 缺失 → 走 `BalanceTable.defaults()`，表现与改造前一致——这是**预期**容错，不是 bug。

---

## 六、与既有文档红线的关系（需 A 拍板修订的措辞）

本规范的口径是“**`GameConfig` 仍是运行期唯一数值入口；JSON 只是它的可编辑持久层，不是第二套并行通道**”。据此与既有文档的对照：

| 文档 | 相关表述 | 结论 |
|---|---|---|
| `docs/接口契约与抽象类说明.md` §1（`:16-17`） | util 子包列表 `map/anim/editor/asset/audio/fx/json` | **需修订**：追加 `balance`，并补一句“数值文件 `resources/config/balance.json` 由 `util.balance` 管理” |
| 同上 §5.1（`:279-282`） | “GameConfig：实例对象 + getter/fluent setter…集中数值” | **需修订**：补“数值来自 `config/balance.json`（`util.balance.BalanceLibrary` 读取，缺省回退内置默认），运行期唯一 getter 入口不变” |
| `docs/视觉尺寸配置规范.md` §五（`:146`） | “实体 HP/速度/造价等数值**仍归 GameConfig** 与实体构造器，一律不动” | **不冲突**：本规范下数值仍“归 GameConfig”（运行期唯一入口），尺寸单只管宽高。**若后续二期把实体构造器里的数值也纳入**，才需同步修订此句 |
| `docs/关卡波次配置规范.md` §四（`:139`）· 红线（`:211`） | “数值由 A 在 GameConfig 侧按 id 补齐”“禁止 waves 里内联数值形成第二套数值通道” | **不冲突**：本期不动敌人类型数值归属；二期若把每类敌人数值写进 `balance.json`，需把“GameConfig 侧补齐”改写为“经 GameConfig 从 `balance.json` 按 id 读取”。数值始终在 `balance.json`、不在 `waves.json`，红线不破 |
| `README.md` | “运行工具”表 | 交付可选编辑器时，由提出方 A 顺带补一行（同《视觉尺寸配置规范》§八 的做法），不阻塞本任务验收 |

> 上述既有文档的修改**不在本次交付内**，仅列出建议；由 A 在派工验收后统一执行，避免多人并行改文档。

> **执行情况（2026-09-10 校准）**：第 1 条（《接口契约》§1 分包补 `balance`）与第 2 条（§5.1 补"数值来自 `config/balance.json`"）**已落地**；
> 第 3 条（《视觉尺寸配置规范》§五 不冲突）与第 4 条（《关卡波次配置规范》红线不冲突）沿用，并已在后者补"波数唯一来源 = `waves.json`、本文件 `totalWaves` 仅兜底"的说明；
> 第 5 条（README 补工具行）见《整改方案-文档与代码一致性》§6.9 待办。

---

## 七、分工

| 角色 | 交付 |
|---|---|
| **A（集成/架构）** | §五·1~4、6：`BalanceTable` / `BalanceLibrary` / `GameConfig` 接入 / `Main` 注释 / seed JSON |
| **D（UI/交互）· 可选** | §五·5：`BalanceEditorTool` 可视化编辑器 + Launcher（二期） |
| **F（测试）** | §八 验收：缺失回退、改值生效、往返一致 |

---

## 八、验收标准（Definition of Done）

1. `mvn -q compile` 通过，无新增编译警告；
2. 首版 `config/balance.json` 与 §3.3 一致；改造后启动游戏，**金币/生命/波数/敌人 HP·速度·赏金/出怪节奏与改造前完全一致**；
3. 改 JSON 中某值（如 `initialLives: 20 → 30`）→ 重启 → HUD 初始生命为 30；改 `normalHp` → 普通敌人更耐打；
4. **删除整个 `config/balance.json`**（模拟缺失）→ 全部回退内置默认，**不抛异常、能正常进游戏**；
5. JSON 中删除/写坏某字段（如 `normalHp` 删掉或写成 `"80"`）→ 仅该项回退默认，其余生效，控制台有警告；
6. `BalanceTable.toJson()/fromJson()` 往返一致（往返后字段值不变）；
7. `GameConfig` 内**搜索不到**被搬走的 24 个字段的默认字面量（100/20/10/80/60/50/120/15/240/35/25/1500/40/200，以及新增的 5/1000/4/700/9/1400/22/1100/0.3）；内置默认只存在于 `BalanceTable` 一处。（波次节奏的 `3/1000/5000/10` 仍允许存在于 `GameConfig` 字面量，见 §3.4）；
8. （若交付编辑器）工具改值保存 → 文件格式符合 §3.2，重启后表现与编辑值一致。

---

## 九、范围红线（不做清单）

- **不新增第二套数值通道**：数值唯一源文件 = `config/balance.json`，唯一运行期入口 = `GameConfig`；不得再在别处写死这些数值；
- 不改实体类（`model/enemy/*`、`model/tower/*`、`model/ally/*`）内的索敌/移动/攻击逻辑——实体类**只接收注入数值**；
  > 📌 **2026-09-10 限定语**：本红线原表述"不改实体类内的**战斗数值**"已被《整改方案》§7 P0-1 取代——
  > 四类敌人的攻击力/冷却**已进 `balance.json`**（§3.2 末 9 行），实体类中原先写死的 `attackDamage = 5/4/9/22`、
  > `maxAttackCooldown = 1000/700/1400/1100` 属**待删除的写死值**（由 B-4 扩构造、A 补 `spawnEnemy` 传参后移除），
  > 不再受本红线保护。塔/士兵数值（P0-4）仍属二期，维持原红线；
- 不动塔升级链与精英塔（`Tower.upgrade()` 的 ×1.3/×1.2 规则已随该"数值式升级"实现一并删除、`Elite*Tower` 升级链已接线）——塔数值入 JSON 属二期讨论范围；
- 不改地图配置（`maps/**`）、动画配置（`assets/animations/**`）、尺寸配置（`assets/sizes.json`）；
- 不引入第三方 JSON 库；不新增并行“关卡/波次/数值数据源”；
- 生效时机只做“启动读取”，不实现运行中热重载；
- `GameConfig` 等公共类仅 A 可改：执行方交付 diff，由 A review 后合入（对齐既有红线惯例）。

---

## 十、参考

**现状位置**：
- 数值字段：`config/GameConfig.java:27-46`；消费方：`model/GameState.java:18-23`、`controller/GameController.java:196-205,232,255-259`；
- 现有“读 classpath JSON + 缺失回退”范例：`config/GameConfig.java:69-94`（读地图）、`util/map/MapRoute.java:165-174`、`util/anim/AnimTable.java:193-213`。

**可复用范式**：
- `util/map/MapRoute`（数据类：`of()` 工厂 + `toJson`/`fromJson`/`loadFromClasspath`）；
- `util/map/MapLibrary`（仓库：`findRepoResourcesDir()` + repo 优先/classpath 回退读写）；
- `util/asset/Assets`（懒加载缓存、缺失静默回退）；
- `util/json/MiniJson`（项目内极简 JSON 解析器，不引第三方库）。

**相关文档**：`docs/接口契约与抽象类说明.md`（§1 分包、§5.1 GameConfig）、`docs/视觉尺寸配置规范.md`、`docs/关卡波次配置规范.md`、`README.md`。
