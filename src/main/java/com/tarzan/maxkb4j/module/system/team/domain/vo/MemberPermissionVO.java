package com.tarzan.maxkb4j.module.system.team.domain.vo;

import lombok.Data;


@Data
public class MemberPermissionVO {

    private String id;
    private String name;
    private String type;
    private String userId;
    private String targetId;
    private String memberId;
    private MemberOperate operate;
}
