package com.tarzan.maxkb4j.module.assistant;

import dev.langchain4j.service.*;
import reactor.core.publisher.Flux;


public interface Assistant {
    String chat(String message);

    Flux<String> chatFlux(String message);

/*    @SystemMessage("{{system}}")
    @UserMessage("{{user}}")
    TokenStream chatStream(@V("system")String system, @V("user")String message);*/

    TokenStream chatStream(String message);

/*    TokenStream chatStream(@MemoryId String memoryId, @UserMessage String message);*/
}
