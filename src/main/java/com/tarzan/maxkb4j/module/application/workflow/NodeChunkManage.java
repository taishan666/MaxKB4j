package com.tarzan.maxkb4j.module.application.workflow;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NodeChunkManage {
    private List<NodeChunk> nodeChunkList;
    private NodeChunk currentNodeChunk;
    @JSONField(serialize = false)
    private WorkflowManage workflow;

    public NodeChunkManage(WorkflowManage workflow) {
        this.nodeChunkList=new ArrayList<>();
        this.workflow = workflow;
    }

    public void addNodeChunk(NodeChunk nodeChunk) {
        nodeChunkList.add(nodeChunk);
    }

    public boolean contains(NodeChunk nodeChunk){
        return nodeChunkList.contains(nodeChunk);
    }

    public JSONObject pop() {
        if (currentNodeChunk == null) {
            if (!nodeChunkList.isEmpty()) {
                currentNodeChunk = nodeChunkList.remove(0);
            }
        }

        if (currentNodeChunk != null) {
            if (!currentNodeChunk.getChunkList().isEmpty()) {
                return currentNodeChunk.getChunkList().remove(0);
            } else if (currentNodeChunk.isEnd()) {
                currentNodeChunk = null;
                if (workflow.answerIsNotEmpty()) {
                    String chatId = workflow.getParams().getChatId();
                    String chatRecordId = workflow.getParams().getChatRecordId();
                    JSONObject chunk = workflow.getBaseToResponse().toStreamChunkResponse(chatId, chatRecordId, null,null,"\n\n", false, 0, 0);
                    workflow.appendAnswer("\n\n");
                    return chunk;
                } else {
                    return pop();
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "NodeChunkManage{" +
                "nodeChunkList=" + nodeChunkList +
                ", currentNodeChunk=" + currentNodeChunk +
                '}';
    }
}
