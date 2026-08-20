package com.maxkb4j.application.service;

import com.maxkb4j.application.dto.PromptGenerateDTO;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.service.impl.ApplicationServiceImpl;
import com.maxkb4j.model.service.IModelProviderService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 应用 Prompt 生成（流式）逻辑，从 {@link ApplicationServiceImpl} 抽离。
 *
 * @author tarzan
 */
@RequiredArgsConstructor
@Service
public class ApplicationPromptService {

    private final IModelProviderService modelFactory;

    public Flux<Map<String, String>> promptGenerate(ApplicationEntity app, String modelId, PromptGenerateDTO dto) {
        StreamingChatModel chatModel = modelFactory.buildStreamingChatModel(modelId);
        List<ChatMessage> messages = dto.getMessages().stream()
                .map(message -> {
                    if ("user".equals(message.getRole())) {
                        return UserMessage.from(message.getContent());
                    } else if ("ai".equals(message.getRole())) {
                        return AiMessage.from(message.getContent());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
        if (messages.isEmpty()) {
            return Flux.error(new IllegalArgumentException("No user message found to generate prompt"));
        }
        String prompt = dto.getPrompt();
        String detail = StringUtils.isBlank(app.getDesc()) ? app.getName() : app.getDesc();
        prompt = prompt.replace("{application_name}", app.getName())
                .replace("{detail}", detail)
                // 注意：messages 是过滤后的列表，取"最后一条输入"必须用原始列表自身的 size，
                // 否则存在 system 等角色时 {userInput} 会被错误替换为中间某条消息
                .replace("{userInput}", dto.getMessages().get(dto.getMessages().size() - 1).getContent());
        List<ChatMessage> finalMessages = new ArrayList<>(messages);
        finalMessages.set(finalMessages.size() - 1, UserMessage.from(prompt));
        Sinks.Many<Map<String, String>> sink = Sinks.many().unicast().onBackpressureBuffer();
        chatModel.chat(finalMessages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                sink.tryEmitNext(Map.of("content", partialResponse));
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
}
