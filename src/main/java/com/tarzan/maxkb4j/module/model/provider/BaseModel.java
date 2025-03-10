package com.tarzan.maxkb4j.module.model.provider;

import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;

public interface BaseModel {

      <T> T newInstance(String modelName, ModelCredential modelCredential);
}
