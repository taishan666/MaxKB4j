package com.tarzan.maxkb4j.module.dataset;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;

import java.util.List;

public class Retriever {

    public static void main(String[] args) {

        String apiKey = "";
        EmbeddingModel embeddingModel = QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-v2")
                .build();
        EmbeddingStore<TextSegment> embeddingStore = PgVectorEmbeddingStore.builder()
                .host("localhost")
                .port(5432)
                .database("Maxkb4j")
                .user("username")
                .password("password")
                .table("my_embedding")
                .dimension(embeddingModel.dimension())
                .build();
        String text = "hello friends";
        String queryText = "hello";
      //  embeddingStore.add(embeddingModel.embed(TextSegment.from(text)).content(),TextSegment.from(text));
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.15)
                .build();
        EmbeddingSearchRequest request= EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingModel.embed(TextSegment.from(queryText)).content())
                .maxResults(3)
                .minScore(0.15)
                .build();
        EmbeddingSearchResult<TextSegment> result= embeddingStore.search(request);
        System.out.println(result.matches());
        List<Content> contents = contentRetriever.retrieve(new Query(queryText));
        System.out.println(contents);
        ChatLanguageModel chatModel = QwenChatModel.builder()
                //   .baseUrl(credential.getApiBase())
                .apiKey(apiKey)
                .modelName("qwen-plus")
                .build();
       // CompressingQueryTransformer queryTransformer=new CompressingQueryTransformer(chatModel);
/*        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryTransformer(queryTransformer)
                .contentRetriever(contentRetriever)
              //  .queryRouter(queryRouter)
                .build();
        retrievalAugmentor.*/

    /*    WebSearchEngine googleSearchEngine = GoogleCustomWebSearchEngine.builder()
                .apiKey(System.getenv("GOOGLE_API_KEY"))
                .csi(System.getenv("GOOGLE_SEARCH_ENGINE_ID"))
                .build();*/
    }
}
