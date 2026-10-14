package com.maxkb4j.system.service;


import com.alibaba.fastjson.JSONObject;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final MailConfigService mailConfigService;
    private final SpringTemplateEngine springTemplateEngine;


    public void sendTextMessage(String to, String subject, String text) {
        JavaMailSenderImpl mailSender = mailConfigService.createMailSender();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailSender.getUsername());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    public void sendMessage(String to, String subject, String templateName, Context context) throws MessagingException {
        JavaMailSenderImpl mailSender = mailConfigService.createMailSender();
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        String content = springTemplateEngine.process(templateName, context);
        String username = mailSender.getUsername();
        if (username == null || !username.contains("@")) {
            throw new IllegalArgumentException("Invalid sender email address: " + username);
        }
        String personal = username.substring(0, username.indexOf("@"));
        try {
            helper.setFrom(new InternetAddress(username, personal, "UTF-8").toString());
        } catch (java.io.UnsupportedEncodingException e) {
            // 编码不支持时降级为纯地址
            helper.setFrom(username);
        }
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
