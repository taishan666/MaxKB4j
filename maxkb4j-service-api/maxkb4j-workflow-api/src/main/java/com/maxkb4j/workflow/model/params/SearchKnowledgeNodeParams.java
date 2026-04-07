package com.maxkb4j.workflow.model.params;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

/**
 * 知识库搜索节点参数
 * 从 SearchKnowledgeNode.NodeParams 提取
 */
@Data
public class SearchKnowledgeNodeParams {
    private List<String> knowledgeIds;
    private String question;
    private Integer topN;
    private Float scoreThreshold;
    private String searchMode;
    private JSONObject rerankConfig;
}