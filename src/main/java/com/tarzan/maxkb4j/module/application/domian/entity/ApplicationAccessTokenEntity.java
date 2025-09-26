package com.tarzan.maxkb4j.module.application.domian.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.handler.type.StringListTypeHandler;
import com.tarzan.maxkb4j.common.util.MD5Util;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-25 18:05:58
 */
@NoArgsConstructor
@Data
@TableName(value = "application_access_token",autoResultMap = true)
public class ApplicationAccessTokenEntity {

    @TableId
    private String applicationId;
    
    private String accessToken;
    
    private Boolean isActive;
    
    private Integer accessNum;
    
    private Boolean whiteActive;
    
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> whiteList;
    
    private Boolean showSource;

    private Boolean showExec;

    private Boolean authentication;

    private String language;


    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;



    public ApplicationAccessTokenEntity(Boolean isActive, Integer accessNum, Boolean whiteActive, List<String> whiteList, Boolean showSource,String language) {
        this.isActive = isActive;
        this.accessNum = accessNum;
        this.whiteActive = whiteActive;
        this.whiteList = whiteList;
        this.showSource = showSource;
        this.language = language;
        this.accessToken=MD5Util.encrypt(UUID.randomUUID().toString(), 8, 24);
    }

    public static ApplicationAccessTokenEntity createDefault() {
        return new ApplicationAccessTokenEntity(true,100,false,new ArrayList<>(),false,"zh-CH");
    }
}
