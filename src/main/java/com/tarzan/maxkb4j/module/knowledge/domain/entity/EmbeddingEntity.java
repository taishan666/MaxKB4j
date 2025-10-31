package com.tarzan.maxkb4j.module.knowledge.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.TextScore;

/**
 * @author tarzan
 * @date 2024-12-30 18:08:16
 */
@Data
@TableName(value = "embedding",autoResultMap = true)
@Document(collection = "embedding")
public class EmbeddingEntity {
	@TableId
	@Id
	private String id;
	private String sourceId;
	private Integer sourceType;
	private Boolean isActive;
	private String knowledgeId;
	private String documentId;
	private String paragraphId;
	@TextIndexed
	private String content;
	@TableField(exist = false)
	@TextScore
	private float score; // 匹配度得分

	public TextSegment toTextSegment() {
		Metadata metadata=new Metadata();
		metadata.put("source_id",sourceId);
		metadata.put("source_type",sourceType);
		metadata.put("paragraph_id",paragraphId);
		metadata.put("document_id",documentId);
		metadata.put("is_active",isActive.toString());
		return TextSegment.from("*",metadata);
	}
} 
