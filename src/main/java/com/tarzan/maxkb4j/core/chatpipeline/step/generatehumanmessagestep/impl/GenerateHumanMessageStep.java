package com.tarzan.maxkb4j.core.chatpipeline.step.generatehumanmessagestep.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.core.chatpipeline.step.generatehumanmessagestep.IGenerateHumanMessageStep;
import com.tarzan.maxkb4j.module.application.domian.entity.KnowledgeSetting;
import com.tarzan.maxkb4j.module.application.domian.entity.LlmModelSetting;
import com.tarzan.maxkb4j.module.application.enums.AIAnswerType;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GenerateHumanMessageStep extends IGenerateHumanMessageStep {
    @Override
    protected String execute(LlmModelSetting llmModelSetting , KnowledgeSetting knowledgeSetting, String problemText, List<ParagraphVO> paragraphList) {
        if (!CollectionUtils.isEmpty(paragraphList)){
            String prompt = llmModelSetting.getPrompt();
            StringBuilder data=new StringBuilder();
            for (ParagraphVO paragraphVO : paragraphList) {
                data.append("<data>").append(paragraphVO.getTitle()).append(":").append(paragraphVO.getContent()).append("</data>");
            }
            prompt=prompt.replace("{question}", problemText).replace("{data}", data);
            return prompt;
        }else {
            String status=knowledgeSetting.getNoReferencesSetting().getStatus();
            if (AIAnswerType.ai_questioning.name().equals(status)){
                String noReferencesPrompt = llmModelSetting.getNoReferencesPrompt();
                return noReferencesPrompt.replace("{question}", problemText);
            }
        }
        return problemText;
    }

    @Override
    public JSONObject getDetails() {
        return null;
    }


}
