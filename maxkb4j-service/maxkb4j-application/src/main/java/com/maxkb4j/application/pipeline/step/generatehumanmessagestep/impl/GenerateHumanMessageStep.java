package com.maxkb4j.application.pipeline.step.generatehumanmessagestep.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.application.pipeline.step.generatehumanmessagestep.AbsGenerateHumanMessageStep;
import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.common.mp.entity.LlmModelSetting;
import com.maxkb4j.knowledge.vo.ParagraphVO;
import dev.langchain4j.model.input.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GenerateHumanMessageStep extends AbsGenerateHumanMessageStep {

    public static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from("{{userMessage}}\n\nAnswer using the following information:\n{{contents}}");
    @Override
    protected String execute(LlmModelSetting llmModelSetting, KnowledgeSetting knowledgeSetting, String problemText, List<ParagraphVO> paragraphList) {
        String safeProblemText = problemText != null ? problemText : "";
        if (!CollectionUtils.isEmpty(paragraphList)) {
            List<String> contents=new ArrayList<>();
            for (ParagraphVO paragraphVO : paragraphList) {
                String title = paragraphVO.getTitle() != null ? paragraphVO.getTitle() : "";
                String content = paragraphVO.getContent() != null ? paragraphVO.getContent() : "";
                contents.add(String.format("content: %s\n%s", title, content));
            }
            Map<String, Object> variables = new HashMap<>();
            variables.put("userMessage", problemText);
            variables.put("contents", String.join("\n", contents));
           return DEFAULT_PROMPT_TEMPLATE.apply( variables).text();
        }
        return safeProblemText;
    }

    @Override
    public JSONObject getDetails() {
        JSONObject details = new JSONObject(true);
        details.put("step_type", "generate_human_message");
        details.put("messageTokens", context.getOrDefault("messageTokens", 0));
        details.put("answerTokens", context.getOrDefault("answerTokens", 0));
        return details;
    }

}
