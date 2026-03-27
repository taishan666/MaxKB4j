package com.maxkb4j.knowledge.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.TextScore;

/**
 * @author tarzan
 * @date 2024-12-30 18:08:16
 */
@Data
@Document(collection = "embedding")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextChunk {
	private String sourceId;
	private Integer sourceType;
	private String knowledgeId;
	private String documentId;
	private String paragraphId;
	@TextIndexed
	private String content;
	@TextScore
	private double score;
}
