package com.tarzan.maxkb4j.module.dataset.repository;

import com.tarzan.maxkb4j.module.embedding.entity.EmbeddingEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface TextSegmentRepository extends ElasticsearchRepository<EmbeddingEntity, String> {

    List<EmbeddingEntity> findByContent(String content);
}
