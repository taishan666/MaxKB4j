package com.tarzan.maxkb4j.module.application;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.TokenUsage;
import lombok.Data;

import java.util.Iterator;

@Data
public class ChatStream {

    private TokenUsage tokenUsage;

    private Iterator<AiMessage> iterator;
}
