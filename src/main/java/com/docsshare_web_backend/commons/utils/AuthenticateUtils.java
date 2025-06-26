package com.docsshare_web_backend.commons.utils;

import com.docsshare_web_backend.users.enums.UserType;
import java.util.List;

public class AuthenticateUtils {
    public static boolean haveAllReadPermission(String type) {
        List<String> validPermission = List.of(
                UserType.ADMIN.name()
        );
        return validPermission.contains(type);
    }

    public static boolean isAdmin(String type) {
        return UserType.ADMIN.name().equals(type);
    }
}