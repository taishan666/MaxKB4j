package com.tarzan.maxkb4j.core.workflow.node.imageunderstand.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.IMAGE_UNDERSTAND;

@Slf4j
public class ImageUnderstandNode extends INode {


    public ImageUnderstandNode(JSONObject properties) {
        super(properties);
        super.setType(IMAGE_UNDERSTAND.getKey());
    }

    private static final Map<String, String> mimeTypeMap = new HashMap<>();
    static {
        mimeTypeMap.put("jpg", "image/jpeg");
        mimeTypeMap.put("jpeg", "image/jpeg");
        mimeTypeMap.put("png", "image/png");
        mimeTypeMap.put("gif", "image/gif");
    }


    @Override
    public void saveContext(JSONObject detail) {
        context.put("answer", detail.get("answer"));
    }



}
