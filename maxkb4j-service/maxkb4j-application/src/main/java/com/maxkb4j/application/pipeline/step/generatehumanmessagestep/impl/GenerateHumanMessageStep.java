package com.maxkb4j.application.pipeline.step.generatehumanmessagestep.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.application.pipeline.step.generatehumanmessagestep.AbsGenerateHumanMessageStep;
import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.common.mp.entity.LlmModelSetting;
import com.maxkb4j.knowledge.util.RagContentInjector;
import com.maxkb4j.knowledge.vo.ParagraphVO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GenerateHumanMessageStep extends AbsGenerateHumanMessageStep {

    public static final RagContentInjector contentInjector = new RagContentInjector();
    @Override
    protected String execute(LlmModelSetting llmModelSetting, KnowledgeSetting knowledgeSetting, String problemText, List<ParagraphVO> paragraphList) {
        String safeProblemText = problemText != null ? problemText : "";
        int maxCharNumber = knowledgeSetting.getMaxParagraphCharNumber();
        if (!CollectionUtils.isEmpty(paragraphList)) {
           return contentInjector.inject(paragraphList, safeProblemText,maxCharNumber);
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
