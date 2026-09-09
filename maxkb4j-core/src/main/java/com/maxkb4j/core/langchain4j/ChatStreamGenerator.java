package com.maxkb4j.core.langchain4j;

import com.maxkb4j.common.domain.dto.MessageDTO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 流式生成公共桥接：将「历史对话 + 基于最后一条用户输入构建的提示词」交给
 * {@link StreamingChatModel}，并把流式响应包装为 {@code Flux<MessageDTO>} 输出。
 * 应用 Prompt 生成、工具代码生成等场景复用，调用方只需提供各自的提示词模板逻辑。
 *
 * @author tarzan
 */
public final class ChatStreamGenerator {

    private ChatStreamGenerator() {
    }

    /**
     * @param chatModel     流式对话模型
     * @param history       原始对话历史（user/ai 角色）
     * @param promptBuilder 入参为最后一条消息内容，返回最终替换完占位符的用户提示词
     */
    public static Flux<MessageDTO> generate(StreamingChatModel chatModel, List<MessageDTO> history, Function<String, String> promptBuilder) {
        List<ChatMessage> messages = toChatMessages(history);
        if (messages.isEmpty()) {
            return Flux.error(new IllegalArgumentException("No user message found"));
        }
        String prompt = promptBuilder.apply(history.getLast().getContent());
        messages.set(messages.size() - 1, UserMessage.from(prompt));
        Sinks.Many<MessageDTO> sink = Sinks.many().unicast().onBackpressureBuffer();
        chatModel.chat(messages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                sink.tryEmitNext(new MessageDTO(partialResponse, "ai"));
            }

            @Override
            public void onCompleteResponse(ChatResponse chatResponse) {
                sink.tryEmitComplete();
            }

            @Override
            public void onError(Throwable throwable) {
                sink.tryEmitError(throwable);
            }
        });
        return sink.asFlux();
    }

    private static List<ChatMessage> toChatMessages(List<MessageDTO> history) {
        if (history == null || history.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(history.stream()
                .map(message -> {
                    if ("user".equals(message.getRole())) {
                        return UserMessage.from(message.getContent());
                    } else if ("ai".equals(message.getRole())) {
                        return AiMessage.from(message.getContent());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList());
    }
}
