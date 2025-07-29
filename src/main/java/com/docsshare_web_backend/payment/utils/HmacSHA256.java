package com.docsshare_web_backend.payment.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HmacSHA256 {
    public static String sign(String data, String secretKey) throws Exception {
        Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
        hmacSHA256.init(secretKeySpec);
        byte[] rawHmac = hmacSHA256.doFinal(data.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : rawHmac) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}