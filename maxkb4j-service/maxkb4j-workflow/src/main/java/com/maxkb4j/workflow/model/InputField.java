package com.maxkb4j.workflow.model;

import com.maxkb4j.model.form.BaseField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class InputField  extends BaseField {
    private List<String> value;
}
