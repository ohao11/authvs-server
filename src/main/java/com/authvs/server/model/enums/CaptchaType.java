package com.authvs.server.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 验证码类型枚举
 */
@Getter
@AllArgsConstructor
public enum CaptchaType {

    /**
     * 登录验证码
     */
    LOGIN("login", "登录验证码");

    private final String type;
    private final String description;
}
