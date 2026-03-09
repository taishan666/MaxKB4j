package com.maxkb4j.system.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.system.entity.SystemSettingEntity;
import com.maxkb4j.system.mapper.SystemSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Properties;

@RequiredArgsConstructor
@Service
public class MailConfigService {

    private final SystemSettingMapper systemSettingMapper;


    public JavaMailSenderImpl createMailSender() {
        SystemSettingEntity systemSetting = systemSettingMapper.selectOne(Wrappers.<SystemSettingEntity>lambdaQuery().eq(SystemSettingEntity::getType,0));
        if(Objects.nonNull(systemSetting)){
           return createJavaMailSender(systemSetting.getMeta());
        }
        return new JavaMailSenderImpl();
    }

    public JavaMailSenderImpl createJavaMailSender(JSONObject meta) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        if(Objects.nonNull(meta)){
            mailSender.setHost(meta.getString("email_host"));
            mailSender.setPort(meta.getInteger("email_port"));
            mailSender.setUsername(meta.getString("email_host_user"));
            mailSender.setPassword(meta.getString("email_host_password"));
            mailSender.setDefaultEncoding("UTF-8");
            mailSender.setProtocol("smtp");
            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", meta.getBooleanValue("email_use_ssl"));
            props.put("mail.smtp.starttls.enable", meta.getBooleanValue("email_use_tls"));
            mailSender.setJavaMailProperties(props);
        }
        return mailSender;
    }

}