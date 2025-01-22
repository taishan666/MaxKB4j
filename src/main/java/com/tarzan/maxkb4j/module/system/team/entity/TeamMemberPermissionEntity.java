package com.tarzan.maxkb4j.module.system.team.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.handler.MemberOperateTypeHandler;
import com.tarzan.maxkb4j.common.dto.MemberOperate;
import com.tarzan.maxkb4j.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
  * @author tarzan
  * @date 2024-12-27 14:06:50
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("team_member_permission")
public class TeamMemberPermissionEntity extends BaseEntity {

	private String authTargetType;

	private String target;
	@TableField(typeHandler = MemberOperateTypeHandler.class)
	private MemberOperate operate;
	private String memberId;
} 
