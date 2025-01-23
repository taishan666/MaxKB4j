package com.tarzan.maxkb4j.module.system.setting.service;


import com.alibaba.fastjson.JSONObject;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Objects;

@Slf4j
@Service
@AllArgsConstructor
public class EmailService {

    private final MailConfigService mailConfigService;
    private final TemplateEngine templateEngine;


    public void sendMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailConfigService.getJavaMailSender().send(message);
    }

    public void sendMessage(String to, String subject, String templateName, Context context) throws MessagingException {
        JavaMailSenderImpl mailSender=mailConfigService.getJavaMailSender();
        MimeMessage mimeMessage = mailConfigService.getJavaMailSender().createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        String content = templateEngine.process(templateName, context);
        helper.setFrom(new InternetAddress(Objects.requireNonNull(mailSender.getUsername()).substring(0, mailSender.getUsername().indexOf("@"))+"<"+mailSender.getUsername()+">").toString());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true); // true indicates HTML content
        mailSender.send(mimeMessage);
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
