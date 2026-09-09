package com.maxkb4j.application.service;

import com.maxkb4j.application.dto.PromptGenerateDTO;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.common.domain.dto.MessageDTO;
import com.maxkb4j.core.langchain4j.ChatStreamGenerator;
import com.maxkb4j.model.service.IModelProviderService;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 应用 Prompt 生成（流式）逻辑，从 ApplicationServiceImpl 抽离。
 *
 * @author tarzan
 */
@RequiredArgsConstructor
@Service
public class ApplicationPromptService {

    private final IModelProviderService modelFactory;

    public Flux<MessageDTO> promptGenerate(ApplicationEntity app, String modelId, PromptGenerateDTO dto) {
        StreamingChatModel chatModel = modelFactory.buildStreamingChatModel(modelId);
        String detail = StringUtils.isBlank(app.getDesc()) ? app.getName() : app.getDesc();
        return ChatStreamGenerator.generate(chatModel, dto.getMessages(), userInput -> dto.getPrompt()
                .replace("{application_name}", app.getName())
                .replace("{detail}", detail)
                .replace("{userInput}", userInput));
    }
}
