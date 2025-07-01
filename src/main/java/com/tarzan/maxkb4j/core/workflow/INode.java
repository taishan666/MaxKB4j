package com.tarzan.maxkb4j.core.workflow;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.dto.Answer;
import com.tarzan.maxkb4j.core.workflow.logic.LfNode;
import com.tarzan.maxkb4j.core.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.util.StreamEmitter;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.Data;
import org.springframework.util.CollectionUtils;

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
    protected LfNode lfNode;
    protected JSONObject nodeParams;
    protected FlowParams flowParams;
    protected WorkflowManage workflowManage;
    protected Map<String, Object> context;
    protected String answerText;
    protected String id;
    protected List<String> lastNodeIdList;
  //  protected NodeChunk nodeChunk;
    protected String runtimeNodeId;
    protected StreamEmitter emitter;


    public INode() {
        this.context = new LinkedHashMap<>();
        this.lastNodeIdList=new ArrayList<>();
    }

    public void setLfNode(LfNode lfNode) {
        this.id = lfNode.getId();
        this.lfNode = lfNode;
        this.nodeParams = getNodeParams(lfNode);
      //  this.nodeChunk = new NodeChunk(this.id);
    }

    public void setLastNodeIdList(List<String> lastNodeIdList) {
        this.lastNodeIdList = lastNodeIdList;
        this.runtimeNodeId= generateRuntimeNodeId();
    }

    private JSONObject getNodeParams(LfNode node) {
        if (Objects.nonNull(node.getProperties()) && node.getProperties().containsKey("nodeData")) {
            return node.getProperties().getJSONObject("nodeData");
        }
        return new JSONObject();
    }


    public abstract NodeResult execute() throws Exception;

    public abstract JSONObject getDetail();

    private String generateRuntimeNodeId() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            assert lastNodeIdList != null;
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


    public void getWriteErrorContext(Exception e) {
        this.status = 500;
        this.errMessage = e.getMessage();
        long startTime = (long) this.context.get("start_time");
        this.context.put("runTime", (System.currentTimeMillis() - startTime)/1000F);
    }

    public NodeResult run() throws Exception {
        long startTime = System.currentTimeMillis();
        this.context.put("start_time", startTime);
        NodeResult result = execute();
        this.context.put("runTime", (System.currentTimeMillis() - startTime)/1000F);
        return result;
    }



    public JSONObject getDetail(int index){
        JSONObject detail=new JSONObject();
        detail.put("name",lfNode.getProperties().getString("stepName"));
        detail.put("index",index);
        detail.put("type",lfNode.getType());
        detail.put("runTime",context.get("runTime"));
        detail.put("status",status);
        detail.put("err_message",errMessage);
        detail.putAll(getDetail());
        return detail;
    }



    public List<Answer> getAnswerList() {
        if (this.answerText == null) {
            return null;
        }
        Answer answer = new Answer(this.answerText, "MANY_VIEW", this.runtimeNodeId,
                this.flowParams.getChatRecordId(), new HashMap<>());
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

    public JSONArray resetMessageList(List<ChatMessage> historyMessage) {
        if (CollectionUtils.isEmpty(historyMessage)) {
            return new JSONArray();
        }
        JSONArray newMessageList = new JSONArray();
        for (ChatMessage chatMessage : historyMessage) {
            JSONObject message = new JSONObject();
            if (chatMessage instanceof SystemMessage systemMessage) {
                message.put("role", "ai");
                message.put("content", systemMessage.text());
            }
            if (chatMessage instanceof UserMessage userMessage) {
                message.put("role", "user");
                message.put("content", userMessage.singleText());
            }
            if (chatMessage instanceof AiMessage aiMessage) {
                message.put("role", "ai");
                message.put("content", aiMessage.text());
            }
            newMessageList.add(message);
        }
        return newMessageList;
    }



    @Override
    public String toString() {
        return "INode{" +
                "viewType='" + viewType + '\'' +
                ", status=" + status +
                ", errMessage='" + errMessage + '\'' +
                ", type='" + type + '\'' +
                ", lfNode=" + lfNode +
                ", nodeParams=" + nodeParams +
                ", flowParams=" + flowParams +
                ", context=" + context +
                ", answerText='" + answerText + '\'' +
                ", id='" + id + '\'' +
                ", lastNodeIdList=" + lastNodeIdList +
                ", runtimeNodeId='" + runtimeNodeId + '\'' +
                '}';
    }
}




