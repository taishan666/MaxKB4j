package com.tarzan.maxkb4j.module.embedding.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.alibaba.fastjson.JSONObject;
 /**
  * @author tarzan
  * @date 2024-12-30 18:08:16
  */
@Data
@TableName("embedding")
public class EmbeddingEntity {
	//null
	private String id;
	//null
	private String sourceId;
	//null
	private String sourceType;
	//null
	private Boolean isActive;
	//null
	private Long embedding;
	//null
	private JSONObject meta;
	//null
	private String datasetId;
	//null
	private String documentId;
	//null
	private String paragraphId;
	//null
	private Long searchVector;
} 
