package com.maxkb4j.workflow.model.params;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

/**
 * 循环节点参数
 * 从 LoopNode.NodeParams 提取
 */
@Data
public class LoopNodeParams {
    private String loopType;
    private JSONObject loopBody;
    private Integer number;
    private List<String> array;
}