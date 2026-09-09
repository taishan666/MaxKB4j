package com.maxkb4j.knowledge.service;

import com.alibaba.fastjson.JSONObject;

/**
 * 知识库动作服务模块内基础契约。
 */
public interface IKnowledgeActionService{

    void updateState(String id, JSONObject details, String state);
}
