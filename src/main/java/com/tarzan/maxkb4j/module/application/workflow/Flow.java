package com.tarzan.maxkb4j.module.application.workflow;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

@Slf4j
@Data
public class Flow {
    private List<Node> nodes;
    private List<Edge> edges;
    private final static List<String> endNodes=List.of("ai-chat-node", "reply-node", "function-node", "function-lib-node", "application-node",
            "image-understand-node", "speech-to-text-node", "text-to-speech-node", "image-generate-node");
    public Flow(List<Node> nodes, List<Edge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public static  Flow newInstance(JSONObject flow){
       /* List<Node> nodes= (List<Node>) flow.get("nodes");
        List<Edge> edges= (List<Node>) flow.get("edges");
        return new Flow(nodes,edges);*/
        return flow.toJavaObject(Flow.class);
    }

    public Node getStartNode(){
        return this.nodes.stream().filter(node -> node.getId().equals("start-node")).findFirst().orElse(null);
    }

    public Node getSearchNode(){
        return this.nodes.stream().filter(node -> node.getId().equals("search-dataset-node")).findFirst().orElse(null);
    }
    public void isValidNode(Node node){
         //todo
    }
    public void isValidWorkFlow(Node lastNode){
        if(Objects.isNull(lastNode)){
            lastNode=getStartNode();
        }
        isValidNode(lastNode);
        List<Node> nextNodes=getNextNodes(lastNode);
        for (Node nextNode : nextNodes) {
            isValidWorkFlow(nextNode);
        }
    }

    public List<Node> getNextNodes(Node node){
        // Filter edges where the sourceNodeId matches the given node"s id.
        List<Edge> edgeList = edges.stream()
                .filter(edge -> edge.getSourceNodeId().equals(node.getId()))
                .toList();

        // Find all target nodes from the filtered edges.
        List<Node> nodeList = edgeList.stream()
                .map(edge -> nodes.stream()
                        .filter(n -> n.getId().equals(edge.getTargetNodeId()))
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull).toList();

        if (nodeList.isEmpty() && !endNodes.contains(node.getType())) {
            //throw new AppApiException(500, "不存在的下一个节点");
            log.error("不存在的下一个节点");
        }
        return nodeList;
    }

    public void isValidBaseNode(){
        // 使用stream过滤出所有id为'base-node'的节点
        List<Node> baseNodeList = nodes.stream()
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
