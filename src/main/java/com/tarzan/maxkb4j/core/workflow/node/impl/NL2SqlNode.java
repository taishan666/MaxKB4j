package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.NL2SQL;

public class NL2SqlNode extends INode {
    public NL2SqlNode(String id,JSONObject properties) {
        super(id,properties);
        this.setType(NL2SQL.getKey());
    }

    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {

    }

    @Data
    public static class NodeParams {
        private String modelId;
        private JSONObject modelParamsSetting;
        private String dialogueType;
        private int dialogueNumber;
        private DatabaseSetting databaseSetting;
        private List<String> questionReferenceAddress;
    }

    @Data
    public static class DatabaseSetting {
        private String type;
        private String host;
        private Integer port;
        private String username;
        private String password;
        private String database;
        private String query;
    }
}
