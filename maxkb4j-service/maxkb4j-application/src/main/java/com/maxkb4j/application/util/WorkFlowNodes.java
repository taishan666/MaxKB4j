package com.maxkb4j.application.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * 工作流 JSON 节点安全访问工具：统一处理 nodes / properties / nodeData 的空值判断。
 *
 * @author tarzan
 */
public final class WorkFlowNodes {

    private WorkFlowNodes() {
    }

    /**
     * 获取工作流中的节点数组，工作流或节点不存在时返回 null。
     */
    public static JSONArray getNodes(JSONObject workFlow) {
        return workFlow == null ? null : workFlow.getJSONArray("nodes");
    }

    /**
     * 获取节点的 nodeData，节点结构不完整时返回 null。
     */
    public static JSONObject getNodeData(JSONObject node) {
        if (node == null) {
            return null;
        }
        JSONObject properties = node.getJSONObject("properties");
        return properties == null ? null : properties.getJSONObject("nodeData");
    }
}
