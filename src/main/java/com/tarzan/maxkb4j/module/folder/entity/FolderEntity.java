package com.tarzan.maxkb4j.module.folder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.domain.base.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("folder")
public class FolderEntity extends BaseEntity {
    private String name;
    private String desc;
    private String source;
    private String parentId;
    private String userId;

}
