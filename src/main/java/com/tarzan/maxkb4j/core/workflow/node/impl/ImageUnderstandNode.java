package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.IMAGE_UNDERSTAND;

@Slf4j
public class ImageUnderstandNode extends AiChatNode {

    public ImageUnderstandNode(String id,JSONObject properties) {
        super(id,properties);
        super.setType(IMAGE_UNDERSTAND.getKey());
    }

}
