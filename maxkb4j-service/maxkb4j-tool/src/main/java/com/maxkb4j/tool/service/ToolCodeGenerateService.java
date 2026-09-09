package com.maxkb4j.tool.service;

import com.alibaba.fastjson.JSONArray;
import com.maxkb4j.common.domain.dto.MessageDTO;
import com.maxkb4j.core.langchain4j.ChatStreamGenerator;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.tool.dto.GenerateCodeDTO;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 工具代码生成（流式）逻辑，从 ToolController 抽离。
 * 消息转换与流式桥接复用 {@link ChatStreamGenerator}。
 *
 * @author tarzan
 */
@RequiredArgsConstructor
@Service
public class ToolCodeGenerateService {

    private final IModelProviderService modelFactory;

    public Flux<MessageDTO> generateCode(GenerateCodeDTO dto) {
        StreamingChatModel chatModel = modelFactory.buildStreamingChatModel(dto.getModelId(), dto.getModelParamsSetting());
        return ChatStreamGenerator.generate(chatModel, dto.getMessages(), userInput -> dto.getPrompt()
                .replace("{userInput}", userInput)
                .replace("{initFieldList}", toJsonArray(dto.getInitFieldList()))
                .replace("{inputFieldList}", toJsonArray(dto.getInputFieldList())));
    }

    private String toJsonArray(JSONArray array) {
        return array == null ? "[]" : array.toJSONString();
    }
}
