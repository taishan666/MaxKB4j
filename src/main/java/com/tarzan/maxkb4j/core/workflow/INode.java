package com.tarzan.maxkb4j.core.workflow;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.Data;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Data
public abstract class INode {
    @Setter
    protected String id;
    protected int status = 200;
    protected String errMessage = "";
    protected String type;
    protected String viewType;
    protected JSONObject properties;
    protected JSONObject nodeParams;
    protected WorkflowManage workflowManage;
    protected JSONObject context;
    protected String answerText;
    protected List<String> upNodeIdList;
    protected String runtimeNodeId;
   // protected Sinks.Many<ChatMessageVO> sink;


    public INode(JSONObject properties) {
        this.context = new JSONObject();
        this.upNodeIdList=new ArrayList<>();
        this.properties = properties;
        this.nodeParams = getNodeParams(properties);
        this.runtimeNodeId= generateRuntimeNodeId();
        this.viewType = "many_view";
    }

  /*  public void setWorkflowManage(WorkflowManage workflowManage) {
        this.workflowManage = workflowManage;
      //  this.flowParams = workflowManage.getFlowParams();
        //this.sink = workflowManage.getSink();
    }*/


    public void setUpNodeIdList(List<String> upNodeIdList) {
        this.upNodeIdList = upNodeIdList;
        this.runtimeNodeId= generateRuntimeNodeId();
    }

    private JSONObject getNodeParams(JSONObject properties) {
        if (Objects.nonNull(properties) && properties.containsKey("nodeData")) {
            return properties.getJSONObject("nodeData");
        }
        return new JSONObject();
    }

    public abstract NodeResult execute() throws Exception;

    public abstract JSONObject getDetail();

    private String generateRuntimeNodeId() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            assert upNodeIdList != null;
            String input = Arrays.toString(upNodeIdList.stream().sorted().toArray()) + id;
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


/*    public void getWriteErrorContext(Exception e) {
        this.status = 500;
        this.errMessage = e.getMessage();
        long startTime = (long) this.context.get("start_time");
        this.context.put("runTime", (System.currentTimeMillis() - startTime)/1000F);
    }*/

    public NodeResult run() throws Exception {
        long startTime = System.currentTimeMillis();
        this.context.put("start_time", startTime);
        NodeResult result = execute();
        this.context.put("runTime", (System.currentTimeMillis() - startTime)/1000F);
        return result;
    }



    public JSONObject getDetail(int index){
        JSONObject detail=new JSONObject();
        detail.put("name",properties.getString("nodeName"));
        detail.put("index",index);
        detail.put("type",type);
        detail.put("runTime",context.get("runTime"));
        detail.put("status",status);
        detail.put("err_message",errMessage);
        detail.putAll(getDetail());
        return detail;
    }



/*    public List<Answer> getAnswerList() {
        if (this.answerText == null) {
            return null;
        }
        Answer answer = new Answer(this.answerText, "MANY_VIEW", this.runtimeNodeId,
                this.flowParams.getChatRecordId(), new HashMap<>());
        return Collections.singletonList(answer);
    }*/

    protected Object getReferenceField(String fields) {
        return getField(context, fields);
    }

    protected static Object getField(Map<String, Object> context, String field) {
        return context.get(field);
    }

    public JSONArray resetMessageList(List<ChatMessage> historyMessage) {
        if (CollectionUtils.isEmpty(historyMessage)) {
            return new JSONArray();
        }
        JSONArray newMessageList = new JSONArray();
        for (ChatMessage chatMessage : historyMessage) {
            JSONObject message = new JSONObject();
            if (chatMessage instanceof UserMessage userMessage) {
                message.put("role", "user");
                message.put("content", userMessage.singleText());
                newMessageList.add(message);
            }
            if (chatMessage instanceof AiMessage aiMessage) {
                message.put("role", "ai");
                message.put("content", aiMessage.text());
                newMessageList.add(message);
            }
        }
        if (newMessageList.size()%2!=0){
            newMessageList.remove(newMessageList.size()-1);
        }
        return newMessageList;
    }

    @Override
    public String toString() {
        return "INode{" +
                "runtimeNodeId='" + runtimeNodeId + '\'' +
                ", id='" + id + '\'' +
                ", status=" + status +
                ", errMessage='" + errMessage + '\'' +
                ", type='" + type + '\'' +
                ", properties=" + properties +
                ", nodeParams=" + nodeParams +
                ", context=" + context +
                ", answerText='" + answerText + '\'' +
                ", upNodeIdList=" + upNodeIdList +
                '}';
    }
}




