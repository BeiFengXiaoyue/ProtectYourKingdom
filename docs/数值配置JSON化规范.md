# 数值配置 JSON 化规范

> 状态：**已实现**（`util.balance.BalanceTable`、`util.balance.BalanceLibrary`、`util.balance.TowerBalance`、`config/balance.json`、`config/tower.json`、`util.editor.BalanceEditorTool` 均已落地）
> 适用分支：`dev`
> 一句话：**玩法数值由一份可编辑的 JSON 唯一决定；工具类负责写它，运行期负责读它；Java 里的默认值只作「文件缺失时」的兜底。**

---

## 0. 数值归属总表（唯一事实源）

> **判定口径**：「可仅改文件生效」= 不改 Java、不重编译，只改文件即可生效。
> 任何「这个数值该写在哪」的争议，以本表为准；**不得新增第二套通道**。

| 数值 | 唯一归属 | 可仅改文件生效 | 生效时机 |
| :--- | :--- | :--- | :--- |
| 开局金币 / 生命 | `config/balance.json` | ✅ | 启动读取一次 |
| 总波数 | **`maps/<key>/waves.json` 唯一决定**；`balance.json.totalWaves` 降级为「波次表缺失时的兜底」 | ✅ | 启动读取一次 |
| 四类敌人 HP / 速度 / 赏金 | `config/balance.json` | ✅ | 启动读取一次 |
| 四类敌人 近战攻击力 / 攻击冷却 | `config/balance.json`（8 键）→ 敌人构造期取值 | ✅ | 启动读取一次 |
| 重甲物理减伤 | `config/balance.json` 的 `tankPhysicalReduction` → `TankEnemy` 构造期设 `damageTakenMultiplier` | ✅ | 启动读取一次 |
| 炮塔溅射半径（1/2/3 级） | `config/balance.json` 的 3 个 `*CannonSplashRadius` 键 → 炮塔构造期 → `Bomb` 构造参数 | ✅ | 塔构造期 |
| Boss 狂暴 震地间隔 / 攻击冷却削减 | `config/balance.json` 的 `bossStompIntervalMs` / `bossEnrageAttackCooldownCut` → `BossEnemy` 构造期取值 | ✅ | 启动读取一次 |
| Boss 狂暴 阈值 / 速度倍率 | `BossEnemy` 类常量（`ENRAGE_HP_RATIO` / `ENRAGE_SPEED_MULTIPLIER`） | ❌ | — |
| 塔 伤害 / 射程 / 冷却 | `config/tower.json`（`arrowL1`…`barrackL3`，见 §2.3） | ✅ | 启动读取一次 |
| 士兵 HP / 速度 / 攻击 / 冷却 | `config/tower.json` 的 `barrackL1/L2/L3` 之 `soldier*` 键 | ✅ | 兵营构造期 |
| 塔 造价 / 升级费 | 塔类常量 `BUILD_COST` / `UPGRADE_COST` → `Main.registerTower` 转成 `TowerSpec` | ❌ | — |
| 士兵 视野 / 散步半径 / 拴绳 | `Soldier` 类常量（行为手感参数） | ❌ | — |
| 实体 宽 / 高 | `assets/sizes.json`（见《视觉尺寸配置规范》） | ✅ | 实体构造期 |
| 敌人路径 / 塔位 / 画布尺寸 | `maps/<key>/path.json`、`spots.json` | ✅ | 启动读取一次 |
| 波次组成 / 密度 / 准备时长 | `maps/<key>/waves.json`（见《关卡波次配置规范》） | ✅ | 启动读取一次 |
| 波次节奏 4 字段 | `GameConfig` 的 Java 字面量（明确**不纳入** JSON；`waveIntermissionMs` 可被 `waves[].maxPrepMs` 覆盖） | ❌ | — |
| 配色 `colorBackground` / `colorPath` | `GameConfig` 的 Java 字面量（属表现，非可调数值） | ❌ | — |

---

## 1. 数据流

```
src/main/resources/config/balance.json   ← 数值唯一源文件
        │
        │  ①开发期：编辑器「写」（repo 文件优先）
        ▼
util.balance.BalanceLibrary.read() / write()
        │
        │  ②运行期：「读」（classpath 只读）
        ▼
BalanceTable.runtime()   ──→ 实体构造期直读（塔 / 士兵 / 敌人 / Boss）
config.GameConfig        ──→ 构造期合并入自身字段（缺文件/缺字段 → 回退内置默认）
        │
        ▼
GameController / GameState / HUD ……（仍只用 GameConfig getter）
```

