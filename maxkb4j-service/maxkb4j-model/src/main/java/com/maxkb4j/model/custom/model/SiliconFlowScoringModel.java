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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class SiliconFlowScoringModel implements ScoringModel {

    private final HttpRequest request;
    private final String modelName;

    public SiliconFlowScoringModel(String modelName, ModelCredential credential, JSONObject params) {
        HttpRequest request= HttpUtil.createRequest(Method.POST, credential.getBaseUrl()+"/rerank");
        request.bearerAuth(credential.getApiKey());
        request.header("Content-Type", "application/json");
        this.request= request;
        this.modelName = modelName;
    }

    @Override
    public Response<List<Double>> scoreAll(List<TextSegment> segments, String query) {
        List<String> documents=segments.stream().map(TextSegment::text).toList();
        JSONObject params = new JSONObject();
        params.put("model",modelName);
        params.put("query",query);
        params.put("documents",documents);
        params.put("top_n",segments.size());
        HttpResponse response = request.body(params.toJSONString()).execute();
        if (response.isOk()){
            JSONObject responseBody = JSONObject.parseObject(response.body());
            JSONArray results = responseBody.getJSONArray("results");
            List<Double> relevanceScores = Optional.ofNullable(results)
                    .orElse(new JSONArray())
                    .stream()
                    .filter(item -> item instanceof JSONObject) // 防御性类型过滤
                    .sorted(Comparator.comparingInt(item -> ((JSONObject) item).getInteger("index")))
                    .map(item -> ((JSONObject) item).getDouble("relevance_score"))
                    .toList();
            return Response.from(relevanceScores);
        }
        return Response.from(List.of());
    }
}
