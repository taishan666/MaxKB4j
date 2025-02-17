/*
package com.tarzan.maxkb4j.module.dataset;

import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.web.search.WebSearchEngine;
import net.sf.jsqlparser.expression.operators.relational.FullTextSearch;

public class Retriever {

    public static void main(String[] args) {
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                // maxResults can also be specified dynamically depending on the query
                .dynamicMaxResults(query -> 3)
                .minScore(0.75)
                // minScore can also be specified dynamically depending on the query
                .dynamicMinScore(query -> 0.75)
                .filter(metadataKey("userId").isEqualTo("12345"))
                // filter can also be specified dynamically depending on the query
                .dynamicFilter(query -> {
                    String userId = getUserId(query.metadata().chatMemoryId());
                    return metadataKey("userId").isEqualTo(userId);
                })
                .build();
        CompressingQueryTransformer queryTransformer=new CompressingQueryTransformer();
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryTransformer(queryTransformer)
                .contentRetriever(contentRetriever)
              //  .queryRouter(queryRouter)
                .build();


        WebSearchEngine googleSearchEngine = GoogleCustomWebSearchEngine.builder()
                .apiKey(System.getenv("GOOGLE_API_KEY"))
                .csi(System.getenv("GOOGLE_SEARCH_ENGINE_ID"))
                .build();
    }
}
*/
