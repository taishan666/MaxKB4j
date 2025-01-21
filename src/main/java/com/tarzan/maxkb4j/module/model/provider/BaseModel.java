package com.tarzan.maxkb4j.module.model.provider;

import com.alibaba.fastjson.JSONObject;

public interface BaseModel {

      <T> T newInstance(String modelName, JSONObject modelCredential);
}
