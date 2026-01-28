package com.authvs.server.model.in;

import lombok.Data;

@Data
public class LoginParam {
    private String username;
    private String password;
    private String captcha;
    private String captchaKey;
}
