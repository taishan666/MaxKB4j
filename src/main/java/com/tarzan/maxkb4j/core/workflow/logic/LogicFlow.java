package com.tarzan.maxkb4j.core.workflow.logic;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Data
public class LogicFlow {
    private List<LfNode> nodes;
    private List<LfEdge> edges;

    public LogicFlow(List<LfNode> nodes, List<LfEdge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public static LogicFlow newInstance(JSONObject flowJson) {
        // 使用TypeReference来指定复杂的类型
        return JSON.parseObject(flowJson.toJSONString(), new TypeReference<LogicFlow>() {});
    }




}
