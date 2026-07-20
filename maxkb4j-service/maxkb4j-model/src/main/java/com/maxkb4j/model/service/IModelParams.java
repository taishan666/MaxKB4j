package com.maxkb4j.model.service;


import com.maxkb4j.common.domain.form.BaseField;

import java.util.List;

public interface IModelParams {

    List<BaseField> toForm();
}
