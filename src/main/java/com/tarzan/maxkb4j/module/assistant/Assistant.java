package com.tarzan.maxkb4j.module.assistant;

import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.*;
import reactor.core.publisher.Flux;


public interface Assistant {
    String chat(String message);

    Flux<String> chatFlux(String message);

    TokenStream chatStream(Query  query);

    TokenStream chatStream(String message);

}
