package com.tarzan.maxkb4j.module.application.workflow.node.imagegenerate;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.INode;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.node.imagegenerate.input.ImageGenerateParams;

import java.util.Objects;

public abstract class IImageGenerateNode  extends INode {

    @Override
    public String getType() {
        return "image-generate-node";
    }

    @Override
    public ImageGenerateParams getNodeParamsClass(JSONObject nodeParams) {
        if(Objects.isNull(nodeParams)){
            return new ImageGenerateParams();
        }
        return nodeParams.toJavaObject(ImageGenerateParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams));
    }



    public abstract NodeResult execute(ImageGenerateParams nodeParams);
}
