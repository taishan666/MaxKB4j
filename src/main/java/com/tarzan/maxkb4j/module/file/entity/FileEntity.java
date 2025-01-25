package com.tarzan.maxkb4j.module.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.alibaba.fastjson.JSONObject;
import java.util.Date;
 /**
  * @author tarzan
  * @date 2025-01-21 09:34:51
  */
@Data
@TableName("file")
public class FileEntity {
	
	private Date createTime;
	
	private Date updateTime;
	
	private String id;
	
	private String fileName;
	
	private Integer loid;
	
	private JSONObject meta;
} 
