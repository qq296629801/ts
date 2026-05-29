package com.ts.gateway.security;

import java.util.regex.Pattern;

/**
 * 日志脱敏：避免 API Key、完整 JWT 写入日志。
 */
public final class LogSanitizer {

    private static final Pattern JWT = Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
    private static final Pattern API_KEY = Pattern.compile("(sk-[A-Za-z0-9]{8,})|([Aa][Pp][Ii][_-]?[Kk][Ee][Yy][\"'=:\\s]+[A-Za-z0-9._-]{8,})");

    private LogSanitizer() {
    }

    public static String sanitize(String message) {
        if (message == null) {
            return "";
        }
        String s = JWT.matcher(message).replaceAll("[JWT_REDACTED]");
        return API_KEY.matcher(s).replaceAll("[API_KEY_REDACTED]");
    }
}
