package com.tarzan.maxkb4j.core.workflow.logic;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.tarzan.maxkb4j.core.exception.ApiException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

@Slf4j
@Data
public class LogicFlow {
    private List<LfNode> nodes;
    private List<LfEdge> edges;
    private final static List<String> endNodes=List.of("ai-chat-node", "reply-node", "function-node", "function-lib-node", "application-node",
            "image-understand-node", "speech-to-text-node", "text-to-speech-node", "image-generate-node");

    public LogicFlow(List<LfNode> nodes, List<LfEdge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public static LogicFlow newInstance(JSONObject flowJson) {
        // 使用TypeReference来指定复杂的类型
        return JSON.parseObject(flowJson.toJSONString(), new TypeReference<LogicFlow>() {});
    }

    public LfNode getStartNode(){
        return this.nodes.stream().filter(node -> node.getId().equals("start-node")).findFirst().orElse(null);
    }

    public LfNode getSearchNode(){
        return this.nodes.stream().filter(node -> node.getId().equals("search-knowledge-node")).findFirst().orElse(null);
    }
    public void isValidNode(LfNode node){
         //todo
    }
    public void isValidWorkFlow(LfNode lastNode){
        if(Objects.isNull(lastNode)){
            lastNode=getStartNode();
        }
        isValidNode(lastNode);
        List<LfNode> nextNodes=getNextNodes(lastNode);
        for (LfNode nextNode : nextNodes) {
            isValidWorkFlow(nextNode);
        }
    }

    public List<LfNode> getNextNodes(LfNode node){
        // Filter edges where the sourceNodeId matches the given node"s id.
        List<LfEdge> edgeList = edges.stream()
                .filter(edge -> edge.getSourceNodeId().equals(node.getId()))
                .toList();

        // Find all target nodes from the filtered edges.
        List<LfNode> nodeList = edgeList.stream()
                .map(edge -> nodes.stream()
                        .filter(n -> n.getId().equals(edge.getTargetNodeId()))
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull).toList();

        if (nodeList.isEmpty() && !endNodes.contains(node.getType())) {
            log.error("不存在的下一个节点");
            throw new ApiException("不存在的下一个节点");
        }
        return nodeList;
    }

    public void isValidBaseNode(){
        // 使用stream过滤出所有id为'base-node'的节点
        List<LfNode> baseNodeList = nodes.stream()
                .filter(node -> "base-node".equals(node.getId()))
                .toList();

        if (baseNodeList.isEmpty()) {
         //   throw new AppApiException(500, "基本信息节点必填");
            log.error("基本信息节点必填");
        }
        if (baseNodeList.size() > 1) {
          //  throw new AppApiException(500, "基本信息节点只能有一个");
            log.error("基本信息节点只能有一个");
        }
    }

}
