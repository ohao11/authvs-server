package com.authvs.server.model.out;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginVo {
    private String token;
    private String redirectUrl;
    private String state; // LOGGED_IN, MFA_REQUIRED
    private String mfaId;
    private java.util.List<MfaOptionVo> options;
}
