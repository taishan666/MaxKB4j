package com.tarzan.maxkb4j.module.knowledge.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.typehandler.EmbeddingTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.TextScore;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-30 18:08:16
 */
@Data
@TableName(value = "embedding",autoResultMap = true)
@Document(collection = "embedding")
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
	@TableField(typeHandler = EmbeddingTypeHandler.class)
	@Transient
	private List<Float> embedding;
	@TextIndexed
	@TableField(exist = false)
	private String content;
	@TableField(exist = false)
	@TextScore
	private float score; // 匹配度得分
	@Transient
	private Integer dimension;

} 
