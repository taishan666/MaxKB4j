package com.tarzan.maxkb4j.module.system.team.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
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

    private String teamId;
    private String userId;
} 
