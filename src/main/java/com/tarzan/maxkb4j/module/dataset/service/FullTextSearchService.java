package com.tarzan.maxkb4j.module.dataset.service;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.tarzan.maxkb4j.module.dataset.vo.HitTestVO;
import com.tarzan.maxkb4j.module.embedding.entity.EmbeddingEntity;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoExpression;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class FullTextSearchService {

    private final MongoTemplate mongoTemplate;

    public List<HitTestVO> search(List<String> datasetIds, String keyword, int maxResults) {
        // 假设 textCriteria 和 keyword 已经定义好
        TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matching(segmentContent(keyword));
        // 创建文本查询
        // Query query = TextQuery.queryText(textCriteria).addCriteria(Criteria.where("datasetId").in(datasetIds));
        // 构建聚合管道
        Aggregation aggregation = Aggregation.newAggregation(
                // 步骤1: 应用查询条件（文本搜索 + datasetId过滤）
                Aggregation.match(textCriteria),
                Aggregation.match(Criteria.where("datasetId").in(datasetIds)),
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
                Aggregation.limit(maxResults + 100)
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
        float maxScore = Math.max(result.get(0).getScore(),2);
        for (EmbeddingEntity entity : result) {
            float score = entity.getScore()/ maxScore;
            entity.setScore(score);
        }
        int endIndex = Math.min(maxResults, result.size());
        result = result.subList(0, endIndex);
        return result.stream().map(entity -> new HitTestVO(entity.getParagraphId(), entity.getScore())).toList();
    }


    public List<String> segment(String text) {
        JiebaSegmenter jiebaSegmenter = new JiebaSegmenter(); // true: 细粒度模式
        return jiebaSegmenter.sentenceProcess(text);
    }

    public String segmentContent(String text) {
        List<String> tokens = segment(text);
        return String.join(" ", tokens);
    }

}
