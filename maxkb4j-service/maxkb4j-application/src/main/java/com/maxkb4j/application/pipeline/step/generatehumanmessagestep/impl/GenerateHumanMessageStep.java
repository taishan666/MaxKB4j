package com.maxkb4j.application.pipeline.step.generatehumanmessagestep.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.application.pipeline.PipelineManage;
import com.maxkb4j.application.pipeline.step.generatehumanmessagestep.AbsGenerateHumanMessageStep;
import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.application.dto.LlmModelSetting;
import com.maxkb4j.core.support.RagContentInjector;
import com.maxkb4j.knowledge.vo.ParagraphRagVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GenerateHumanMessageStep extends AbsGenerateHumanMessageStep {

    public static final RagContentInjector contentInjector = new RagContentInjector();

    /**
     * 引用上限缺省值，与前端应用默认配置（maxParagraphCharNumber=5000）保持一致；
     * MK 模板导入 / API 直建 / 旧数据可能缺失该键。
     */
    private static final int DEFAULT_MAX_PARAGRAPH_CHAR_NUMBER = 5000;

    @Override
    protected String execute(LlmModelSetting llmModelSetting, KnowledgeSetting knowledgeSetting, String problemText, List<ParagraphRagVO> paragraphList) {
        String safeProblemText = problemText != null ? problemText : "";
        if (CollectionUtils.isEmpty(paragraphList)) {
            return safeProblemText;
        }
        int maxCharNumber = Optional.ofNullable(knowledgeSetting)
                .map(KnowledgeSetting::getMaxParagraphCharNumber)
                .orElse(DEFAULT_MAX_PARAGRAPH_CHAR_NUMBER);
        return contentInjector.inject(paragraphList, safeProblemText, maxCharNumber);
    }

    @Override
    public JSONObject getDetails(PipelineManage manage) {
        Map<String, Object> context = stepContext(manage);
        JSONObject details = new JSONObject(true);
        details.put("step_type", "generate_human_message");
        details.put("messageTokens", context.getOrDefault("messageTokens", 0));
        details.put("answerTokens", context.getOrDefault("answerTokens", 0));
        return details;
    }

}
