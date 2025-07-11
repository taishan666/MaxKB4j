package com.tarzan.maxkb4j.module.system.team.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
  * @author tarzan
  * @date 2024-12-27 14:03:01
  */
@Data
@TableName("team")
public class TeamEntity {

	@TableField(fill = FieldFill.INSERT)
	private Date createTime;
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private Date updateTime;

	@TableId(value = "user_id", type = IdType.INPUT)
	private String userId;
	
	private String name;
} 
