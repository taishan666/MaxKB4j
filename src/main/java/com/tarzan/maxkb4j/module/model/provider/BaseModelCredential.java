package com.tarzan.maxkb4j.module.model.provider;

import com.tarzan.maxkb4j.core.form.BaseFiled;
import com.tarzan.maxkb4j.core.form.TextInputField;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public  class BaseModelCredential {

    private boolean showBaseUrl;
    private boolean showApiKey;

    public List<BaseFiled> toForm() {
        List<BaseFiled> list=new ArrayList<>(2);
        if(showBaseUrl){
            list.add(new TextInputField("API 域名","baseUrl",true));
        }
        if(showApiKey){
            list.add(new TextInputField("API KEY" ,"apiKey",true));
        }
        return list;
    }
}
