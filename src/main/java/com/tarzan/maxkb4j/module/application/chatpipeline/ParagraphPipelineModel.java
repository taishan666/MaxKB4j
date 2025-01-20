package com.tarzan.maxkb4j.module.application.chatpipeline;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ParagraphPipelineModel {
    private String id;
    private String documentId;
    private String datasetId;
    private String content;
    private String title;
    private String status;
    private boolean isActive;
    private float comprehensiveScore;
    private float similarity;
    private String datasetName;
    private String documentName;
    private String hitHandlingMethod;
    private float directlyReturnSimilarity;
    private Map<String, Object> meta = new HashMap<>();

    public ParagraphPipelineModel(String id, String documentId, String datasetId, String content, String title,
                                  String status, boolean isActive, float comprehensiveScore, float similarity,
                                  String datasetName, String documentName, String hitHandlingMethod,
                                  float directlyReturnSimilarity) {
        this.id = id;
        this.documentId = documentId;
        this.datasetId = datasetId;
        this.content = content;
        this.title = title;
        this.status = status;
        this.isActive = isActive;
        this.comprehensiveScore = comprehensiveScore;
        this.similarity = similarity;
        this.datasetName = datasetName;
        this.documentName = documentName;
        this.hitHandlingMethod = hitHandlingMethod;
        this.directlyReturnSimilarity = directlyReturnSimilarity;
    }

    // Getters and Setters
    // ...

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("document_id", documentId);
        map.put("dataset_id", datasetId);
        map.put("content", content);
        map.put("title", title);
        map.put("status", status);
        map.put("is_active", isActive);
        map.put("comprehensive_score", comprehensiveScore);
        map.put("similarity", similarity);
        map.put("dataset_name", datasetName);
        map.put("document_name", documentName);
        map.put("meta", meta);
        return map;
    }
}
