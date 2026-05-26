package com.maxkb4j.knowledge.linearrag.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

/**
 * MongoDB entity for storing extracted graph entity nodes.
 * Each document represents a unique entity in a knowledge base,
 * along with its occurrence locations (paragraphs).
 */
@Data
@Document(collection = "graph_entity_node")
@CompoundIndex(name = "kb_name", def = "{'knowledgeId': 1, 'name': 1}", unique = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphEntityNode {

    @Id
    private String id;

    /** Normalized entity name (lowercase) for dedup */
    private String name;

    /** Knowledge base this entity belongs to */
    private String knowledgeId;

    /** Document IDs where this entity appears */
    private List<String> documentIds;

    /** Paragraph IDs where this entity appears */
    private List<String> paragraphIds;

    /** Number of occurrences across all paragraphs */
    private int frequency;

    /** Pre-computed embedding vector for this entity */
    private float[] embedding;

    private Date createTime;

    private Date updateTime;
}
