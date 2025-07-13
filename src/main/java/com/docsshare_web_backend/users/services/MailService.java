package com.docsshare_web_backend.users.services;

public interface MailService {
    //void sendPasswordChangeNotification(String toEmail, String userName);
    void sendCustomEmail(String toEmail, String subject, String content);
    void sendChangePasswordNotification(String toEmail, String name);

}