package com.tarzan.maxkb4j.module.model.custom.model;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.rerank.*;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.model.scoring.ScoringModel;
import lombok.Data;

import java.util.List;
@Data
public class BaiLianReranker implements ScoringModel {


    private TextReRankParam param;

    public BaiLianReranker(String modelName, ModelCredential credential, JSONObject params) {
        param = TextReRankParam.builder()
                .model(modelName)
                .apiKey(credential.getApiKey())
                .returnDocuments(false)
                .build();
    }

    @Override
    public Response<List<Double>> scoreAll(List<TextSegment> segments, String query) {
        TextReRank textReRank = new TextReRank();
        List<String> documents = segments.stream().map(TextSegment::text).toList();
        param.setDocuments(documents);
        param.setQuery(query);
        try {
            TextReRankResult result = textReRank.call(param);
            List<Double> scores = result.getOutput().getResults().stream().map(TextReRankOutput.Result::getRelevanceScore).toList();
            TextReRankUsage usage = result.getUsage();
            return Response.from(scores, new TokenUsage(usage.getTotalTokens()));
        } catch (NoApiKeyException | InputRequiredException e) {
            throw new RuntimeException(e);
        }
    }


}
