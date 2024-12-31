package com.tarzan.maxkb4j.module.team.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.handler.MemberOperateTypeHandler;
import com.tarzan.maxkb4j.handler.UUIDTypeHandler;
import com.tarzan.maxkb4j.module.common.dto.MemberOperate;
import com.tarzan.maxkb4j.module.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
  * @author tarzan
  * @date 2024-12-27 14:06:50
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("team_member_permission")
public class TeamMemberPermissionEntity extends BaseEntity {

	private String authTargetType;

	@TableField(typeHandler = UUIDTypeHandler.class)
	private UUID target;
	@TableField(typeHandler = MemberOperateTypeHandler.class)
	private MemberOperate operate;
	@TableField(typeHandler = UUIDTypeHandler.class)
	private UUID memberId;
} 
