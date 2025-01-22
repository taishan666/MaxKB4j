package com.tarzan.maxkb4j.module.system.team.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;
import java.util.UUID;

/**
  * @author tarzan
  * @date 2024-12-27 14:03:01
  */
@Data
@TableName("team")
public class TeamEntity {
	
	private Date createTime;
	
	private Date updateTime;

	@TableId(value = "user_id", type = IdType.INPUT)
	//@TableField(typeHandler = UUIDTypeHandler.class)
	private String userId;
	
	private String name;
} 
