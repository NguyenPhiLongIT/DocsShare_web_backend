package com.docsshare_web_backend.users.services.impl;
import com.docsshare_web_backend.users.services.MailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendChangePasswordNotification(String toEmail, String name) {
        String subject = "🔐 Password Changed Successfully";
        String content = String.format("Hi %s,\n\nYour password was changed successfully. If you did not do this, please contact support immediately.", name);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(content);

        mailSender.send(message);
    }


    @Override
    public void sendCustomEmail(String toEmail, String subject, String content) {
        sendSimpleMessage(toEmail, subject, content);
    }

    private void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
