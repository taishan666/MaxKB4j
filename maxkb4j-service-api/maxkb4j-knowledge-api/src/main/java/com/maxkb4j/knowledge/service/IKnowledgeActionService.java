package com.maxkb4j.knowledge.service;

import com.alibaba.fastjson.JSONObject;

public interface IKnowledgeActionService{

    void updateState(String id, JSONObject details, String state);
}
