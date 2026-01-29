package com.authvs.server.model.in;

import lombok.Data;

@Data
public class TotpEnableParam {
    private String secret;
    private String code;
}
