---
name: pm
description: Use when managing software projects — initializing, proposing changes, planning phases, executing plans, verifying deliverables, debugging issues, or resuming previous work. Triggers on any project management intent or when .project/ directory exists.
---

# PM — 渐进式项目管理

统一 GSD 全生命周期执行与 OpenSpec 变更隔离，所有状态存放在 `.project/` 下，纯 markdown，无需外部 CLI。

**核心原则：** 上下文先行，缓存避重复，渐进式采用。
**语言规则：** 始终使用中文回复、编写文档、注释和提交信息。代码标识符保持英文。

## 命令速查

| 命令 | 层级 | 说明 |
|------|------|------|
| `pm:quick` | L0 | 无仪式，直接干 |
| `pm:init` | L1+ | 初始化项目 |
| `pm:explore` | L1+ | 探索模式（只思考不写码） |
| `pm:propose` | L1 | 提议变更（proposal + tasks） |
| `pm:apply` | L1 | 实现变更任务 |
| `pm:archive` | L1 | 归档已完成变更 |
| `pm:plan` | L2 | 研究 + 规划阶段 |
| `pm:execute` | L2 | 波次并行执行计划 |
| `pm:verify` | L2 | 目标倒推验证 |
| `pm:status` | 全部 | 显示进度，路由下一步 |
| `pm:resume` | 全部 | 恢复上次工作 |
| `pm:debug` | 全部 | 科学方法调试 |

---

## 启动协议（每次会话必须执行）

### 步骤 0：自动初始化（当 `.project/` 不存在时）

**绝不询问用户 "你要构建什么"。** 直接行动：

**情况 A — 工作目录已有代码文件：**

1. 派生子代理并行扫描代码库：
   - 扫描目录结构，读取 package.json / pom.xml / Cargo.toml / go.mod / pyproject.toml
   - 读取 README.md / CLAUDE.md（如果存在）
   - 扫描主要源码目录的文件头和导出
   - 检测技术栈、框架、测试框架、构建工具
2. 自动生成 PROJECT.md（项目名从 package.json/README 推断，已有功能作为"已验证"需求）、STATE.md、CONFIG.md
3. 初始化 `.cache/`（index.md + relations.md + decisions.md）
4. git commit: `docs(project): 自动初始化项目（从现有代码库扫描）`
5. 向用户报告扫描结果，等待指令

**情况 B — 空目录：**
创建最小 `.project/`（PROJECT.md 标题占位 + STATE.md + CONFIG.md），告知用户已创建，等待输入。

### 步骤 1：加载上下文

**必读：** `.project/STATE.md`、`.project/PROJECT.md`
**按需：** `.project/CONFIG.md`、`.project/ROADMAP.md`、`.project/REQUIREMENTS.md`
**活跃工作：** `.project/changes/*/tasks.md`、`.project/todos/pending.md`

### 步骤 2：加载/构建缓存索引

读取 `.project/.cache/index.md`。不存在或过期则从上下文文件构建并写入。

### 步骤 3：向用户报告

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 PM ► 上下文已加载
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
项目: [名称]  进度: [██████░░░░] XX%
当前: [正在做什么 / 上次停在哪里]
下一步: [建议操作]
```

---

## 缓存与索引机制

所有缓存写入 `.project/.cache/`，跨会话持久化：

```
.project/.cache/
├── index.md        # 文件路径 → 内容摘要 + 关键值
├── relations.md    # 导入/导出/测试 关联图
└── decisions.md    # 已确定的设计决策（避免重复询问）
```

**index.md 条目格式：**
```markdown
### [文件路径]
- **摘要**: [一句话描述]
- **关键值**: [提取的关键数据]
- **导出**: [函数/类/组件]
- **行数**: [N]
```

**relations.md 条目格式：**
```markdown
### [文件路径]
- **被导入于**: [文件列表]
- **导入自**: [文件列表]
- **测试文件**: [路径]
- **关联变更**: [.project/changes/名称/]
- **关联需求**: [REQ-ID 列表]
```

**decisions.md 格式：**
```markdown
## 已确定
| 决策 | 来源 | 内容 |
|------|------|------|

