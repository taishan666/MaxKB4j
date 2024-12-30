package com.tarzan.maxkb4j.module.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.handler.UUIDTypeHandler;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
public class BaseEntity {

    @TableId
    @TableField(fill = FieldFill.INSERT,typeHandler = UUIDTypeHandler.class)
    private UUID id;

    @JsonProperty("create_time")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @JsonProperty("update_time")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

}