| 角色 | 面向 | 行为 |
|---|---|---|
| `util.balance.BalanceLibrary` | 开发期 | **读写仓库文件** `src/main/resources/config/balance.json`（编辑器保存） |
| `config.GameConfig` | 游戏启动 | 从 classpath 只读同一份文件；读到即用、缺失回退，**不写文件** |
| `util.balance.BalanceTable.runtime()` | 实体构造期 | 只读懒加载单例，供**不持有 `GameConfig` 引用**的实体取值（对齐 `SizeTable.getInstance()` 范式） |
| `util.balance.TowerBalance` | 塔 / 兵营构造期 | 只读 `config/tower.json`，取不到即返回调用点传入的兜底值 |

> 三条读取路径指向**同一份文件**，不是多份配置；写入通道只有 JSON 文件本身。

---

## 2. JSON 契约

### 2.1 文件与位置

- 全局数值：`src/main/resources/config/balance.json`
- 塔 / 士兵数值：`src/main/resources/config/tower.json`
- 两者均为**扁平对象**（顶层键 = 字段名），便于逐字段回退与人工对照，不支持嵌套分组（`tower.json` 为「塔 id → 字段表」两级）。
- 现行取值一律以这两个文件为准，**本文档不抄写具体数字**（避免文档与配置漂移）。

### 2.2 `balance.json` 键表（29 键）

| 分组 | 键 | 类型 | 合法范围 | 说明 |
|---|---|---|---|---|
| ① 开局 | `initialGold` | number | ≥0 整数 | 开局金币 |
| | `initialLives` | number | ≥1 整数 | 初始生命 |
| | `totalWaves` | number | ≥1 整数 | 总波数（⚠️ 通常被 `maps/<key>/waves.json` 覆盖，仅作兜底） |
| ② 普通敌人 | `normalHp` / `normalSpeed` / `normalGoldReward` | number | HP、赏金 ≥0 整数；速度 >0（可小数，px/s） | |
| ③ 快速敌人 | `fastHp` / `fastSpeed` / `fastGoldReward` | number | 同上 | |
| ④ 重甲敌人 | `tankHp` / `tankSpeed` / `tankGoldReward` | number | 同上 | |
| ⑤ Boss | `bossHp` / `bossSpeed` / `bossGoldReward` | number | 同上 | |
| ⑥ 敌人近战 | `normalAttackDamage` / `normalAttackCooldownMs` | number | 攻击力 ≥1 整数；冷却 ≥1 整数（ms） | 冷却下限为 1：0 冷却 = 无限攻速，禁止 |
| | `fastAttackDamage` / `fastAttackCooldownMs` | number | 同上 | |
| | `tankAttackDamage` / `tankAttackCooldownMs` | number | 同上 | |
| | `bossAttackDamage` / `bossAttackCooldownMs` | number | 同上 | |
| ⑦ 减伤 | `tankPhysicalReduction` | number | **[0, 1)** | 重甲物理减伤比例（仅普通箭矢吃减伤，炮塔破甲无视）。上界为开区间：≥1 会把伤害变成治疗 |
| ⑧ 溅射 | `cannonSplashRadius` / `eliteCannonSplashRadius` / `masterCannonSplashRadius` | number | **>0** | 1/2/3 级炮塔 `Bomb` 溅射半径（px）。开区间：0/负数 = 溅射永不命中 |
| ⑨ Boss 狂暴 | `bossStompIntervalMs` | number | ≥1 整数 | 狂暴后震地间隔（ms）；每间隔触发一次全塔眩晕 |
| | `bossEnrageAttackCooldownCut` | number | **[0, 1)** | 狂暴后攻击冷却削减比例；=1.0 会把冷却削到 0 → 无限攻速，禁止 |

> 全部字段**选填**：任一字段缺失 / 非数字 / 越界 → 该项回退内置默认，不影响其他字段。
> 调参提示：`bossStompIntervalMs` 设得过小会让全塔被反复眩晕，属预期行为而非 bug。

### 2.3 `tower.json` 键表（塔 id → 字段）

| 塔 id | 对应塔类 | 字段 |
|---|---|---|
| `arrowL1` / `arrowL2` / `arrowL3` | `ArrowTower` / `EliteArrowTower` / `MasterArrowTower` | `damage` / `range` / `cooldownMs`；L3 另有 `chainTargets` / `chainDamageRatio` |
| `cannonL1` / `cannonL2` / `cannonL3` | `CannonTower` / `EliteCannonTower` / `MasterCannonTower` | `damage` / `range` / `cooldownMs`；L3 另有 `burnDps` / `burnDurationMs` |
| `barrackL1` / `barrackL2` / `barrackL3` | `Barrack` / `EliteBarrack` / `MasterBarrack` | `maxSoldiers` / `spawnIntervalMs` / `soldierHp` / `soldierSpeed` / `soldierAttack` / `soldierCooldown`；L3 另有 `healPerSecond` / `auraRadius` |

