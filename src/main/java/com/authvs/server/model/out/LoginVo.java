package com.authvs.server.model.out;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginVo {
    private String token;
    private String redirectUrl;
}
