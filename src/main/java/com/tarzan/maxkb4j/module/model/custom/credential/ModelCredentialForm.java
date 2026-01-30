package com.tarzan.maxkb4j.module.model.custom.credential;

import com.tarzan.maxkb4j.common.domain.form.BaseField;
import com.tarzan.maxkb4j.common.domain.form.TextInputField;

import java.util.ArrayList;
import java.util.List;

public  class ModelCredentialForm {

    private final boolean showBaseUrl;
    private final boolean showApiKey;
    private String defaultBaseUrl;

    public ModelCredentialForm(boolean showBaseUrl, boolean showApiKey) {
        this.showBaseUrl = showBaseUrl;
        this.showApiKey = showApiKey;
    }

    public ModelCredentialForm(boolean showBaseUrl, boolean showApiKey, String defaultBaseUrl) {
        this.showBaseUrl = showBaseUrl;
        this.showApiKey = showApiKey;
        this.defaultBaseUrl = defaultBaseUrl;
    }

    public List<BaseField> toForm() {
        List<BaseField> list=new ArrayList<>(2);
        if(showBaseUrl){
            list.add(new TextInputField("API 域名","baseUrl",true,defaultBaseUrl));
        }
        if(showApiKey){
            list.add(new TextInputField("API KEY" ,"apiKey",true));
        }
        return list;
    }
}
