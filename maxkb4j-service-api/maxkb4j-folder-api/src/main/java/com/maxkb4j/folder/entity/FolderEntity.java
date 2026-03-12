package com.maxkb4j.folder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.maxkb4j.common.mp.base.BaseEntity;
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
