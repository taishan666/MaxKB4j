package com.maxkb4j.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.ModelAwareParams;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;

import static com.maxkb4j.workflow.enums.NodeType.NL2SQL;


public class NL2SqlNode extends AbsNode {
    public NL2SqlNode(String id,JSONObject properties) {
        super(id,properties);
        this.setType(NL2SQL.getKey());
    }

    @Data
    public static class NodeParams implements ModelAwareParams {
        private String modelId;
        private String modelIdType;
        private List<String> modelIdReference;
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
