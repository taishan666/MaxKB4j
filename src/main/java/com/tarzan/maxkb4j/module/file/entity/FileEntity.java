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
	//null
	private Date createTime;
	//null
	private Date updateTime;
	//null
	private String id;
	//null
	private String fileName;
	//null
	private Integer loid;
	//null
	private JSONObject meta;
} 
