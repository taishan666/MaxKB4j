package com.tarzan.maxkb4j.module.team.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.tarzan.maxkb4j.handler.UUIDTypeHandler;
import com.tarzan.maxkb4j.module.common.entity.BaseEntity;
import com.tarzan.maxkb4j.serializer.NullRootSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;
import java.util.Date;

/**
 * @author tarzan
 * @date 2024-12-25 12:42:39
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("team_member")
public class TeamMemberEntity extends BaseEntity {
    @JsonSerialize(using = ToStringSerializer.class, nullsUsing = NullRootSerializer.class)
	@TableField(typeHandler = UUIDTypeHandler.class)
    private UUID id;
    @JsonProperty("team_id")
	@TableField(typeHandler = UUIDTypeHandler.class)
    private UUID teamId;
    @JsonProperty("user_id")
	@TableField(typeHandler = UUIDTypeHandler.class)
    private UUID userId;
} 
