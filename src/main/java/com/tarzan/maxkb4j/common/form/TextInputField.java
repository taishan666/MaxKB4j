package com.tarzan.maxkb4j.common.form;

public class TextInputField extends BaseFiled{
    public TextInputField(String labelName,String field,Boolean required) {
        super.setInput_type("TextInput");
        super.setLabel(labelName);
        super.setField(field);
        super.setRequired(required);
    }
}
