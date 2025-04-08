package com.tarzan.maxkb4j.module.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInputVO;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public  class ImageModelCredential {

    private boolean needBaseUrl;
    private boolean needApiKey;

    public List<ModelInputVO> getModelParamsSettingForm() {
        ModelInputVO input1=new ModelInputVO();
        JSONObject attrs1=new JSONObject();
        attrs1.put("min",0.1);
        attrs1.put("max",1);
        attrs1.put("step",0.01);
        attrs1.put("precision",2);
        attrs1.put("show-input",true);
        attrs1.put("show-input-controls",false);
        input1.setAttrs(attrs1);
        input1.setInput_type("Slider");
        JSONObject label1=new JSONObject();
        JSONObject labelAttrs1=new JSONObject();
        labelAttrs1.put("tooltip","较高的数值会使输出更加随机，而较低的数值会使其更加集中和确定");
        label1.put("attrs",labelAttrs1);
        label1.put("input_type","TooltipLabel");
        label1.put("label","温度");
        label1.put("props_info",new JSONObject());
        input1.setLabel(label1);
        input1.setField("temperature");
        input1.setDefault_value(0.7);
        ModelInputVO input2=new ModelInputVO();
        JSONObject attrs2=new JSONObject();
        attrs2.put("min",1);
        attrs2.put("max",100000);
        attrs2.put("step",1);
        attrs2.put("precision",0);
        attrs2.put("show-input",true);
        attrs2.put("show-input-controls",false);
        input2.setAttrs(attrs2);
        input2.setInput_type("Slider");
        JSONObject label2=new JSONObject();
        JSONObject labelAttrs2=new JSONObject();
        labelAttrs2.put("tooltip","指定模型可生成的最大token个数");
        label2.put("attrs",labelAttrs2);
        label2.put("input_type","TooltipLabel");
        label2.put("label","输出最大Tokens");
        label2.put("props_info",new JSONObject());
        input2.setLabel(label2);
        input2.setField("max_tokens");
        input2.setDefault_value(800);
        return List.of(input1,input2);
    }


    public List<ModelInputVO> toForm() {
        List<ModelInputVO> list=new ArrayList<>(2);
        if(needBaseUrl){
            ModelInputVO input1=new ModelInputVO();
            input1.setInput_type("TextInput");
            input1.setLabel("API 域名");
            input1.setField("api_base");
            list.add(input1);
        }
        if(needApiKey){
            ModelInputVO input2=new ModelInputVO();
            input2.setInput_type("PasswordInput");
            input2.setLabel("API Key");
            input2.setField("api_key");
            list.add(input2);
        }
        return list;
    }
}
