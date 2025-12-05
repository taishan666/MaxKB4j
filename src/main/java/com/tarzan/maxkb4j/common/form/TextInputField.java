package com.tarzan.maxkb4j.common.form;

public class TextInputField extends BaseFiled{
    public TextInputField(String labelName,String field,Boolean required) {
        super.setInput_type("TextInput");
        super.setLabel(labelName);
        super.setField(field);
        super.setRequired(required);
    }

    public TextInputField(String labelName,String field,Boolean required,Object defaultValue) {
        super.setInput_type("TextInput");
        super.setLabel(labelName);
        super.setField(field);
        super.setRequired(required);
        super.setDefault_value(defaultValue);
        if (defaultValue != null){
            super.setShow_default_value(true);
        }
    }
}
