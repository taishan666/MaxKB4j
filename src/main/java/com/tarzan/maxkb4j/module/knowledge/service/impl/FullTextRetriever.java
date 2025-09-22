package com.tarzan.maxkb4j.module.knowledge.service.impl;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.TextChunkVO;
import com.tarzan.maxkb4j.module.knowledge.consts.SearchType;
import com.tarzan.maxkb4j.module.knowledge.service.IDataRetriever;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoExpression;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

@Component(SearchType.FULL_TEXT)
@AllArgsConstructor
public class FullTextRetriever implements IDataRetriever {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<TextChunkVO> search(List<String> datasetIds, List<String> excludeParagraphIds, String keyword, int maxResults, float minScore) {
        // 假设 textCriteria 和 keyword 已经定义好
        TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matching(segmentContent(keyword));
        // 构建聚合管道
        Aggregation aggregation = Aggregation.newAggregation(
                // 步骤1: 应用查询条件（文本搜索 + datasetId过滤）
                Aggregation.match(textCriteria),
                Aggregation.match(Criteria.where("knowledgeId").in(datasetIds)),
                // 步骤2: 添加文本搜索得分字段
                Aggregation.addFields()
                        .addField("score")
                        .withValueOf(MongoExpression.create("{$meta: 'textScore'}"))
                        .build(),

                // 步骤3: 按得分降序排序（确保每个paragraphId的最高分排在第一）
                Aggregation.sort(Sort.Direction.DESC, "score"),

                // 步骤4: 按paragraphId分组，取每组第一个文档（最高分）
                Aggregation.group("paragraphId")
                        .first("$$ROOT").as("highestScoreDoc"),

                // 步骤5: 将嵌套文档提升为根文档
                Aggregation.replaceRoot("highestScoreDoc"),

                // 步骤6: 再次按得分降序排序全局结果
                Aggregation.sort(Sort.Direction.DESC, "score"),

                // 步骤7: 限制最终返回数量
                Aggregation.limit(maxResults)
        );
        // 执行聚合查询
        List<EmbeddingEntity> result = mongoTemplate.aggregate(
                aggregation,
                mongoTemplate.getCollectionName(EmbeddingEntity.class), // 获取集合名
                EmbeddingEntity.class
        ).getMappedResults();
        if (CollectionUtils.isEmpty(result)) {
            return Collections.emptyList();
        }
        //得分归一化处理
        float maxScore = Math.max(result.get(0).getScore(),2);
        for (EmbeddingEntity entity : result) {
            float score = entity.getScore()/ maxScore;
            entity.setScore(score);
        }
        int endIndex = Math.min(maxResults, result.size());
        result = result.subList(0, endIndex);
        return result.stream().map(entity -> new TextChunkVO(entity.getParagraphId(), entity.getScore())).filter(vo -> vo.getScore() >= minScore).toList();
    }

    public String segmentContent(String text) {
        JiebaSegmenter jiebaSegmenter = new JiebaSegmenter();
        List<String> tokens = jiebaSegmenter.sentenceProcess(text);
        return String.join(" ", tokens);
    }
}
