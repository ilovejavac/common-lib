package com.dev.lib.harness.session;

import com.dev.lib.harness.session.model.SpawnArgs;
import com.dev.lib.harness.session.model.Submission;
import com.dev.lib.util.Dispatcher;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class AgentSession {

    private String id;

    private volatile SessionStatus status;

    private final Object mutex = new Object();

    // pending user messages
    private final BlockingQueue<Submission> channel;
    private Future<?> loop_handler;
    // agent

    // subscribe

    AgentSession(
            SpawnArgs args
    ) {
        status = SessionStatus.IDLE;
        channel = new LinkedBlockingQueue<>();
        spawn(args);
    }

    private void spawn(SpawnArgs args) {
        // load skills
        loop_handler = Dispatcher.IO.submit(this::loop);
    }

    // sender
    public void submit(Submission submission) {
        try {
            channel.put(submission);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Agent session loop interrupted: {}", id);
        } catch (Exception e) {
            log.error("Agent session loop error: {}", id, e);
        }
    }

    // receiver
    protected void loop() {
        try {
            while (status != SessionStatus.CLOSED) {
                Submission smss = channel.take();

                boolean should_exist = false;

                /**
                 * 这些 Op 大致可以分 8 类：
                 *
                 *   1. 会话控制类
                 *   2. turn / 用户输入类
                 *   3. 审批与交互回流类
                 *   4. skill / prompt / MCP 查询类
                 *   5. 历史与线程管理类
                 *   6. 记忆管理类
                 *   7. shell / tool 辅助类
                 *   8. review / realtime 类
                 *
                 *   ———
                 *
                 *   ## 1. 会话控制类
                 *
                 *   ### Op::Interrupt
                 *
                 *   中断当前正在执行的 turn/task。
                 *
                 *   作用：
                 *
                 *   - 取消当前任务
                 *   - 让这一轮尽快停止
                 *   - 最终一般会出现 TurnAborted
                 *
                 *   ### Op::CleanBackgroundTerminals
                 *
                 *   清理后台仍在运行的 terminal 进程。
                 *
                 *   作用：
                 *
                 *   - 杀掉后台 shell / unified exec 进程
                 *   - 不只是“停止当前模型”，而是清后台执行环境
                 *
                 *   ### Op::Shutdown
                 *
                 *   关闭这个 session。
                 *
                 *   作用：
                 *
                 *   - 停止 session loop
                 *   - 清理资源
                 *   - 最终发 ShutdownComplete
                 *
                 *   ———
                 *
                 *   ## 2. turn / 用户输入类
                 *
                 *   ### Op::UserInput
                 *
                 *   提交用户输入。
                 *
                 *   这是“基础版输入操作”，只带输入内容，例如：
                 *
                 *   - 文本
                 *   - 图片
                 *   - skill item 等
                 *
                 *   通常 app-server 走这个。
                 *
                 *   ### Op::UserTurn
                 *
                 *   提交“一轮完整用户请求”。
                 *
                 *   它比 UserInput 多了 turn 级上下文：
                 *
                 *   - cwd
                 *   - approval_policy
                 *   - sandbox_policy
                 *   - model
                 *   - effort
                 *   - collaboration_mode
                 *   - personality
                 *
                 *   通常当前 TUI 主流程走这个。
                 *
                 *   作用：
                 *
                 *   - 创建新一轮 turn
                 *   - 更新这轮使用的上下文
                 *   - 启动 run_turn
                 *
                 *   ### Op::OverrideTurnContext
                 *
                 *   只修改 turn 默认上下文，不直接提交用户消息。
                 *
                 *   比如只改：
                 *
                 *   - cwd
                 *   - model
                 *   - sandbox
                 *   - approval
                 *   - personality
                 *
                 *   作用：
                 *
                 *   - 更新 session 后续 turn 的默认配置
                 *   - 不会直接触发 run_turn
                 *
                 *   ———
                 *
                 *   ## 3. 审批与交互回流类
                 *
                 *   这些都属于“模型/工具向用户发起请求后，用户再回一个结果”。
                 *
                 *   ### Op::ExecApproval
                 *
                 *   用户对命令执行审批的回应。
                 *
                 *   比如：
                 *
                 *   - 批准
                 *   - 拒绝
                 *   - 中止
                 *
                 *   ### Op::PatchApproval
                 *
                 *   用户对补丁/apply_patch 的审批回应。
                 *
                 *   ### Op::UserInputAnswer
                 *
                 *   用户对 request_user_input 工具调用的回应。
                 *
                 *   也就是模型问用户一个问题，用户回填答案。
                 *
                 *   ### Op::RequestPermissionsResponse
                 *
                 *   用户对权限请求的回应。
                 *
                 *   比如允许额外文件权限或网络权限。
                 *
                 *   ### Op::DynamicToolResponse
                 *
                 *   外部系统对 dynamic tool call 的回应。
                 *
                 *   比如 core 发出一个 dynamic tool 请求，外部插件/UI/系统把执行结果再发回来。
                 *
                 *   ### Op::ResolveElicitation
                 *
                 *   处理 MCP server 发起的 elicitation 交互请求。
                 *
                 *   本质也是“外部请求用户 -> 用户回答 -> 再回给运行时”。
                 *
                 *   ———
                 *
                 *   ## 4. skill / prompt / MCP 查询类
                 *
                 *   这些主要是“列出/查询/刷新元数据”，不是直接跑一轮主任务。
                 *
                 *   ### Op::ListMcpTools
                 *
                 *   列出当前可用的 MCP tools / resources。
                 *
                 *   ### Op::RefreshMcpServers
                 *
                 *   刷新 MCP server 配置。
                 *
                 *   ### Op::ReloadUserConfig
                 *
                 *   重新加载用户配置。
                 *
                 *   ### Op::ListCustomPrompts
                 *
                 *   列出自定义 prompts。
                 *
                 *   ### Op::ListSkills
                 *
                 *   列出 skills。
                 *
                 *   ### Op::ListRemoteSkills
                 *
                 *   列出远程 skills。
                 *
                 *   ### Op::DownloadRemoteSkill
                 *
                 *   下载远程 skill 到本地。
                 *
                 *   这些 op 的共同特点：
                 *
                 *   - 不一定创建 turn
                 *   - 更像系统查询命令
                 *   - 一般执行完会回一个响应事件
                 *
                 *   ———
                 *
                 *   ## 5. 历史与线程管理类
                 *
                 *   ### Op::AddToHistory
                 *
                 *   把一条内容加到历史记录里。
                 *
                 *   通常用于输入历史/命令历史等持久化，而不是 turn 执行主链路。
                 *
                 *   ### Op::GetHistoryEntryRequest
                 *
                 *   查询历史记录项。
                 *
                 *   ### Op::Undo
                 *
                 *   撤销上一个操作/turn 相关结果。
                 *
                 *   ### Op::Compact
                 *
                 *   触发上下文压缩。
                 *
                 *   也就是历史太长时做 compact。
                 *
                 *   ### Op::ThreadRollback { num_turns }
                 *
                 *   回滚线程里最近若干 turn。
                 *
                 *   ### Op::SetThreadName
                 *
                 *   设置线程名。
                 *
                 *   这些都是围绕 thread/session 生命周期做管理。
                 *
                 *   ———
                 *
                 *   ## 6. 记忆管理类
                 *
                 *   ### Op::DropMemories
                 *
                 *   删除记忆。
                 *
                 *   ### Op::UpdateMemories
                 *
                 *   更新记忆。
                 *
                 *   这些和 memory subsystem 相关，不是普通对话 turn 的主路径。
                 *
                 *   ———
                 *
                 *   ## 7. shell / tool 辅助类
                 *
                 *   ### Op::RunUserShellCommand
                 *
                 *   执行用户主动发起的 shell 命令。
                 *
                 *   注意这个和“模型决定调用 shell tool”不同。
                 *
                 *   它更像用户显式要求运行一个命令，然后系统帮你跑。
                 *
                 *   ———
                 *
                 *   ## 8. review / realtime 类
                 *
                 *   ### Op::Review { review_request }
                 *
                 *   启动 review 模式/代码审查任务。
                 *
                 *   这通常会进入专门的 review task，而不是普通 regular task。
                 *
                 *   ### Op::RealtimeConversationStart
                 *
                 *   开启 realtime conversation。
                 *
                 *   ### Op::RealtimeConversationAudio
                 *
                 *   往 realtime 会话里送音频帧。
                 *
                 *   ### Op::RealtimeConversationText
                 *
                 *   往 realtime 会话里送文本。
                 *
                 *   ### Op::RealtimeConversationClose
                 *
                 *   关闭 realtime 会话。
                 *
                 *   这几类属于另一条实时交互链路，不是普通 run_turn 文本任务主链。
                 */

                if (should_exist) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Agent session loop interrupted: {}", id);
        } catch (Exception e) {
            log.error("Agent session loop error: {}", id, e);
        }
    }

    public void destroy() {
        markClosed();
        if (loop_handler != null) {
            loop_handler.cancel(true);
        }
    }

    void markRunningIfIdle() {
        status = SessionStatus.RUNNING;
    }

    void markClosed() {
        status = SessionStatus.CLOSED;
    }
}
