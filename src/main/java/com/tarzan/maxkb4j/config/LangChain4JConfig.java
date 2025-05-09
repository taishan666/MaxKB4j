package com.tarzan.maxkb4j.config;

import com.tarzan.maxkb4j.listener.LlmListener;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class LangChain4JConfig {

    @Bean
    ChatModelListener chatModelListener() {
        return new LlmListener();
    }

    @Bean
    ChatMemoryStore chatMemoryStore() {
        //todo  自定义
        return new InMemoryChatMemoryStore();
    }
}
