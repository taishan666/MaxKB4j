package com.tarzan.maxkb4j.module.system.team.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.tarzan.maxkb4j.common.entity.BaseEntity;
import com.tarzan.maxkb4j.serializer.NullRootSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author tarzan
 * @date 2024-12-25 12:42:39
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("team_member")
public class TeamMemberEntity extends BaseEntity {
    @JsonSerialize(using = ToStringSerializer.class, nullsUsing = NullRootSerializer.class)
    private String id;
    @JsonProperty("team_id")
    private String teamId;
    @JsonProperty("user_id")
    private String userId;
} 
