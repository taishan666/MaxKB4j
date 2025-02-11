package com.tarzan.maxkb4j.module.application;


import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;

import java.util.function.Consumer;

public interface TokenStream {


    TokenStream onNext(Consumer<String> var1);

    TokenStream onComplete(Consumer<Response<AiMessage>> var1);

    TokenStream onError(Consumer<Throwable> var1);

    void start();
}
