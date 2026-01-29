package com.authvs.server.model.out;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TotpSetupVo {
    private String secret;
    private String otpAuthUrl;
}
