package com.maxkb4j.start.config;

import com.maxkb4j.knowledge.entity.TextChunk;
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
        template.indexOps(TextChunk.class).createIndex(
                new TextIndexDefinition.TextIndexDefinitionBuilder()
                        .onField("content")
                        .build()
        );
        return template;
    }
}