## 待决定
| 问题 | 上下文 | 需要谁决定 |
|------|--------|-----------|
```

**维护规则：**
- 会话启动 → 写入/更新 index.md
- 首次读取源码 → 追加到 index.md + relations.md
- 用户做设计决策 → 追加到 decisions.md
- 完成任务 → 更新对应条目
- 自己修改文件 → 刷新该文件条目
- 超过 10 轮对话 → 刷新 STATE.md 条目
- 修改文件前 → 查 relations.md 找同步修改目标
- 涉及设计选择 → 查 decisions.md 确认是否已决定

---

## 目录规范

```
.project/
├── PROJECT.md              # 项目身份、愿景、约束
├── STATE.md                # 当前位置、会话连续性
├── CONFIG.md               # 工作流偏好
├── ROADMAP.md              # 里程碑/阶段结构（可选）
├── REQUIREMENTS.md         # 需求追踪（可选）
├── .cache/                 # 缓存索引（自动维护）
├── changes/                # 隔离变更（OpenSpec 风格）
│   ├── <名称>/
│   │   ├── proposal.md     # 做什么 & 为什么
│   │   ├── design.md       # 怎么做（架构决策）
│   │   ├── tasks.md        # 实现步骤（复选框）
│   │   └── summary.md      # 完成总结
│   └── archive/YYYY-MM-DD-<名称>/
├── phases/                 # 阶段执行（GSD 风格）
│   └── <NN>-<slug>/
│       ├── NN-CONTEXT.md   # 讨论决策（锁定决策 vs 自主裁量）
│       ├── NN-RESEARCH.md  # 技术研究
│       ├── NN-PLAN-WW-PP.md # 执行计划（波次-计划编号）
│       ├── NN-SUMMARY-WW-PP.md
│       └── NN-VERIFY.md    # 三级验证报告
├── research/               # 项目级研究（STACK/FEATURES/ARCHITECTURE/PITFALLS/SUMMARY）
├── debug/                  # 调试会话（科学方法持久化）
│   └── resolved/
└── todos/pending.md
```

**层级：** L0 无需目录 | L1 changes/ | L2 phases/ + ROADMAP | L3 全部

---

## pm:explore — 探索模式

**这是一种立场，不是工作流。** 没有固定步骤、没有必需的输出。你是思考伙伴。

**IMPORTANT: 探索模式只思考不写码。** 可以读取文件、搜索代码、画图分析，但绝不写应用代码。如果用户要求实现，提醒他们先退出探索模式创建变更提案。可以创建 `.project/` 下的文档（那是捕获思考，不是实现）。

### 立场

- **好奇而非指令式** — 自然涌现的问题，不是脚本
- **开放线索** — 呈现多个方向，让用户追随共鸣的
- **可视化** — 大量使用 ASCII 图（系统图、状态机、数据流、架构草图、对比表）
- **自适应** — 追随有趣的线索，新信息出现时转向
- **耐心** — 不急于结论，让问题的形状自然显现
- **接地** — 探索实际代码库，不空谈理论

### 入口处理

**用户带来模糊想法 →** 画出可能性光谱（如协作类型：感知/协调/同步），问"你在想哪个方向？"

**用户带来具体问题 →** 读取代码库，画出当前架构图，识别纠缠点，问"哪个在烧？"

**用户在实现中卡住 →** 读取变更产物，画出选项图，提供路径建议

**用户要比较选项 →** 追问上下文而非给通用答案，画对比表

### 见解结晶时

| 见解类型 | 捕获到哪里 |
|---------|-----------|
| 新功能想法 | → pm:propose |
| 设计决策 | → design.md |
| 范围变更 | → proposal.md |
| 新任务 | → tasks.md |
| 发现 bug | → pm:debug |

**用户决定。** 提供选择，不强制捕获，不自动保存。

### 护栏

- 不写应用代码
- 不假装理解（不清楚就深挖）
- 不催促（探索是思考时间）
- 不强加结构（让模式自然显现）
- 不自动捕获（提供后继续，不施压）

---

## pm:propose — 提议变更

创建隔离变更提案 `.project/changes/<名称>/`，生成三个产物：
- **proposal.md** — 做什么 & 为什么
- **design.md** — 怎么做（可选，简单变更跳过）
- **tasks.md** — 实现步骤（复选框）

### 流程

1. **获取变更描述** — 从参数获取或询问用户。派生 kebab-case 名称。
2. **检查重复** — 同名变更已存在则问续做还是新建。
3. **创建目录** — `mkdir -p .project/changes/<名称>`
4. **收集上下文** — 读取 PROJECT.md 获取约束，侦察代码库（架构、集成点、约定）
5. **写 proposal.md** — 包含：做什么、为什么、范围（包含/不包含）、影响（文件列表、风险、依赖）
6. **评估复杂度** — 非简单变更则写 design.md（方案、架构决策、数据流、风险/缓解）
7. **写 tasks.md** — 具体任务带复选框，按 准备/实现/验证 分组
8. **更新 STATE.md + .cache/index.md** → git commit

**产物创建指南（来自 OpenSpec）：**
- `context` 和 `rules` 是给你的约束，不要复制到产物文件中
- 读取依赖产物后再创建新产物
- 上下文严重不清楚时才问用户 — 更倾向于做合理决定保持动力
- 每个产物写完后验证文件存在

**任务质量要求：**
- 每个任务必须具体到可直接实现（不问问题就能做）
- 包含文件路径、具体值、验证条件
- 禁止模糊表述（"对齐 X 和 Y" → 写出具体目标值）

---

## pm:apply — 实现变更任务

### 选择变更
- 参数指定 → 使用
- 唯一活跃变更 → 自动选择并宣布
- 多个 → 让用户选择
- 无变更 → 提示先 pm:propose

始终宣布："使用变更：**[名称]**"

### 读取上下文
读取该变更的 proposal.md + design.md + tasks.md。查 `.cache/relations.md` 了解关联，查 `.cache/decisions.md` 确认已有决策。

### 实现循环

对每个待办 `- [ ]` 任务：
1. 显示正在做哪个任务
2. 读取相关文件 → 实现代码修改 → 保持最小聚焦
3. 标记 `- [x]` → git commit: `feat(change-<名称>): [任务描述]`
4. 继续下一个

**暂停条件：**
- 任务不清楚 → 问用户
- 实现暴露设计问题 → 建议更新产物
- 遇到错误/阻塞 → 报告等待指导
- 用户中断

**流式工作流集成（来自 OpenSpec）：**
- 可随时调用（任务部分完成也行）
- 允许产物更新（实现暴露设计问题时建议更新 design.md）
- 不锁定阶段，灵活工作

---

## pm:plan — 研究 + 规划阶段

**需要 ROADMAP.md（L2 模式）。** 默认流程：上下文 → 研究(可选) → 规划 → 验证 → 完成。

### 深度提问（来自 GSD questioning.md）

**你是思考伙伴，不是访谈者。** 用户通常有模糊想法，你帮他们锐化。

- **开放式开始** — 让他们倾倒心智模型，不要用结构打断
- **追随能量** — 他们强调的东西深挖（什么让他们兴奋？什么问题触发了这个？）
- **挑战模糊** — 永不接受模糊回答（"好" 意味着什么？"用户" 是谁？"简单" 怎么定义？）
- **抽象变具体** — "带我走一遍使用流程" "那实际长什么样？"
- **知道何时停止** — 理解了 做什么/为什么/给谁/完成什么样 → 提议继续

**反模式：** 清单遍历、罐头问题、企业话术、审讯式提问、浅层接受

### 研究阶段

如果 CONFIG.md 启用研究且无 `--skip-research`：

派生研究子代理（见 `agents/pm-researcher.md`），产出 RESEARCH.md 包含：
- **技术栈推荐** — 具体库+版本+理由+置信度
- **架构模式** — 适用场景、工作原理、代码示例
- **集成点** — 与现有代码的连接
- **陷阱** — 风险、预防、检测
- **不要自建** — 应该用库的功能
- **验证架构** — 测试框架和波次 0 缺口

研究员必须：验证训练数据为假设（非断言），优先 Context7 → 官方文档 → WebSearch

### 规划阶段

派生规划子代理（见 `agents/pm-planner.md`），产出 PLAN 文件：

**PLAN 文件格式：** `[NN]-PLAN-[WW]-[PP].md`（阶段-PLAN-波次-计划编号）

```markdown
# 计划 WW-PP: [名称]

