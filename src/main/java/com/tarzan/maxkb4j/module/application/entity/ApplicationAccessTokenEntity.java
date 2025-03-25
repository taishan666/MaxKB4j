package com.tarzan.maxkb4j.module.application.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.handler.type.StringSetTypeHandler;
import com.tarzan.maxkb4j.util.MD5Util;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-25 18:05:58
 */
@NoArgsConstructor
@Data
@TableName("application_access_token")
public class ApplicationAccessTokenEntity {

    @TableId
    private String applicationId;
    
    private String accessToken;
    
    private Boolean isActive;
    
    private Integer accessNum;
    
    private Boolean whiteActive;
    
    @TableField(typeHandler = StringSetTypeHandler.class)
    private Set<String> whiteList;
    
    private Boolean showSource;

    private String language;


    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;



    public ApplicationAccessTokenEntity(Boolean isActive, Integer accessNum, Boolean whiteActive, Set<String> whiteList, Boolean showSource,String language) {
        this.isActive = isActive;
        this.accessNum = accessNum;
        this.whiteActive = whiteActive;
        this.whiteList = whiteList;
        this.showSource = showSource;
        this.language = language;
        this.accessToken=MD5Util.encrypt(UUID.randomUUID().toString(), 8, 24);
    }

    public static ApplicationAccessTokenEntity createDefault() {
        return new ApplicationAccessTokenEntity(true,100,false,new HashSet<>(),false,"zh-CH");
    }
}
