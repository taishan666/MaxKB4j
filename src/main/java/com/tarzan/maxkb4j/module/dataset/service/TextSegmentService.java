package com.tarzan.maxkb4j.module.dataset.service;

import com.tarzan.maxkb4j.module.embedding.entity.EmbeddingEntity;
import lombok.AllArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TextSegmentService {

  //  private final TextSegmentRepository repository;
    private final ElasticsearchTemplate elasticsearchTemplate;

    public void saveBatch(List<EmbeddingEntity> entities) {
      //  repository.saveAll(entities);
    }


    public List<EmbeddingEntity> search(List<String> datasetIds,String keyword, int maxResults) {
        // 构建全文检索查询
        // 使用 Criteria 创建匹配查询
        Criteria criteria = new Criteria("content").matches(keyword); // 替换 "content" 为你的字段名，keyword 为搜索关键字

        // 使用 CriteriaQuery 包装 Criteria
        Query searchQuery = new CriteriaQuery(criteria);

        SearchHits<EmbeddingEntity> hits = elasticsearchTemplate.search(searchQuery, EmbeddingEntity.class);

        // 提取结果并保留得分
        return hits.stream()
                .map(hit -> {
                    EmbeddingEntity article = hit.getContent();
                    article.setScore(hit.getScore()); // 设置得分
                    return article;
                })
                .collect(Collectors.toList());
    }
}