## 元数据
- **阶段**: [N] — [名称]
- **波次**: [W]
- **依赖**: [计划 ID 或 "无"]
- **自主**: true | false
- **需求**: [REQ-ID]

## 目标
[2-3 句：构建什么、为什么重要]

## 必须交付
- [可观察结果]

## 任务

### 任务 N: [名称]

<read_first>
[实现前必读文件 — 包括被修改文件本身]
</read_first>

<action>
[具体步骤含实际值。不写 "对齐配置" 而写 "设置 DATABASE_URL=postgresql://..."。
执行者仅凭此文本即可完成任务。]
</action>

<acceptance_criteria>
[可 grep 验证: `file.ts 包含 'export function X'`]
[测试命令: `npm test -- x.test.ts 退出码 0`]
</acceptance_criteria>
```

**反浅层执行规则（强制）：** 每个任务**必须**有 read_first + 具体 action + 可验证 acceptance_criteria。模糊指令 = 浅层执行。具体指令的成本远低于重做的成本。

**TDD 计划（来自 GSD tdd.md）：** 当行为可用 `expect(fn(input)).toBe(output)` 描述时，创建 TDD 计划：
- RED：写失败测试 → commit
- GREEN：最小实现使测试通过 → commit
- REFACTOR：清理（如需）→ commit
- 单个功能一个 TDD 计划，产出 2-3 个原子提交

**跳过 TDD：** UI 布局、配置变更、胶水代码、一次性脚本、无业务逻辑的 CRUD

### 计划验证（如果 CONFIG.md 启用）

派生验证子代理检查（来自 GSD plan-checker 8 维度）：
1. 需求覆盖 — 每个 REQ-ID 至少在一个计划中
2. 任务完整性 — 具体到可直接实现
3. 依赖正确性 — 波次间依赖无环
4. 关键连接 — 组件间接线已规划
5. 范围合理 — 不超出阶段边界
6. 必须交付推导 — 从成功标准倒推
7. 上下文合规 — 尊重 CONTEXT.md 锁定决策
8. 验证合规 — 测试策略覆盖

有问题则发回规划师修订（最多 3 轮）。

---

## pm:execute — 波次并行执行

编排器保持精简（~10-15% 上下文），执行委托给子代理。

### 流程

1. **发现计划** — 列出阶段目录 PLAN 文件，跳过已有 SUMMARY 的
2. **波次分组** — 解析每个计划的 wave 元数据，同波次可并行
3. **逐波次执行：**
   - 描述将要构建什么（从计划目标提取，2-3 句，不是 "执行计划 XX"）
   - 派生执行子代理（见 `agents/pm-executor.md`）— 传递文件路径而非内容
   - 等待本波次全部完成
   - 抽检结果（SUMMARY 存在 + git commit 存在 + 无 Self-Check FAILED）
   - 报告波次成果（从 SUMMARY 提取，说明这使下一波次能做什么）
4. **全部波次完成 → 验证阶段目标**

### 执行子代理规则（来自 GSD executor）

**四条偏差处理规则：**
| 规则 | 情况 | 处理 |
|------|------|------|
| 规则 1 | 实现中遇到 bug | 自动修复，记录到总结 |
| 规则 2 | 缺少关键功能（导入/类型/验证） | 自动补充，记录到总结 |
| 规则 3 | 阻塞性问题（依赖/环境/配置） | 自动修复，记录到总结 |
| 规则 4 | 需要架构变更 | **CHECKPOINT — 询问用户** |

每个任务最多自动修复 3 次，之后 CHECKPOINT。

### 检查点协议（来自 GSD checkpoints.md）

**黄金法则：如果 Claude 能运行，Claude 就运行。** 检查点用于验证和决策，不是手动工作。

**三种类型：**
- **checkpoint:human-verify (90%)** — 自动化完成后，人确认视觉/功能正确。Claude 启动服务器后再呈现检查点，用户只需访问 URL 验证。
- **checkpoint:decision (9%)** — 人做架构/技术选择。提供选项+优缺点。
- **checkpoint:human-action (1%)** — 真正无法自动化的（邮件验证链接、2FA、OAuth 浏览器授权）。**不是**预先规划的手动工作 — 是 Claude 尝试自动化后遇到认证门。

**认证门模式：** Claude 尝试 CLI/API → 认证错误 → 创建 checkpoint:human-action → 用户认证 → Claude 重试 → 继续

**绝不：** 让用户运行 CLI 命令 | 在环境坏的时候呈现检查点 | 每个任务后都加检查点

### SUMMARY 文件格式

每个计划执行后创建 `[NN]-SUMMARY-[WW]-[PP].md`：
- 状态（完成/部分/阻塞）
- 任务表（名称/状态/备注）
- 关键文件（新建/修改）
- 做出的决策
- 偏差
- 自检（通过/失败）

---

## pm:verify — 目标倒推验证

**不只检查任务完成，而是验证目标达成。**（来自 GSD verifier 目标倒推分析）

### 三级验证（来自 GSD verification-patterns.md）

对每个成功标准执行：

**Level 1 — 存在：** 文件/函数/组件/路由在磁盘上存在

**Level 2 — 实质：** 不是空壳/TODO/占位符
- 空壳检测：grep `TODO|FIXME|placeholder|not implemented`
- 空实现：`return null|return {}|return []`
- 占位文本：`lorem ipsum|coming soon|sample data`
- 硬编码值：应该动态的地方用了固定值

**Level 3 — 连接：** 被系统其他部分使用
- 组件 → API：fetch/axios 调用存在且使用响应
- API → 数据库：查询存在且结果返回
- 表单 → 处理器：onSubmit 调用 API/mutation（不只是 preventDefault）
- 状态 → 渲染：状态变量出现在 JSX 中

### 需求交叉核对

映射到本阶段的每个 REQ-ID 必须有证据。

### VERIFY.md 格式

```markdown
# 验证：阶段 [N] — [名称]

