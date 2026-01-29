package com.authvs.server.util;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TotpUtil {

    private static final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    /**
     * 生成 TOTP 密钥
     */
    public static GoogleAuthenticatorKey createCredentials() {
        return gAuth.createCredentials();
    }

    /**
     * 生成 OTP Auth URL
     *
     * @param issuer      发行者 (如: AuthVs)
     * @param accountName 账号名 (如: user@example.com)
     * @param credentials 密钥信息
     * @return otpauth://...
     */
    public static String getQrCodeUrl(String issuer, String accountName, GoogleAuthenticatorKey credentials) {
        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(issuer, accountName, credentials);
    }

    /**
     * 验证验证码
     *
     * @param secret 密钥
     * @param code   用户输入的验证码
     * @return 是否验证通过
     */
    public static boolean authorize(String secret, int code) {
        return gAuth.authorize(secret, code);
    }

    /**
     * 验证验证码 (字符串入参)
     */
    public static boolean authorize(String secret, String code) {
        try {
            return authorize(secret, Integer.parseInt(code));
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
