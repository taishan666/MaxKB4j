package com.tarzan.maxkb4j.core.workflow;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
@Data
public abstract class INode {
    private String id;
    private int status = 200;
    private String errMessage = "";
    private String type;
    private String viewType;
    private JSONObject properties;
    protected Map<String, Object> context;
    protected JSONObject detail;
    private List<String> upNodeIdList;
    private String runtimeNodeId;
    private String answerText;


    public INode(JSONObject properties) {
        this.context = new HashMap<>(10);
        this.detail = new JSONObject();
        this.upNodeIdList = new ArrayList<>();
        this.properties = properties;
        this.viewType = "many_view";
        this.answerText = "";
    }


    public JSONObject getNodeData() {
        if (Objects.nonNull(properties) && properties.containsKey("nodeData")) {
            return properties.getJSONObject("nodeData");
        }
        return new JSONObject();
    }

    public void setUpNodeIdList(List<String> upNodeIdList) {
        this.upNodeIdList = upNodeIdList;
        this.runtimeNodeId = generateRuntimeNodeId();
    }


    protected abstract void saveContext(Workflow workflow,JSONObject detail);

    public JSONObject executeDetail() {
        return detail;
    }


    private String generateRuntimeNodeId() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            String input = Arrays.toString(upNodeIdList.toArray()) + id;
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


    public void setDetail(JSONObject detail) {
        this.detail = detail;
       // saveContext(detail);
    }


    public JSONArray resetMessageList(List<ChatMessage> historyMessages) {
        if (CollectionUtils.isEmpty(historyMessages)) {
            return new JSONArray();
        }
        JSONArray newMessageList = new JSONArray();
        for (ChatMessage chatMessage : historyMessages) {
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
        if (newMessageList.size() % 2 != 0) {
            newMessageList.remove(newMessageList.size() - 1);
        }
        return newMessageList;
    }


}




