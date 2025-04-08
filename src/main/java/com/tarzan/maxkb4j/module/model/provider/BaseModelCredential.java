package com.tarzan.maxkb4j.module.model.provider;

import com.tarzan.maxkb4j.core.form.BaseFiled;
import com.tarzan.maxkb4j.core.form.TextInputField;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public  class BaseModelCredential {

    private boolean needBaseUrl;
    private boolean needApiKey;

    public List<BaseFiled> toForm() {
        List<BaseFiled> list=new ArrayList<>(2);
        if(needBaseUrl){
            list.add(new TextInputField("API 域名","api_base"));
        }
        if(needApiKey){
            list.add(new TextInputField("API KEY" ,"api_key"));
        }
        return list;
    }
}
