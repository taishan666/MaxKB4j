package com.tarzan.maxkb4j.core.langchain4j;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.DefaultMetadataStorageConfig;
import dev.langchain4j.store.embedding.pgvector.MetadataStorageConfig;
import dev.langchain4j.store.embedding.pgvector.MetadataStorageMode;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EmbeddingStoreFactory {

    @Value(value = "${spring.datasource.username}")
    private String username;
    @Value(value = "${spring.datasource.password}")
    private String password;
    @Value(value = "${spring.datasource.url}")
    private String url;


    private EmbeddingStore<TextSegment> build(String knowledgeId, Integer dimension, boolean createTable, boolean dropTableFirst) {
        String tableName = "knowledge_vector_" + knowledgeId;
        List<String> columnDefinitions = new ArrayList<>();
        columnDefinitions.add("is_active bool NOT NULL");
        columnDefinitions.add("source_id varchar(50) COLLATE pg_catalog.default NOT NULL");
        columnDefinitions.add("source_type int2 NOT NULL");
        columnDefinitions.add("paragraph_id varchar(50) COLLATE pg_catalog.default NOT NULL");
        columnDefinitions.add("document_id varchar(50) COLLATE pg_catalog.default NOT NULL");
        MetadataStorageConfig metadataStorageConfig = DefaultMetadataStorageConfig.builder()
                .storageMode(MetadataStorageMode.COLUMN_PER_KEY)
                .columnDefinitions(columnDefinitions)
                .build();
        PgVectorEmbeddingStore.PgVectorEmbeddingStoreBuilder builder = PgVectorEmbeddingStore.builder()
                .host(getHost())
                .port(getPort())
                .database(getDatabase())
                .user(username)
                .password(password)
                .table(tableName)
                .createTable(createTable)
                .dropTableFirst(dropTableFirst)
                .metadataStorageConfig(metadataStorageConfig);
        if (createTable && dimension != null) {
            builder.dimension(dimension);
        }
        return builder.build();
    }

    public EmbeddingStore<TextSegment> create(String knowledgeId, int dimension) {
        return build(knowledgeId, dimension, true, false);
    }

    public EmbeddingStore<TextSegment> get(String knowledgeId) {
        return build(knowledgeId, null, false, false);
    }


    public void drop(String knowledgeId) {
        build(knowledgeId, null, false, true);
    }

    private String getHost() {
        return url.split("//")[1].split(":")[0];
    }

    private int getPort() {
        return Integer.parseInt(url.split("//")[1].split(":")[1].split("/")[0]);
    }

    private String getDatabase() {
        Pattern pattern = Pattern.compile("jdbc:postgresql://[^/]+/([^?]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Invalid JDBC URL format: " + url);
    }
}
