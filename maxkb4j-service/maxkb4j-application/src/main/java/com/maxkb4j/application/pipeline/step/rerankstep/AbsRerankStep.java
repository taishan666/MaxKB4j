package com.maxkb4j.application.pipeline.step.rerankstep;

import com.maxkb4j.application.pipeline.AbsStep;
import com.maxkb4j.application.pipeline.PipelineManage;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.knowledge.vo.ParagraphRagVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;

/**
 * 检索结果重排步骤：位于 {@code SearchDatasetStep} 之后、
 * {@code GenerateHumanMessageStep} 之前，对召回段落用 ScoringModel 精排。
 *
 * <p>未启用（{@code rerankEnable} 未开启或缺重排模型）时为空操作，管线行为不变。</p>
 */
public abstract class AbsRerankStep extends AbsStep {

    @Override
    @SuppressWarnings("unchecked")
    protected void _run(PipelineManage manage) {
        ApplicationVO application = manage.application;
        KnowledgeSetting setting = Optional.ofNullable(application.getKnowledgeSetting())
                .orElseGet(KnowledgeSetting::new);
        List<ParagraphRagVO> paragraphList = (List<ParagraphRagVO>) manage.context.get("paragraphList");
        if (CollectionUtils.isEmpty(paragraphList)
                || !Boolean.TRUE.equals(setting.getRerankEnable())
                || StringUtils.isBlank(setting.getRerankModelId())) {
            return;
        }
        List<ParagraphRagVO> reranked = execute(manage.chatParams.getMessage(), setting, paragraphList);
        manage.context.put("paragraphList", reranked);
        context.put("paragraphList", reranked);
    }

    protected abstract List<ParagraphRagVO> execute(String question, KnowledgeSetting setting, List<ParagraphRagVO> paragraphList);
}
