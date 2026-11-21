package com.maxkb4j.model.custom.model;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alibaba.fastjson.JSONObject;
import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.output.Response;
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
 * SiliconFlowImageModel 错误传递契约测试：
 * 非 2xx 时抛出携带真实错误（status/body）的异常，而非静默返回空列表
 */
class SiliconFlowImageModelTest {

    private HttpServer server;
    private volatile int statusCode;
    private volatile String responseBody;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/images/generations", exchange -> {
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

    private SiliconFlowImageModel newModel() {
        HttpRequest request = HttpUtil.createRequest(Method.POST,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/images/generations");
        return new SiliconFlowImageModel(request, "Kwai-Kolors/Kolors", new JSONObject());
    }

    @Test
    void generateNon2xxThrowsWithRealError() {
        statusCode = 401;
        responseBody = "{\"code\":20012,\"message\":\"API key 无效\"}";
        RuntimeException ex = assertThrows(RuntimeException.class, () -> newModel().generate("一只猫", 1));
        assertTrue(ex.getMessage().contains("401"));
        assertTrue(ex.getMessage().contains("API key 无效"));
    }

    @Test
    void generateRateLimitThrowsWithRealError() {
        statusCode = 429;
        responseBody = "{\"code\":30001,\"message\":\"Requests rate limit exceeded\"}";
        RuntimeException ex = assertThrows(RuntimeException.class, () -> newModel().generate("一只猫", 1));
        assertTrue(ex.getMessage().contains("429"));
        assertTrue(ex.getMessage().contains("rate limit"));
    }

    @Test
    void generateSingleWithEmptyImagesThrowsClearError() {
        statusCode = 200;
        responseBody = "{\"images\":[]}";
        RuntimeException ex = assertThrows(RuntimeException.class, () -> newModel().generate("一只猫"));
        assertTrue(ex.getMessage().contains("未包含图像数据"));
    }

    @Test
    void editWithEmptyImagesThrowsClearError() {
        statusCode = 200;
        responseBody = "{\"images\":[]}";
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> newModel().edit(Image.builder().build(), "一只猫"));
        assertTrue(ex.getMessage().contains("未包含图像数据"));
    }

    @Test
    void generateParsesImageUrlsAndSkipsUrlLessEntries() {
        statusCode = 200;
        responseBody = "{\"images\":[{\"url\":\"http://img.example.com/a.png\"},{}]}";
        Response<List<Image>> res = newModel().generate("一只猫", 2);
        assertEquals(1, res.content().size());
        assertEquals("http://img.example.com/a.png", res.content().get(0).url().toString());
    }

    @Test
    void parseImagesHandlesBlankResponses() {
        assertTrue(SiliconFlowImageModel.parseImages(null).isEmpty());
        assertTrue(SiliconFlowImageModel.parseImages("{\"images\":[]}").isEmpty());
        assertTrue(SiliconFlowImageModel.parseImages("{}").isEmpty());
        assertTrue(SiliconFlowImageModel.parseImages("{\"images\":[{}]}").isEmpty());
    }
}
