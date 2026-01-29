package com.authvs.server.util;

/**
 * Redis Key 管理
 *
 * 统一维护所有业务相关的 Redis Key 构造，避免散落各处。
 * 每个方法说明包含用途与典型 TTL 建议（TTL 由调用方设置）。
 */
public final class RedisKeys {

    private RedisKeys() {}

    /**
     * 图形验证码存储 Key
     * 用途：存储指定类型的图形验证码值以供校验
     * 结构：captcha:{type}:{key}
     * TTL 建议：5分钟
     */
    public static String captcha(String type, String key) {
        return "captcha:" + type + ":" + key;
    }

    /**
     * 登录 MFA 临时态 Key
     * 用途：保存登录过程中生成的 mfaId 与用户名的关联
     * 结构：mfa:login:{mfaId}
     * TTL 建议：5分钟
     */
    public static String mfaLogin(String mfaId) {
        return "mfa:login:" + mfaId;
    }

    /**
     * 登录 MFA 验证码 Key
     * 用途：保存按类型生成的验证码值，用于后续校验
     * 结构：mfa:code:{type}:{mfaId}
     * TTL 建议：5分钟
     */
    public static String mfaCode(String type, String mfaId) {
        return "mfa:code:" + type + ":" + mfaId;
    }

    /**
     * 验证码发送限流（分钟维度）Key
     * 用途：控制同一渠道+目标在 1 分钟内只发送一次
     * 结构：mfa:limit:minute:{channel}:{target}
     * TTL 建议：1分钟
     */
    public static String mfaLimitMinute(String channel, String target) {
        return "mfa:limit:minute:" + channel + ":" + target;
    }

    /**
     * 验证码发送限流（小时维度）Key
     * 用途：控制同一渠道+目标在 1 小时内最多发送 10 次
     * 结构：mfa:limit:hour:{channel}:{target}
     * TTL 建议：1小时
     */
    public static String mfaLimitHour(String channel, String target) {
        return "mfa:limit:hour:" + channel + ":" + target;
    }
}
