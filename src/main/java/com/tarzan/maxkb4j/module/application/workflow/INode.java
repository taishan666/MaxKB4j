package com.tarzan.maxkb4j.module.application.workflow;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.dto.Answer;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Consumer;

@Data
public abstract class INode {
    public static final String MANY_VIEW = "many_view";
    protected int status = 200;
    protected String errMessage = "";
    protected String type;
    protected Node node;
    protected JSONObject nodeParams;
    protected FlowParams workflowParams;
    protected WorkflowManage workflowManage;
    protected Map<String, Object> nodeParamsSerializer;
    protected Map<String, Object> flowParamsSerializer;
    protected Map<String, Object> context;
    protected String answerText;
    protected String id;
    protected List<String> upNodeIdList;
    protected String runtimeNodeId;
    protected NodeChunk nodeChunk;

    public INode() {
    }

    public INode(Node node, FlowParams workflowParams, WorkflowManage workflowManage, List<String> upNodeIdList) {
        this.node = node;
        this.workflowParams = workflowParams;
        this.workflowManage = workflowManage;
        this.upNodeIdList = upNodeIdList != null ? upNodeIdList : new ArrayList<>();
        this.nodeParams = getNodeParams(node);
        this.context = new HashMap<>();
        this.id=node.getId();
        this.runtimeNodeId = generateRuntimeNodeId();
    }

    private JSONObject getNodeParams(Node node) {
        if (Objects.nonNull(node.getProperties()) && node.getProperties().containsKey("node_data")) {
            return  node.getProperties().getJSONObject("node_data");
        }
        return new JSONObject();
    }

    private String generateRuntimeNodeId() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            String input = Arrays.toString(upNodeIdList.stream().sorted().toArray()) + node.getId();
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
        Object statusObj = ((Map<?, ?>) node.getProperties().get("status"));
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

    public BaseParams getFlowParamsClass(JSONObject flowParams) {
        return flowParams.toJavaObject(FlowParams.class);
    }

    public Consumer<Answer> getWriteErrorContext(Exception e) {
        this.status = 500;
        this.errMessage = e.getMessage();
        this.context.put("run_time", System.currentTimeMillis() - (long) this.context.get("start_time"));
        return answer -> {};
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
        Answer answer = new Answer(this.answerText, MANY_VIEW, this.runtimeNodeId,
                this.workflowParams.getChatRecordId(), new HashMap<>());
        return Collections.singletonList(answer);
    }

    protected Object getReferenceField(List<String> fields) {
        return getField(context, fields);
    }

    private static Object getField(Map<String, Object> obj, List<String> fields) {
        Object value = obj;
        for (String field : fields) {
            if (value instanceof Map) {
                value = ((Map<?, ?>) value).get(field);
                if (value == null) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return value;
    }
}




