package com.maxkb4j.common.domain.form;

public class SwitchField extends BaseField {

    public SwitchField(String labelName,String field,String tooltip,boolean defaultValue) {
        super("SwitchInput",labelName,field,tooltip,true,defaultValue);
    }
}
