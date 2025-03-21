package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.BaseReranker;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
public class BaiLianReranker extends BaseReranker implements BaseModel {

    String url = "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank";
    private static final JSONObject params = new JSONObject();


    private String apiKey;

    public BaiLianReranker(String apiKey) {
        super();
        this.apiKey = apiKey;
    }

    @Override
    public <T> T build(String modelName, ModelCredential credential, JSONObject params) {
        params.put("model", modelName);
        JSONObject parameters = new JSONObject();
        parameters.put("return_documents", true);
        parameters.put("top_n", 5);
        params.put("parameters", parameters);
        return (T) new BaiLianReranker(credential.getApiKey());
    }

    @Override
    public List<Map<String,Object>> textReRank(String query, List<String> documents)  {
        JSONObject input = new JSONObject();
        input.put("query", query);
        input.put("documents", documents);
        params.put("input", input);
        HttpResponse res=HttpUtil.createPost(url)
                .header("Authorization", "Bearer " + this.apiKey)
                .body(params.toJSONString())
                .execute();
        List<Map<String,Object>> resultList=new ArrayList<>();
        JSONObject jsonObject= JSONObject.parseObject(res.body());
        JSONObject output = jsonObject.getJSONObject("output");
        if (output != null) {
            JSONArray results = output.getJSONArray("results");
            for (int i = 0; i < results.size(); i++) {
                JSONObject result= results.getJSONObject(i);
                JSONObject document = result.getJSONObject("document");
                resultList.add(Map.of("text", document.getString("text"),"score",result.getFloat("relevance_score")));
            }
            return resultList;
        }
        return new ArrayList<>();
    }
}
