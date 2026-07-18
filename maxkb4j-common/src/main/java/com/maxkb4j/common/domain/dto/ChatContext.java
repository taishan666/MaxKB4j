package com.maxkb4j.common.domain.dto;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.maxkb4j.common.enums.ChatSource;
import com.maxkb4j.common.enums.ChatUserType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 对话执行上下文：服务端在请求处理过程中解析/加载的运行时状态。
 * <p>
 * 与 {@link ChatParams}（纯请求入参，由 {@code @RequestBody} 绑定）相对，本类承载的是
 * 服务端解析出的身份信息（chatUserId/chatUserType/source/ipAddress/debug/appId）以及
 * 按 chatId 从存储加载的历史记录（historyChatRecords/chatRecord）。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>不会被 Jackson 反序列化——仅由服务端构造，沿调用链显式传递。</li>
 *   <li>两阶段装配：入口点（controller / 定时任务 / 工具执行）填充身份字段，
 *       {@code ApplicationChatService} 随后补全历史记录，因此保留 setter。</li>
 *   <li>非 ThreadLocal——含两阶段装配、异步线程（{@code chatTaskExecutor}）及嵌套子调用
 *       （{@code ApplicationNodeHandler}/{@code AgentExecutor}），需作为显式参数传递。</li>
 *   <li>与 {@code com.maxkb4j.common.context.UserContext}（请求线程管理员身份的 ThreadLocal 抽象）
 *       关注点不同：此处的 chatUser 通常是匿名/API-key/触发器用户，与管理员登录身份无关。</li>
 * </ul>
 *
 * @author tarzan
 */
@Builder
@Data
@Schema(description = "对话执行上下文（服务端构建，非请求入参）")
public class ChatContext {

    @Schema(description = "应用id")
    private String appId;
    @Schema(description = "聊天用户id")
    private String chatUserId;
    @Schema(description = "聊天用户类型")
    private String chatUserType;
    @Schema(description = "对话来源")
    private ChatSource source;
    @Schema(description = "客户端ip地址")
    private String ipAddress;
    @Schema(description = "是否调试模式")
    private Boolean debug;
    @Schema(description = "历史聊天记录（按 chatId 加载）")
    private List<ChatRecordDTO> historyChatRecords;
    @Schema(description = "当前对话记录（重新回答时定位）")
    private ChatRecordDTO chatRecord;

    public String getChatUserId() {
        return StringUtils.isBlank(chatUserId) ? IdWorker.get32UUID() : chatUserId;
    }

    public String getChatUserType() {
        return StringUtils.isBlank(chatUserType) ? ChatUserType.ANONYMOUS_USER.name() : chatUserType;
    }
}
