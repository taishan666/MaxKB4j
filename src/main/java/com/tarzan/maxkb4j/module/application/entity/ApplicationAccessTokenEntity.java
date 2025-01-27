package com.tarzan.maxkb4j.module.application.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.handler.StringSetTypeHandler;
import lombok.Data;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author tarzan
 * @date 2024-12-25 18:05:58
 */
@Data
@TableName("application_access_token")
public class ApplicationAccessTokenEntity {

    @JsonProperty("create_time")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @JsonProperty("update_time")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @JsonProperty("application_id")
    @TableId
    private String applicationId;
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("is_active")
    private Boolean isActive;
    
    @JsonProperty("access_num")
    private Integer accessNum;
    
    @JsonProperty("white_active")
    private Boolean whiteActive;
    
    @JsonProperty("white_list")
    @TableField(typeHandler = StringSetTypeHandler.class)
    private Set<String> whiteList;
    
    @JsonProperty("show_source")
    private Boolean showSource;

    public ApplicationAccessTokenEntity() {
    }

    public ApplicationAccessTokenEntity(Boolean isActive, Integer accessNum, Boolean whiteActive, Set<String> whiteList, Boolean showSource) {
        this.isActive = isActive;
        this.accessNum = accessNum;
        this.whiteActive = whiteActive;
        this.whiteList = whiteList;
        this.showSource = showSource;
    }

    public static ApplicationAccessTokenEntity createDefault() {
        return new ApplicationAccessTokenEntity(true,0,false,new HashSet<>(),false);
    }
}
