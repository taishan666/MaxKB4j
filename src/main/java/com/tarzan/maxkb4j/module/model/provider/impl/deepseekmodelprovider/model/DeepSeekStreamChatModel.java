package com.tarzan.maxkb4j.module.model.provider.impl.deepseekmodelprovider.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.TokenUsage;
import lombok.Builder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Builder
public class DeepSeekStreamChatModel implements StreamingChatModel {

    private String baseUrl = "https://api.deepseek.com/v1";
    private String apiKey;
    private String modelName;
    private String modelId;
    private final StringBuffer contentBuilder = new StringBuffer();
    private final AtomicReference<TokenUsage> tokenUsage = new AtomicReference<>();
    public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        JSONObject params = new JSONObject();
        JSONArray messages = new JSONArray();
        chatRequest.messages().forEach(e -> {
            JSONObject message = new JSONObject();
            if (e instanceof SystemMessage systemMessage) {
                message.put("role", "system");
                message.put("content", systemMessage.text());
            }
            if (e instanceof UserMessage userMessage) {
                message.put("role", "user");
                message.put("content", userMessage.singleText());
            }
            if (e instanceof AiMessage aiMessage) {
                message.put("role", "ai");
                message.put("content", aiMessage.text());
            }
            messages.add(message);
        });
        params.put("messages", messages);
        params.put("model", modelName);
        params.put("stream", true);
        // 错误回调
        webClient.post()
                .uri("/chat/completions")
                .bodyValue(params.toJSONString())
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(data -> {
                    System.out.println(data);
                    if ("[DONE]".equals(data)) {
                        return Mono.empty(); // 忽略 [DONE] 信号
                    }
                    JSONObject chunk= JSONObject.parseObject(data);
                    JSONArray choices = chunk.getJSONArray("choices");
                    JSONObject usage = chunk.getJSONObject("usage");
                    if (usage != null){
                        tokenUsage.set(new TokenUsage(usage.getInteger("input_tokens"),usage.getInteger("output_tokens"),usage.getInteger("total_tokens")));
                    }
                    JSONObject delta= choices.getJSONObject(0).getJSONObject("delta");
                    String content = delta.getString("content");
                    String reasoningContent = delta.getString("reasoning_content");
                    if (Objects.nonNull(reasoningContent)){
                        handler.onPartialResponse("<think>"+reasoningContent+"</think>");
                    }
                    if (Objects.nonNull(content)){
                        contentBuilder.append(content);
                        handler.onPartialResponse(content);
                    }
                    return Mono.empty();
                })
                .doOnComplete(() -> {
                    ChatResponse chatResponse=ChatResponse.builder().aiMessage(AiMessage.from(contentBuilder.toString())).modelName(modelName).tokenUsage(tokenUsage.get()).build();
                    handler.onCompleteResponse(chatResponse);
                }) // 所有数据接收完成
                .doOnError(handler::onError)
                .subscribe();
    }


}
