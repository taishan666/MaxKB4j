package com.tarzan.maxkb4j.module.application.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.handler.UUIDTypeHandler;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-25 18:05:58
 */
@Data
@TableName("application_access_token")
public class ApplicationAccessTokenEntity {
    
    @JsonProperty("create_time")
    private Date createTime;
    
    @JsonProperty("update_time")
    private Date updateTime;
    
    @JsonProperty("application_id")
	@TableField(typeHandler = UUIDTypeHandler.class)
    private UUID applicationId;
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("is_active")
    private Boolean isActive;
    
    @JsonProperty("access_num")
    private Integer accessNum;
    
    @JsonProperty("white_active")
    private Boolean whiteActive;
    
/*    @JsonProperty("white_list")
    private List<String> whiteList;*/
    
    @JsonProperty("show_source")
    private Boolean showSource;
} 
