package com.rental.common;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public final class SessionTokenUtil {
    public static final String COOKIE_NAME = "rental_session";

    private SessionTokenUtil() {
    }

    public static String extract(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && !authorization.isBlank()) {
            return authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public static String authorization(HttpServletRequest request) {
        String token = extract(request);
        return token == null ? null : "Bearer " + token;
    }
}
