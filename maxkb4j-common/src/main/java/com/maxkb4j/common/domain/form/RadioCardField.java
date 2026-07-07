package com.maxkb4j.common.domain.form;

import java.util.Map;

public class RadioCardField extends SingleSelectField {

    public RadioCardField(String labelName, String field, Map<String,Object> options) {
        super(labelName,field,"",options,null);
        super.setInput_type("RadioCard");
    }
}
