package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.application.dto.ApplicationChatDTO;
import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.application.dto.ShareChatDTO;
import com.maxkb4j.application.vo.ShareChatVO;
import com.maxkb4j.common.domain.dto.ChatInfo;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.domain.vo.ChatMessageVO;
import com.maxkb4j.common.domain.vo.ResultCallback;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface IApplicationChatService {

    String chatOpen(String appId, boolean debug);

    String chatOpen(String appId, boolean debug, String chatUserId);

    /**
     * 以调用方指定的 chatId 获取或创建会话，供子应用场景使用（AgentExecutor / 应用节点）：
     * 其会话 id 由父会话派生，需跨请求保持稳定，因此不能走 {@link #chatOpen} 的随机 id。
     * <p>缓存或库中已存在则复用（库中存在时回填历史记录）；均不存在时新建并写入缓存；
     * 命中会话但应用/用户归属不匹配时抛出异常。</p>
     */
    ChatInfo getOrCreateChatInfo(String chatId, String appId, String chatUserId);

    ChatResponse chatMessage(ChatParams chatParams, ChatState chatState, ResultCallback<ChatMessageVO> callback);

    /**
     * 异步执行对话，返回该任务的 Future；调用方可在客户端断连时 cancel(true)
     * 中断底层执行线程。正常/异常收尾仍通过 callback 的 onComplete/onError 传递。
     */
    CompletableFuture<Void> chatMessageAsync(ChatParams chatParams, ChatState chatState, ResultCallback<ChatMessageVO> callback);

    Boolean deleteById(String chatId);

    IPage<ApplicationChatDTO> page(String appId, String userId, int current, int size);

    boolean clear(String appId, String userId);

    boolean updateDtoById(ApplicationChatDTO applicationChatDTO);

    /**
     * 判断会话是否属于指定应用下的指定用户（用于防止越权访问他人会话）
     */
    boolean isOwnedBy(String chatId, String appId, String userId);


    Map<String, String> shareChat(String id, String chatId, ShareChatDTO dto);

    ShareChatVO shareChat(String id);
}
