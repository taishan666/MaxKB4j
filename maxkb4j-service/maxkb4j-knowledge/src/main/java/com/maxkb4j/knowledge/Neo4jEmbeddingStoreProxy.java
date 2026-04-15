package com.maxkb4j.knowledge;

import dev.langchain4j.community.store.embedding.neo4j.Neo4jEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Component
public class Neo4jEmbeddingStoreProxy {

    private final ConcurrentHashMap<Integer, EmbeddingStore<TextSegment>> embeddingStores = new ConcurrentHashMap<>();
    private final DataSource dataSource;


    private EmbeddingStore<TextSegment> build(Integer dimension) {
        return  Neo4jEmbeddingStore.builder()
                .withBasicAuth("", "username", "password")
                .dimension(dimension)
                .indexName("embeddings_" + dimension)
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
