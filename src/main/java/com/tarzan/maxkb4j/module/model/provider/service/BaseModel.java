package com.tarzan.maxkb4j.module.model.provider.service;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;

public interface BaseModel<T>  {

       T build(String modelName, ModelCredential modelCredential, JSONObject params);
}
