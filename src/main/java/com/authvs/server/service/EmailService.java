package com.authvs.server.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String from;

    @Async
    public void sendSimpleMail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("邮件发送成功: {} -> {}", from, to);
        } catch (Exception e) {
            log.error("邮件发送失败: {} -> {}", from, to, e);
        }
    }

    @Async
    public void sendHtmlMail(String to, String subject, String html) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mimeMessage);
            log.info("邮件发送成功: {} -> {}", from, to);
        } catch (Exception e) {
            log.error("邮件发送失败: {} -> {}", from, to, e);
        }
    }

    public String renderTemplate(String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            if (variables != null) {
                context.setVariables(variables);
            }
            // Thymeleaf 默认模板路径在 classpath:/templates/
            // 如果传入 "templates/email/verification.html"，需要去掉 "templates/" 前缀和 ".html" 后缀
            // 这里假设调用方传入的是逻辑视图名，例如 "email/verification"
            // 为了兼容旧调用 "templates/email/verification.html"，做一下处理
            String logicalName = templateName;
            if (logicalName.startsWith("templates/")) {
                logicalName = logicalName.substring("templates/".length());
            }
            if (logicalName.endsWith(".html")) {
                logicalName = logicalName.substring(0, logicalName.length() - 5);
            }

            return templateEngine.process(logicalName, context);
        } catch (Exception e) {
            throw new IllegalStateException("渲染模板失败: " + templateName, e);
        }
    }

    @Async
    public void sendLoginCodeMail(String to, String code, int expiresMinutes) {
        String html = renderTemplate("email/verification", Map.of(
                "appName", "AuthVs",
                "code", code,
                "expires", String.valueOf(expiresMinutes)
        ));
        sendHtmlMail(to, "AuthVs 登录验证码", html);
    }
}
