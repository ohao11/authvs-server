package com.authvs.server.model.in;

import lombok.Data;

@Data
public class MfaSendParam {
    private String mfaId;
    private String type; // SMS, EMAIL
}
