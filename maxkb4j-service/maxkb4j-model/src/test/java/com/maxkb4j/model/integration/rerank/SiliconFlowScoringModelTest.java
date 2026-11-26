package com.maxkb4j.model.integration.rerank;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SiliconFlowScoringModel 错误传递契约测试：
 * 非 2xx 时抛出携带真实错误（status/body）的异常，而非静默返回空分数
 */
class SiliconFlowScoringModelTest {

    private HttpServer server;
    private volatile int statusCode;
    private volatile String responseBody;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/rerank", exchange -> {
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private SiliconFlowScoringModel newModel() {
        HttpRequest request = HttpUtil.createRequest(Method.POST,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/rerank");
        return new SiliconFlowScoringModel(request, "BAAI/bge-reranker-v2-m3");
    }

    private List<TextSegment> segments() {
        return List.of(TextSegment.from("文档一"), TextSegment.from("文档二"));
    }

    @Test
    void scoreAllNon2xxThrowsWithRealError() {
        statusCode = 401;
        responseBody = "{\"code\":20012,\"message\":\"API key 无效\"}";
        RuntimeException ex = assertThrows(RuntimeException.class, () -> newModel().scoreAll(segments(), "查询"));
        assertTrue(ex.getMessage().contains("401"));
        assertTrue(ex.getMessage().contains("API key 无效"));
    }

    @Test
    void scoreAllRateLimitThrowsWithRealError() {
        statusCode = 429;
        responseBody = "{\"code\":30001,\"message\":\"Requests rate limit exceeded\"}";
        RuntimeException ex = assertThrows(RuntimeException.class, () -> newModel().scoreAll(segments(), "查询"));
        assertTrue(ex.getMessage().contains("429"));
        assertTrue(ex.getMessage().contains("rate limit"));
    }

    @Test
    void scoreAllParsesScoresInIndexOrder() {
        statusCode = 200;
        responseBody = "{\"results\":[{\"index\":1,\"relevance_score\":0.9},{\"index\":0,\"relevance_score\":0.5}]}";
        Response<List<Double>> res = newModel().scoreAll(segments(), "查询");
        assertEquals(List.of(0.5, 0.9), res.content());
    }

    @Test
    void scoreAllToleratesMissingResultsAndMalformedEntries() {
        statusCode = 200;
        responseBody = "{\"results\":[{\"index\":0,\"relevance_score\":0.5},\"bad\"]}";
        Response<List<Double>> res = newModel().scoreAll(segments(), "查询");
        assertEquals(List.of(0.5), res.content());
    }

    @Test
    void parseScoresHandlesBlankResponses() {
        assertTrue(SiliconFlowScoringModel.parseScores(null).isEmpty());
        assertTrue(SiliconFlowScoringModel.parseScores("{\"results\":[]}").isEmpty());
        assertTrue(SiliconFlowScoringModel.parseScores("{}").isEmpty());
    }
}
