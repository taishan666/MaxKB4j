package com.tarzan.maxkb4j.module.application;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import lombok.Data;

import java.util.Iterator;

@Data
public class ChatStream {

    private Response<AiMessage> response;

    private Iterator<AiMessage> iterator;
}
