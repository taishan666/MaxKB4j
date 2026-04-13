package com.maxkb4j.application.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.maxkb4j.common.mp.base.BaseEntity;
import com.maxkb4j.common.typehandler.StringListTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-26 09:50:23
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "application_chat_share_link",autoResultMap = true)
public class ApplicationChatShareLinkEntity extends BaseEntity {
    private String shareType;
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> chatRecordIds;
    private String chatId;
    private String applicationId;
    private String userId;

}
