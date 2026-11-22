package com.maxkb4j.model.custom.model;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.entity.ModelCredential;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * SiliconFlow 重排模型（/rerank 接口）
 * <p>
 * 职责拆分：
 * <ul>
 *     <li>{@link #createRequest(ModelCredential)} —— 由凭证构建 HTTP 请求，与业务逻辑解耦</li>
 *     <li>{@link #parseScores(String)} —— 纯函数式响应解析，便于单元测试</li>
 * </ul>
 */
@Slf4j
public class SiliconFlowScoringModel implements ScoringModel {

    private final HttpRequest request;
    private final String modelName;

    public SiliconFlowScoringModel(String modelName, ModelCredential credential, JSONObject params) {
        this(createRequest(credential), modelName);
    }

    /**
     * 包级构造器：允许注入已构建好的请求，便于测试与替换传输实现
     */
    SiliconFlowScoringModel(HttpRequest request, String modelName) {
        this.request = request;
        this.modelName = modelName;
    }

    private static HttpRequest createRequest(ModelCredential credential) {
        HttpRequest request = HttpUtil.createRequest(Method.POST, credential.getBaseUrl() + Http.ENDPOINT_RERANK);
        request.bearerAuth(credential.getApiKey());
        request.header(Http.CONTENT_TYPE, Http.APPLICATION_JSON);
        return request;
    }

    /**
     * 从响应体解析相关性分数列表（纯函数，便于单元测试）：
     * 按 index 升序还原与入参 documents 对应的分数顺序，跳过异常条目
     */
    static List<Double> parseScores(String responseBody) {
        JSONObject body = JSONObject.parseObject(responseBody);
        if (body == null) {
            return List.of();
        }
        JSONArray results = body.getJSONArray(RequestField.RESULTS);
        return Optional.ofNullable(results)
                .orElse(new JSONArray())
                .stream()
                .filter(item -> item instanceof JSONObject) // 防御性类型过滤
                .sorted(Comparator.comparingInt(item -> ((JSONObject) item).getInteger(RequestField.INDEX)))
                .map(item -> ((JSONObject) item).getDouble(RequestField.RELEVANCE_SCORE))
                .toList();
    }

    @Override
    public Response<List<Double>> scoreAll(List<TextSegment> segments, String query) {
        List<String> documents = segments.stream().map(TextSegment::text).toList();
        JSONObject params = new JSONObject();
        params.put(RequestField.MODEL, modelName);
        params.put(RequestField.QUERY, query);
        params.put(RequestField.DOCUMENTS, documents);
        params.put(RequestField.TOP_N, segments.size());
        try (HttpResponse response = request.body(params.toJSONString()).execute()) {
            if (response.isOk()) {
                return Response.from(parseScores(response.body()));
            }
            String message = "SiliconFlow 重排接口调用失败, model=" + modelName
                    + ", status=" + response.getStatus() + ", body=" + response.body();
            log.error(message);
            throw new RuntimeException(message);
        }
    }
}
