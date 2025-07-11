package com.tarzan.maxkb4j.config;

import com.tarzan.maxkb4j.module.dataset.domain.entity.EmbeddingEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

@Configuration
public class MongoConfig {
    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory factory) {
        MongoTemplate template = new MongoTemplate(factory);
        template.indexOps(EmbeddingEntity.class).ensureIndex(
                new TextIndexDefinition.TextIndexDefinitionBuilder()
                        .onField("content")
                        .build()
        );
        return template;
    }
}
