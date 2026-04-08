package com.maxkb4j.application.pipeline.step.generatehumanmessagestep.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.common.mp.entity.LlmModelSetting;
import com.maxkb4j.application.enums.AIAnswerType;
import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.application.pipeline.step.generatehumanmessagestep.AbsGenerateHumanMessageStep;
import com.maxkb4j.knowledge.vo.ParagraphVO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GenerateHumanMessageStep extends AbsGenerateHumanMessageStep {
    @Override
    protected String execute(LlmModelSetting llmModelSetting, KnowledgeSetting knowledgeSetting, String problemText, List<ParagraphVO> paragraphList) {
        String safeProblemText = problemText != null ? problemText : "";
        if (!CollectionUtils.isEmpty(paragraphList)) {
            String prompt = llmModelSetting.getPrompt();
            if (prompt != null) {
                StringBuilder data = new StringBuilder();
                for (ParagraphVO paragraphVO : paragraphList) {
                    String title = paragraphVO.getTitle() != null ? paragraphVO.getTitle() : "";
                    String content = paragraphVO.getContent() != null ? paragraphVO.getContent() : "";
                    data.append("<data>").append(title).append(":").append(content).append("</data>");
                }
                prompt = prompt.replace("{question}", safeProblemText).replace("{data}", data);
                return prompt;
            }
        } else {
            String status = knowledgeSetting.getNoReferencesSetting().getStatus();
            if (AIAnswerType.ai_questioning.name().equals(status)) {
                String noReferencesPrompt = llmModelSetting.getNoReferencesPrompt();
                if (noReferencesPrompt != null) {
                    return noReferencesPrompt.replace("{question}", safeProblemText);
                }
            }
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
