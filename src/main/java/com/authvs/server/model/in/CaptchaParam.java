package com.authvs.server.model.in;

import lombok.Data;

@Data
public class CaptchaParam {
    private String type = "login";
}
