package com.maxkb4j.knowledge.linearrag.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * MongoDB entity for storing sentence nodes in the LinearRAG Tri-Graph.
 * Each document represents a sentence extracted from a paragraph,
 * with its position (index) within the paragraph.
 */
@Data
@Document(collection = "graph_sentence_node")
@CompoundIndex(name = "kb_para_idx", def = "{'knowledgeId': 1, 'paragraphId': 1, 'sentenceIndex': 1}", unique = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphSentenceNode {

    @Id
    private String id;

    /** Sentence text content */
    private String content;

    /** Paragraph this sentence belongs to */
    private String paragraphId;

    /** Position index within the paragraph (0-based) */
    private int sentenceIndex;

    /** Knowledge base this sentence belongs to */
    private String knowledgeId;

    /** Document this sentence originates from */
    private String documentId;

    private Date createTime;

    private Date updateTime;
}
