package com.tarzan.maxkb4j.core.workflow;

import com.tarzan.maxkb4j.module.application.vo.ChatMessageVO;
import lombok.Data;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class NodeChunkManage {

    private List<NodeChunk> nodeChunkList;

    public NodeChunkManage() {
        this.nodeChunkList = new CopyOnWriteArrayList<>();
    }

    public void addNodeChunk(NodeChunk nodeChunk) {
        nodeChunkList.add(nodeChunk);
    }

    public ChatMessageVO pop() {
        NodeChunk currentNodeChunk = null;
        if (!nodeChunkList.isEmpty()) {
            currentNodeChunk = nodeChunkList.get(0);
        }
        if (currentNodeChunk != null) {
            if (!currentNodeChunk.getChunkList().isEmpty()) {
                return currentNodeChunk.getChunkList().remove(0);
            } else {
                if (currentNodeChunk.isEnd()) {
                    nodeChunkList.remove(0);
                }
            }
        }
        return null;
    }

/*    @Override
    public String toString() {
        return "NodeChunkManage{" +
                "nodeChunkList=" + nodeChunkList +
                '}';
    }*/
}
