package com.guiderag.common.constant;

public class AuthConstants {
    public static final String AUTH_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String USER_ID_HEADER = "X-User-Id";
    
    public static final String CAPTCHA_CODE_KEY = "auth:captcha:";
    public static final long CAPTCHA_EXPIRATION = 5; // 5 minutes
    
    public static final String JWT_SECRET = "guide_rag_secret_key_needs_to_be_long_enough_for_hs256_algorithm_signature";
    public static final long JWT_EXPIRATION = 2 * 60 * 60 * 1000L; // 2 hours
    public static final long JWT_REFRESH_EXPIRATION = 7 * 24 * 60 * 60 * 1000L; // 7 days
}
