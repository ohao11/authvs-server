package com.authvs.server.model.in;

import lombok.Data;

@Data
public class MfaLoginParam {
    private String mfaId;
    private String code;
}
