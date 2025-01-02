package com.tarzan.maxkb4j.module.systemSetting.service;


import com.alibaba.fastjson.JSONObject;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Properties;

@Slf4j
@Service
public class EmailService {

    private final MailConfigService mailConfigService;

    @Autowired
    public EmailService(MailConfigService mailConfigService) {
        this.mailConfigService = mailConfigService;
    }

    public void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailConfigService.getJavaMailSender().send(message);
    }

    public boolean testConnect(JSONObject meta) {
        JavaMailSenderImpl mailSender = mailConfigService.createJavaMailSender(meta);
        try {
            mailSender.testConnection();
            return true;
        } catch (MessagingException e) {
            log.error(e.getMessage());
        }
        return false;
    }
}
