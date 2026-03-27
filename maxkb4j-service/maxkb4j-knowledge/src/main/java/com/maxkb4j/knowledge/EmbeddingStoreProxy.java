package com.maxkb4j.knowledge;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.pgvector.DefaultMetadataStorageConfig;
import dev.langchain4j.store.embedding.pgvector.MetadataStorageMode;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Component
public class EmbeddingStoreProxy {

    private final ConcurrentHashMap<Integer, EmbeddingStore<TextSegment>> embeddingStores = new ConcurrentHashMap<>();
    private final DataSource dataSource;


    private EmbeddingStore<TextSegment> build(Integer dimension) {
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table("embeddings_" + dimension)
                .dimension(dimension)
                .metadataStorageConfig(DefaultMetadataStorageConfig.builder()
                        .storageMode(MetadataStorageMode.COMBINED_JSONB)
                        .columnDefinitions(Collections.singletonList("metadata JSONB NULL"))
                        .build())
                .build();
    }

    public EmbeddingStore<TextSegment> get(Integer dimension) {
        return embeddingStores.computeIfAbsent(dimension, this::build);
    }

    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest searchRequest) {
        return get(searchRequest.queryEmbedding().dimension()).search(searchRequest);
    }

    public void removeAll(Filter filter) {
        embeddingStores.values().forEach(store -> store.removeAll(filter));
    }
}
