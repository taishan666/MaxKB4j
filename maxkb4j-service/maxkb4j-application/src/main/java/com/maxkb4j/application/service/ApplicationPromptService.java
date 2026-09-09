package com.maxkb4j.application.service;

import com.maxkb4j.application.dto.PromptGenerateDTO;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.service.impl.ApplicationServiceImpl;
import com.maxkb4j.common.domain.dto.MessageDTO;
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

    public Flux<MessageDTO> promptGenerate(ApplicationEntity app, String modelId, PromptGenerateDTO dto) {
        StreamingChatModel chatModel = modelFactory.buildStreamingChatModel(modelId);
        List<ChatMessage> messages = new ArrayList<>(dto.getMessages().stream()
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
        if (messages.isEmpty()) {
            return Flux.error(new IllegalArgumentException("No user message found to generate prompt"));
        }
        String prompt = dto.getPrompt();
        String detail = StringUtils.isBlank(app.getDesc()) ? app.getName() : app.getDesc();
        prompt = prompt.replace("{application_name}", app.getName())
                .replace("{detail}", detail)
                .replace("{userInput}", dto.getMessages().getLast().getContent());
        messages.set(messages.size() - 1, UserMessage.from(prompt));
        Sinks.Many<MessageDTO> sink = Sinks.many().unicast().onBackpressureBuffer();
        chatModel.chat(messages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                sink.tryEmitNext(new MessageDTO(partialResponse,"ai"));
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