- 读取入口：`util.balance.TowerBalance.getInt/getDouble(towerId, field, defaultValue)`，**字段缺失/文件缺失即返回调用点传入的兜底值**，不抛异常。
- 溅射半径**不在** `tower.json` 内，仍在 `balance.json`（§2.2 ⑧ 组）。

### 2.4 明确**不纳入** `balance.json` 的数值

| 不纳入项 | 归口 | 原因 |
|---|---|---|
| 画布 `viewWidth/viewHeight` | 地图 `path.json` 的 `width/height` | 会与地图数据形成双源 |
| 敌人路径 / 塔位 | `maps/<key>/path.json`、`spots.json` | 已由 `MapRoute` / `TowerSpots` 统一管理 |
| 配色 | `GameConfig` Java 字段 | 属表现，非「数值调控」 |
| 塔目录（显示名 / 造价） | `Main.registerTower(...)` 登记 | 属装配 |
| 单位宽高 | `assets/sizes.json` | 已有专项规范 |
| 波次节奏 4 键 | `GameConfig` Java 字面量 | 避免与波次模块并行开发冲突；`waveIntermissionMs` 可被 `waves[].maxPrepMs` 覆盖 |
| 塔 / 士兵 伤害 / 射程 / 冷却 / 士兵属性 | **`config/tower.json`** | 单独一份文件，见 §2.3 |
| 敌人 攻击力 / 冷却 / 减伤 / 溅射 / Boss 狂暴 2 参数 | **本文件** | 已纳入（§2.2 ⑥⑦⑧⑨） |

### 2.5 回退规则（严格顺序，全程不抛异常）

1. 文件存在且解析成功、顶层为对象 → 进入逐字段判定；
2. 逐字段：键存在且为**数字**且在合法范围内 → 采用该值；
3. 否则（缺失 / 非数字 / 越界 / 解析失败）→ 该字段取**内置默认**，打印一条 `[BalanceTable]` 前缀的警告；
4. 文件整体缺失 / 读取失败 / JSON 非法 / 顶层非对象 → **整表**回退内置默认，打印警告，游戏正常启动；
5. 内置默认**只允许存在于 `BalanceTable.defaults()` 一处**，且**必须与 `config/balance.json` 保持一致**（改一处请同步另一处）。

---

## 3. 运行期接线要点

- **`GameConfig` 仍是 `GameController` / `GameState` / `HUD` 的数值入口**：23 个 JSON 化字段（开局 3 + 四类敌人 HP/速度/赏金 12 + 八类近战攻击力/冷却 8）构造期从 `balance.json` 读入并合并，消费方一行不改。
- **重甲减伤不在 `GameConfig` 暴露**：敌人侧直接读 `BalanceTable.runtime()`，避免两套口径。
- **塔实例不持有 `GameConfig`**：塔 / 兵营构造期经 `TowerBalance` 读 `tower.json`；炮塔溅射半径与 Boss 狂暴参数经 `BalanceTable.runtime()` 读 `balance.json`。
- **生效时机**：一律「启动（或塔/实体构造）读取一次」，**不实现运行中热重载**——改 JSON 后需重启游戏。
- **未知键**：`BalanceTable.fromJson` 会把无对应实现的键收进内部 `extra`，运行期无人读取，仅在启动时告警。请勿写入无实现的字段（防「假配置通道」）。
- ⚠️ **编辑器覆盖范围**：`BalanceEditorTool` 表单目前仅覆盖前 15 键（开局 3 + 四类敌人 HP/速度/赏金 12）。**保存时以「文件现有值」为基底、只覆盖这 15 键**，因此表单外的 14 键（8 个近战参数、`tankPhysicalReduction`、3 个溅射半径、2 个 Boss 狂暴参数）**按文件原值保留**，不会被打回内置默认；要改它们请直接编辑 `config/balance.json`。另：新增「自定义变量」时的重名校验取 `BalanceTable.fixedKeys()`（全部 29 键），避免与固定字段同名而在 JSON 中写出重复键。
- JSON 解析**复用 `util/json/MiniJson`**，不引入第三方库；`MiniJson` 数字统一解析为 `Double`，整型字段需 `((Number) v).intValue()` 转换。

---

## 4. 范围红线（不做清单）

- **不新增第二套数值通道**：数值唯一源文件 = `config/balance.json`（塔数值 = `config/tower.json`）；不得再在别处写死这些数值。
- 不改地图配置（`maps/**`）、动画配置（`assets/animations/**`）、尺寸配置（`assets/sizes.json`）。
- 不引入第三方 JSON 库。
- 不实现运行中热重载，只做「启动读取一次」。
- 实体类只**接收注入数值**，不改其索敌 / 移动 / 攻击逻辑。
- `GameConfig` / `GameController` / 抽象基类 / 接口属公共契约，改动需集成负责人过手。