## 状态: [通过 | 有缺口 | 需人工]
## 得分: [N]/[M]

## 成功标准验证
| # | 标准 | 存在 | 实质 | 连接 | 状态 |

## 需求覆盖
| REQ-ID | 描述 | 计划 | 证据 | 状态 |

## 缺口（如有）
### 缺口 N: [标题]
- 标准 / 缺失 / 影响 / 建议修复 / 工作量

## 需人工验证（如有）
- [ ] [需要手动测试的项目]
```

**缺口处理：** `pm:plan [N] --gaps` 读取 VERIFY.md → 创建缺口修复计划 → `pm:execute [N] --gaps-only`

---

## pm:debug — 科学方法调试

状态持久化到 `.project/debug/<slug>.md`，跨会话保持。

### 流程

1. **检查活跃会话** — 列出 debug/ 下非 resolved 文件，有则让用户选择恢复或新建
2. **收集症状** — 预期/实际/错误/时间线/复现步骤
3. **创建调试文件** — `.project/debug/<slug>.md`
4. **派生调试子代理**（见 `agents/pm-debugger.md`）
5. **科学方法循环：**
   - 形成假设 → 收集证据 → 确认/排除 → 下一假设
   - 每个假设记录到调试文件：证据/反证/判定
   - 最多 3 次修复尝试 → 之后 CHECKPOINT
6. **根因确认后** → 最小修复 → 更新调试文件 → 移到 `debug/resolved/`

---

## pm:resume — 恢复工作

1. 加载 STATE.md（缺失则从 ROADMAP/changes/phases 重建）
2. 检测未完成工作：变更部分实现 | 阶段计划未执行（PLAN 无 SUMMARY）| 活跃调试
3. 优先级路由：未完成变更 > 未执行计划 > 活跃调试
4. 快速恢复：用户说 "继续" → 跳过展示直接执行

---

## 状态管理

### 更新规则

每次完成有意义操作后**必须**更新：
1. STATE.md 活动日志新增行
2. STATE.md 会话连续性（last_session / stopped_at / next_action）
3. `.cache/index.md` 修改过的文件
4. `.cache/relations.md` 如果改了导入/导出
5. `.cache/decisions.md` 如果做了设计决策

### 状态转换

```
变更: proposed → implementing → complete → archived
阶段: pending → discussing → researching → planning → executing → verifying → complete
调试: investigating → hypothesis → fixing → resolved
```

---

## 子代理编排

编排器精简（~10-15% 上下文），子代理获得完整新窗口。传递文件路径而非内容。

| 代理 | 规范文件 | 用途 |
|------|---------|------|
| 研究员 | agents/pm-researcher.md | 领域研究（技术栈/架构/陷阱/集成） |
| 规划师 | agents/pm-planner.md | 创建 PLAN（波次/依赖/反浅层任务） |
| 执行者 | agents/pm-executor.md | 执行 PLAN（原子提交/4条偏差规则/检查点） |
| 验证员 | agents/pm-verifier.md | 目标倒推验证（三级检查/空壳检测/连接验证） |
| 调试员 | agents/pm-debugger.md | 科学方法调试（假设/证据/持久状态） |

**子代理返回标记：**
```
## PLANNING COMPLETE — N 个计划已创建
## PLAN COMPLETE — 所有任务已执行
## CHECKPOINT REACHED — 需要用户输入（附类型和详情）
## VERIFICATION PASSED — 目标已达成
## GAPS FOUND — 存在缺口（附缺口列表）
## ROOT CAUSE FOUND — 根因已确认
## RESEARCH COMPLETE — 关键发现摘要
## INVESTIGATION INCONCLUSIVE — 已检查内容列表
```

---

## 代码规范

- **不可变模式（关键）** — 创建新对象，永不修改入参
- **文件组织** — 200-400 行典型，800 行上限，高内聚低耦合
- **错误处理** — try/catch 所有 async，抛出用户友好信息
- **输入验证** — zod 或等价方案校验用户输入
- **安全** — 无硬编码密钥、参数化查询、HTML 转义、错误信息不泄露敏感数据
- **函数** — <50 行，嵌套 <4 层

## Git 规范

```
<type>(<scope>): <中文描述>
```
type: feat/fix/refactor/docs/test/chore | scope: phase-N/change-名称/project/debug/cache
每个任务一次原子提交，只 add 相关文件。

## 输出格式

阶段横幅：`━━━ PM ► [阶段名称] ━━━`
任务完成：`✓ 任务完成 → [做了什么] → 修改: [文件] → 下一步: [建议]`

## 常见错误

| 错误 | 正确做法 |
|------|---------|
| 不读上下文直接干活 | 先执行启动协议 |
| 每次搜索同一文件 | 查 `.cache/index.md` |
| 重复问已决定的事 | 查 `.cache/decisions.md` |
| 修改文件不查关联 | 查 `.cache/relations.md` 找同步修改 |
| 模糊任务描述 | tasks.md 中写具体值和验证条件 |
| 猜测遇到阻塞 | 暂停报告，等用户指导 |
| 大量修改不提交 | 每个任务原子提交 |
| 让用户跑 CLI | 自己跑，遇到认证门才 CHECKPOINT |
| 环境坏了还呈现检查点 | 先修好环境再呈现 |
| 接受模糊回答 | 挑战"好"/"简单"/"用户"这种词 |
| 空壳/TODO 当已实现 | 三级验证：存在 → 实质 → 连接 |
