package com.maxkb4j.knowledge;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;

import java.util.HashMap;
import java.util.Map;

public class EmbeddingStoreProxy {

    static Map<Integer, EmbeddingStore<TextSegment>> embeddingStores = new HashMap<>();

    private static EmbeddingStore<TextSegment> build(Integer dimension) {
        EmbeddingStore<TextSegment> embeddingStore = PgVectorEmbeddingStore.builder()
                .host("localhost")
                .port(5432)
                .database("MaxKB4J_v2")
                .user("username")
                .password("password")
                .table("embeddings_"+dimension)
                .dimension(dimension)
                .build();
        embeddingStores.put(dimension, embeddingStore);
        return embeddingStore;
    }

    public static EmbeddingStore<TextSegment> get(Integer dimension) {
        EmbeddingStore<TextSegment> embeddingStore = embeddingStores.get(dimension);
        if (embeddingStore == null) {
            embeddingStore = build(dimension);
        }
        return embeddingStore;
    }

    public static void removeAll(Filter filter) {
        embeddingStores.values().forEach(embeddingStore -> embeddingStore.removeAll(filter));
    }
}
