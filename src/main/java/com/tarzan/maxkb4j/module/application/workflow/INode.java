package com.tarzan.maxkb4j.module.application.workflow;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.dto.Answer;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.NodeDetail;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Data
public abstract class INode {
    String viewType = "many_view";
    protected int status = 200;
    protected String errMessage = "";
    protected String type;
    protected Node node;
    protected JSONObject nodeParams;
    protected FlowParams workflowParams;
    protected WorkflowManage workflowManage;
    protected JSONObject context;
    protected String answerText;
    protected String id;
    protected List<String> lastNodeIdList;
    private NodeChunk nodeChunk;
    protected String runtimeNodeId;


    public INode() {
        this.context = new JSONObject();
        this.lastNodeIdList=new ArrayList<>();
        this.nodeChunk = new NodeChunk();
        this.runtimeNodeId= generateRuntimeNodeId();
    }

    public void setNode(Node node) {
        this.node = node;
        this.nodeParams = getNodeParams(node);
    }

    private JSONObject getNodeParams(Node node) {
        if (Objects.nonNull(node.getProperties()) && node.getProperties().containsKey("node_data")) {
            return node.getProperties().getJSONObject("node_data");
        }
        return new JSONObject();
    }

    private String generateRuntimeNodeId() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            assert lastNodeIdList != null;
            assert node != null;
            String input = Arrays.toString(lastNodeIdList.stream().sorted().toArray()) + id;
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void validArgs(JSONObject nodeParams, FlowParams flowParams) throws Exception {
        //  BaseParams flowParamsClass = getFlowParamsClass(flowParams);
        BaseParams nodeParamsClass = getNodeParamsClass(nodeParams);

        if (flowParams != null) {
            if (!flowParams.isValid()) {
                throw new Exception("Invalid flow params");
            }
        }

        if (nodeParamsClass != null) {
            if (!nodeParamsClass.isValid()) {
                throw new Exception("Invalid node params");
            }
        }

        // Ensure proper type casting before comparison
        Object statusObj = node.getProperties().get("status");
        if (statusObj instanceof Integer && (Integer) statusObj != 200) {
            throw new Exception("Node is not available");
        } else if (statusObj instanceof String) {
            try {
                int status = Integer.parseInt((String) statusObj);
                if (status != 200) {
                    throw new Exception("Node is not available");
                }
            } catch (NumberFormatException e) {
                throw new Exception("Invalid status format: " + statusObj);
            }
        } else if (!(statusObj instanceof Integer)) {
            throw new Exception("Status not found or invalid type");
        }
    }

    public abstract BaseParams getNodeParamsClass(JSONObject nodeParams);


    public void getWriteErrorContext(Exception e) {
        this.status = 500;
        this.errMessage = e.getMessage();
        long startTime = this.context.getLongValue("start_time");
        this.context.put("run_time", System.currentTimeMillis() - startTime);
    }

    public NodeResult run() {
        long startTime = System.currentTimeMillis();
        this.context.put("start_time", startTime);
        NodeResult result = _run();
        this.context.put("run_time", System.currentTimeMillis() - startTime);
        return result;
    }

    public abstract NodeResult _run();

    public JSONObject getDetails(int index) {
        return new JSONObject();
    }

    public List<Answer> getAnswerList() {
        if (this.answerText == null) {
            return null;
        }
        Answer answer = new Answer(this.answerText, "MANY_VIEW", this.runtimeNodeId,
                this.workflowParams.getChatRecordId(), new HashMap<>());
        return Collections.singletonList(answer);
    }

    protected Object getReferenceField(String fields) {
        return getField(context, fields);
    }

    protected static Object getField(Map<String, Object> context, String field) {
        return context.get(field);
    }

    public JSONObject getDefaultGlobalVariable(List<JSONObject> inputFieldList) {
        JSONObject resultMap = new JSONObject();

        if (inputFieldList == null) {
            return resultMap;
        }

        for (Map<String, Object> item : inputFieldList) {
            if (item.containsKey("default_value")) {
                Object defaultValue = item.get("default_value");
                if (defaultValue != null) {
                    String variableName = (String) item.get("variable");
                    resultMap.put(variableName, defaultValue);
                }
            }
        }

        return resultMap;
    }

    public abstract void saveContext(NodeDetail nodeDetail, WorkflowManage workflowManage);

    @Override
    public String toString() {
        return "INode{" +
                "viewType='" + viewType + '\'' +
                ", status=" + status +
                ", errMessage='" + errMessage + '\'' +
                ", type='" + type + '\'' +
                ", node=" + node +
                ", nodeParams=" + nodeParams +
                ", workflowParams=" + workflowParams +
                ", context=" + context +
                ", answerText='" + answerText + '\'' +
                ", id='" + id + '\'' +
                ", lastNodeIdList=" + lastNodeIdList +
                ", nodeChunk=" + nodeChunk +
                ", runtimeNodeId='" + runtimeNodeId + '\'' +
                '}';
    }
}




