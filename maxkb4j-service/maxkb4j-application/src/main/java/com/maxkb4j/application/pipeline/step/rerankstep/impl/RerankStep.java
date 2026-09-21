package com.maxkb4j.application.pipeline.step.rerankstep.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.application.pipeline.step.rerankstep.AbsRerankStep;
import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.knowledge.vo.ParagraphRagVO;
import com.maxkb4j.model.service.IModelProviderService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.scoring.ScoringModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 基于 {@link ScoringModel} 的检索结果重排：按 query 对召回段落重新打分，
 * 以 rerank 分排序、按 similarity 阈值过滤、截断到 rerankTopN。
 *
 * <p>重排分写回 {@code ParagraphRagVO.similarity}，因此后续的命中直答
 * （{@code returnIfSatisfied}）与前端展示均基于精排后的相关度。</p>
 *
 * <p>重排调用失败时降级保留原始召回顺序，不阻断对话管线。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RerankStep extends AbsRerankStep {

    private final IModelProviderService modelFactory;

    @Override
    protected List<ParagraphRagVO> execute(String question, KnowledgeSetting setting, List<ParagraphRagVO> paragraphList) {
        // 仅对内容非空的段落打分；空内容段落无重排信号，直接保留在末尾
        List<ParagraphRagVO> candidates = paragraphList.stream()
                .filter(p -> p != null && StringUtils.isNotBlank(p.getContent()))
                .toList();
        if (candidates.isEmpty()) {
            return paragraphList;
        }
        List<ParagraphRagVO> blanks = paragraphList.stream()
                .filter(p -> p == null || StringUtils.isBlank(p.getContent()))
                .toList();
        try {
            List<TextSegment> segments = candidates.stream()
                    .map(p -> TextSegment.from(buildSegmentText(p)))
                    .toList();
            ScoringModel reranker = modelFactory.buildScoringModel(setting.getRerankModelId());
            List<Double> scores = reranker.scoreAll(segments, question).content();
            if (scores == null || scores.size() != candidates.size()) {
                log.warn("Rerank 返回分数数量({})与候选数({})不一致，保留原始召回顺序",
                        scores == null ? 0 : scores.size(), candidates.size());
                return paragraphList;
            }
            for (int i = 0; i < candidates.size(); i++) {
                candidates.get(i).setSimilarity(scores.get(i));
            }
            Comparator<ParagraphRagVO> byScore = Comparator.comparing(ParagraphRagVO::getSimilarity,
                    Comparator.nullsFirst(Comparator.naturalOrder()));
            List<ParagraphRagVO> ranked = candidates.stream()
                    .sorted(byScore.reversed())
                    .filter(p -> setting.getSimilarity() == null || p.getSimilarity() == null
                            || p.getSimilarity() >= setting.getSimilarity())
                    .limit(resolveTopN(setting))
                    .toList();
            log.debug("Rerank 完成: 候选 {} 条，精排后保留 {} 条", candidates.size(), ranked.size());
            return CollectionUtils.isEmpty(blanks) ? ranked : concat(ranked, blanks);
        } catch (Exception e) {
            log.warn("Rerank 调用失败，降级保留原始召回顺序: {}", e.getMessage());
            return paragraphList;
        }
    }

    private static int resolveTopN(KnowledgeSetting setting) {
        Integer rerankTopN = setting.getRerankTopN();
        return rerankTopN != null && rerankTopN > 0 ? rerankTopN
                : (setting.getTopN() != null && setting.getTopN() > 0 ? setting.getTopN() : Integer.MAX_VALUE);
    }

    /**
     * 重排输入文本：标题 + 正文（标题缺失时仅正文），与检索索引侧的嵌入输入保持同构。
     */
    private static String buildSegmentText(ParagraphRagVO paragraph) {
        String title = StringUtils.trimToEmpty(paragraph.getTitle());
        return title.isEmpty() ? paragraph.getContent() : title + "\n" + paragraph.getContent();
    }

    private static List<ParagraphRagVO> concat(List<ParagraphRagVO> ranked, List<ParagraphRagVO> blanks) {
        List<ParagraphRagVO> result = new ArrayList<>(ranked.size() + blanks.size());
        result.addAll(ranked);
        result.addAll(blanks.stream().filter(Objects::nonNull).toList());
        return result;
    }

    @Override
    public JSONObject getDetails() {
        JSONObject details = new JSONObject(true);
        details.put("step_type", "rerank_step");
        details.put("paragraphList", context.get("paragraphList"));
        details.put("runTime", context.get("runTime"));
        return details;
    }
}
