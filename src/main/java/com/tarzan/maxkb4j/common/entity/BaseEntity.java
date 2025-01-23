package com.tarzan.maxkb4j.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tarzan.maxkb4j.serializer.NullRootSerializer;
import lombok.Data;

import java.util.Date;

@Data
public class BaseEntity {

    @JsonSerialize(nullsUsing = NullRootSerializer.class)
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @JsonProperty("create_time")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @JsonProperty("update_time")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

}
