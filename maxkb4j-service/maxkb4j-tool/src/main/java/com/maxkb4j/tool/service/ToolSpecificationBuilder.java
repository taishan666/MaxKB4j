package com.maxkb4j.tool.service;

import com.maxkb4j.tool.dto.ToolInputField;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.util.ToolNaming;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 工具规范（ToolSpecification）构建器，负责从工具实体和应用实体构建参数 schema
 */
@Component
@Slf4j
public class ToolSpecificationBuilder {

    /**
     * 根据工具实体构建 ToolSpecification
     */
    public ToolSpecification build(ToolEntity tool) {
        JsonObjectSchema.Builder parametersBuilder = JsonObjectSchema.builder();
        List<ToolInputField> params = Optional.ofNullable(tool.getInputFieldList()).orElse(Collections.emptyList());
        List<String> required = new ArrayList<>();
        for (ToolInputField param : params) {
            String type = param.getType();
            String name = param.getName();
            if (param.getIsRequired()) {
                required.add(name);
            }
            // 参数说明必须一并下发给模型，否则模型只能靠 name 猜格式
            // （例如 MongoDB 工具的 query 会被填成 db.xxx.find({...}) 这类 shell 语句）
            String desc = param.getDesc();
            String description = (desc == null || desc.isBlank()) ? null : desc;
            switch (type) {
                case "string" -> parametersBuilder.addStringProperty(name, description);
                case "int" -> parametersBuilder.addIntegerProperty(name, description);
                case "number" -> parametersBuilder.addNumberProperty(name, description);
                case "boolean" -> parametersBuilder.addBooleanProperty(name, description);
                case "array" ->
                        parametersBuilder.addProperty(name, JsonArraySchema.builder().description(description).build());
                case "object" ->
                        parametersBuilder.addProperty(name, JsonObjectSchema.builder().description(description).build());
                default -> log.warn("Unsupported parameter type: {} for field: {}", type, name);
            }
        }
        if (!required.isEmpty()) {
            parametersBuilder.required(required);
        }
        return ToolSpecification.builder()
                .name(ToolNaming.buildToolName(tool.getId()))
                .description("**" + tool.getName() + "**" + ":" + tool.getDesc())
                .parameters(parametersBuilder.build())
                .build();
    }
}
