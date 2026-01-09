⏺ Read Tool

用途：读取本地文件内容

参数：
- file_path（必需）：文件的绝对路径
- offset（可选）：从第几行开始读
- limit（可选）：读取多少行

特点：
- 默认读取前 2000 行
- 支持图片（PNG、JPG 等）、PDF、Jupyter notebook
- 输出带行号（cat -n 格式）
- 超过 2000 字符的行会被截断

Write Tool

用途：创建或覆盖文件

参数：
- file_path（必需）：绝对路径
- content（必需）：写入的内容

特点：
- 会覆盖已存在的文件
- 写已存在的文件前必须先 Read
- 优先用 Edit 而非 Write（除非新建文件）

我的使用方式

1. 读文件前不猜测内容，先 Read
2. 修改已有文件用 Edit（精确替换），不用 Write 全量覆盖
3. 只在必须新建文件时用 Write
4. 大文件用 offset/limit 分段读取

Edit Tool

用途：精确字符串替换

参数：
- file_path（必需）：绝对路径
- old_string（必需）：要替换的原文本
- new_string（必需）：替换后的文本
- replace_all（可选，默认 false）：是否替换所有匹配项

约束
- 使用前必须先 Read 过该文件
- old_string 必须在文件中唯一，否则失败
- 如果不唯一，要么扩大上下文使其唯一，要么用 replace_all: true
- 必须保留原文件的缩进格式（tab/空格）

典型用法

1. 修改单处代码：提供足够上下文让 old_string 唯一
2. 批量重命名变量：replace_all: true
3. 删除代码：new_string 设为空字符串

⏺ 1. Task Tool

用途：启动子代理处理复杂多步骤任务

参数：
- prompt（必需）：任务描述
- description（必需）：3-5 词简述
- subagent_type（必需）：代理类型
    - Bash：命令执行
    - general-purpose：通用任务
    - Explore：代码库探索
    - Plan：架构设计
    - claude-code-guide：使用指南
- model（可选）：sonnet/opus/haiku
- run_in_background（可选）：后台运行
- resume（可选）：恢复之前的代理

  ---
2. TaskOutput Tool

用途：获取后台任务的输出

参数：
- task_id（必需）：任务 ID
- block（默认 true）：是否等待完成
- timeout（默认 30000ms）：最大等待时间

  ---
3. Bash Tool

用途：执行 shell 命令

参数：
- command（必需）：要执行的命令
- description（可选）：命令描述
- timeout（可选）：超时时间，最大 600000ms
- run_in_background（可选）：后台运行
- dangerouslyDisableSandbox（可选）：禁用沙箱

约束：
- 不用于文件操作（用专用工具）
- 路径有空格要加引号
- 避免交互式命令（如 git rebase -i）

  ---
4. Glob Tool

用途：按模式匹配文件名

参数：
- pattern（必需）：glob 模式，如 **/*.js
- path（可选）：搜索目录

  ---
5. Grep Tool

用途：搜索文件内容（基于 ripgrep）

参数：
- pattern（必需）：正则表达式
- path（可选）：搜索路径
- glob（可选）：文件过滤，如 *.ts
- type（可选）：文件类型，如 js、py
- output_mode（可选）：content/files_with_matches/count
- -A/-B/-C（可选）：上下文行数
- -i（可选）：忽略大小写
- -n（可选）：显示行号
- multiline（可选）：多行匹配
- head_limit/offset（可选）：分页

  ---
6. Read Tool

（已说明）

  ---
7. Edit Tool

（已说明）

  ---
8. Write Tool

（已说明）

  ---
9. NotebookEdit Tool

用途：编辑 Jupyter notebook 单元格

参数：
- notebook_path（必需）：绝对路径
- new_source（必需）：新内容
- cell_id（可选）：单元格 ID
- cell_type（可选）：code/markdown
- edit_mode（可选）：replace/insert/delete

  ---
10. WebFetch Tool

用途：获取网页内容并用 AI 处理

参数：
- url（必需）：完整 URL
- prompt（必需）：提取什么信息

特点：
- HTML 转 markdown
- 15 分钟缓存
- HTTP 自动升级 HTTPS

  ---
11. WebSearch Tool

用途：网页搜索

参数：
- query（必需）：搜索词
- allowed_domains（可选）：限定域名
- blocked_domains（可选）：排除域名

  ---
12. TodoWrite Tool

用途：任务列表管理

参数：
- todos（必需）：任务数组，每项包含：
    - content：任务描述（祈使句）
    - activeForm：进行时描述
    - status：pending/in_progress/completed

  ---
13. AskUserQuestion Tool

用途：向用户提问

参数：
- questions（必需）：1-4 个问题，每个包含：
    - question：问题内容
    - header：短标签（≤12 字符）
    - options：2-4 个选项
    - multiSelect：是否多选

  ---
14. Skill Tool

用途：调用预定义技能

参数：
- skill（必需）：技能名，如 commit、review-pr
- args（可选）：参数